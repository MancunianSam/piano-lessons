package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import configuration.StripeConfiguration
import controllers.BookingController.{Booking, Contact, Times}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request}
import repositories.StudentRepository
import services.{AmountService, CalendarService, EmailService}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Calendar, Locale, UUID}
import scala.concurrent.{ExecutionContext, Future}

class BookingController @Inject()(
                                   val controllerComponents: SecurityComponents,
                                   val calendarService: CalendarService,
                                   val amountService: AmountService,
                                   val emailService: EmailService,
                                   val stripeConfiguration: StripeConfiguration,
                                   val studentRepository: StudentRepository,
                                   configuration: Configuration
                                 )(implicit val executionContext: ExecutionContext) extends OidcSecurity {

  def contactForm: Form[Contact] = Form[Contact](
    mapping(
      "email" -> email.verifying("Please enter an email address", a => a.nonEmpty),
      "name" -> nonEmptyText,
      "phone" -> nonEmptyText,
      "notes" -> optional(text)
    )
    (Contact.apply)(Contact.unapply)
  )

  def chooseLength(): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok(views.html.chooseLength())
  }

  def book(lessonLength: Int): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok(views.html.book(amountService.getPrices(lessonLength), lessonLength))
  }

  def calendar(numberOfLessons: Int, lessonLength: Int, monthOpt: Option[Int], yearOpt: Option[Int]): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val calendar = Calendar.getInstance()
    val thisMonth = calendar.get(Calendar.MONTH)
    val month = monthOpt.getOrElse(thisMonth)
    val year = yearOpt.getOrElse(calendar.get(Calendar.YEAR))
    if (month < thisMonth || month == 12) {
      Redirect(routes.BookingController.calendar(numberOfLessons, lessonLength, Option(thisMonth), yearOpt))
    } else {
      calendar.set(Calendar.MONTH, month)
      calendar.set(Calendar.DAY_OF_MONTH, 1)
      val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
      val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
      val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
      val allDays = (1 to maxDay).map(_.toString)
      val days: List[String] = List.fill(dayOfWeek)("") ++ allDays
      Ok(views.html.calendar(numberOfLessons, lessonLength, days, monthName, month, month != thisMonth, year))
    }
  }

  def bookingContactDetails(numOfLessons: Int, lessonLength: Int, date: String, time: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok(views.html.bookingContactDetails(contactForm, numOfLessons, lessonLength, date, time))
  }

  def saveBookingContactDetails(numOfLessons: Int, lessonLength: Int, date: String, time: String): Action[AnyContent] = Action.async { implicit request: Request[Any] =>
    contactForm.bindFromRequest().fold(err => {
      Future.successful(BadRequest(views.html.bookingContactDetails(err, numOfLessons, lessonLength, date, time)))
    }, contact => {
      if(lessonLength > 0) {
        val amount = amountService.calculateAmount(numOfLessons, lessonLength)
        for {
          student <- studentRepository.addStudent(contact, amount, None)
          paymentIntent = stripeConfiguration.paymentIntent(amount, student.id)
          _ <- studentRepository.updatePaymentIntentId(student.id, paymentIntent.getId)
        } yield {
          Redirect(routes.BookingController.bookingSummary(numOfLessons, lessonLength, date, time, student.id))
        }
      } else {
        studentRepository.addStudent(contact, 0).map(_ => {
          Redirect(routes.BookingController.thanks())
        })
      }
    })
  }

  def times(numOfLessons: Int, lessonLength: Int, date: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
    //    val slots = calendarService.getAvailableSlots(date, numOfLessons)
    val slots = List("09:00", "10:00", "11:00")
    Ok(views.html.times(numOfLessons, lessonLength, date, slots))

  }

  def bookingSummary(numOfLessons: Int, lengthOfLesson: Int, date: String, time: String, studentId: UUID): Action[AnyContent] = Action.async { implicit request: Request[Any] =>
    val apiKey = configuration.get[String]("stripe.public")
    val cost = amountService.calculateAmount(numOfLessons, lengthOfLesson)
    val dates = (0 until numOfLessons).toList.map(plusWeeks => {
      val localDateTime = LocalDateTime.parse(s"${date}T${time}:00").plusWeeks(plusWeeks)
      localDateTime.format(DateTimeFormatter.ofPattern(s"EEEE d LLLL yyyy HH mm"))
    })
    studentRepository.getStudent(studentId).map(student => {
      val email = student.head.email
      val booking = Booking(studentId, email, numOfLessons, lengthOfLesson, dates, cost)
      Ok(views.html.bookingSummary(booking, apiKey))
    })

  }

  def thanks(): Action[AnyContent] = Action { _ =>
    Ok(views.html.thanks())
  }
}

object BookingController {
  case class Booking(studentId: UUID, email: String, numberOfLessons: Int, lengthOfLesson: Int, dates: List[String], totalCost: Int)

  case class Times(slot: String)

  case class Contact(email: String, name: String, phone: String, notes: Option[String]) {
    override def toString: String = {
      s"""
         |Email: $email
         |Name: $name
         |Phone: $phone
         |Notes: ${notes.getOrElse("")}
         |""".stripMargin
    }
  }
}
