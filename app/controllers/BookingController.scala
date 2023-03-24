package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import configuration.StripeConfiguration
import controllers.BookingController.{Booking, Contact}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request}
import repositories.TimesRepository.formattedPattern
import repositories.{StudentRepository, TimesRepository}
import services.{AmountService, CalendarService, EmailService}

import java.time.LocalDateTime
import java.util.{Calendar, Locale, UUID}
import scala.concurrent.{ExecutionContext, Future}

class BookingController @Inject()(
                                   val controllerComponents: SecurityComponents,
                                   val calendarService: CalendarService,
                                   val amountService: AmountService,
                                   val emailService: EmailService,
                                   val stripeConfiguration: StripeConfiguration,
                                   val studentRepository: StudentRepository,
                                   val timesRepository: TimesRepository,
                                   configuration: Configuration
                                 )(implicit val executionContext: ExecutionContext) extends OidcSecurity {

  def contactForm: Form[Contact] = Form[Contact](
    mapping(
      "email" -> email.verifying("Please enter an email address", a => a.nonEmpty),
      "name" -> nonEmptyText,
      "student" -> optional(text),
      "level" -> optional(text),
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
      if (lessonLength > 0) {
        val amount = amountService.calculateAmount(numOfLessons, lessonLength)
        for {
          student <- studentRepository.addStudent(contact, amount, None)
          paymentIntent = stripeConfiguration.paymentIntent(amount, student.id)
          _ <- studentRepository.updatePaymentIntentId(student.id, paymentIntent.getId)
          booking <- createBooking(numOfLessons, lessonLength, date, time, student.id)
          _ <- timesRepository.addTimes(booking)
        } yield {
          Redirect(routes.BookingController.bookingSummary(numOfLessons, lessonLength, date, time, student.id))
        }
      } else {
        for {
          student <- studentRepository.addStudent(contact, 0, chargeCompleted = true)
          booking <- createBooking(numOfLessons, 30, date, time, student.id)
          times <- timesRepository.addTimes(booking)
        } yield {
          times.map(time => {
            val contact = Contact(student.email, student.name, student.student, student.level, student.phone, student.notes)
            calendarService.putEvent(time.startDate, time.endDate, contact)
          })
          Ok(views.html.paymentConfirmation(booking))
        }
      }
    })
  }

  def times(numOfLessons: Int, lessonLength: Int, date: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val updateLessonLength = if (lessonLength == 0) 30 else lessonLength
    val slots = calendarService.getAvailableSlots(date, numOfLessons, updateLessonLength)
    Ok(views.html.times(numOfLessons, lessonLength, date, slots))

  }

  def bookingSummary(numOfLessons: Int, lengthOfLesson: Int, date: String, time: String, studentId: UUID): Action[AnyContent] = Action.async { implicit request: Request[Any] =>
    val apiKey = configuration.get[String]("stripe.public")
    createBooking(numOfLessons, lengthOfLesson, date, time, studentId).map(booking => Ok(views.html.bookingSummary(booking, apiKey)))
  }

  private def createBooking(numOfLessons: Int, lengthOfLesson: Int, date: String, time: String, studentId: UUID) = {
    val cost = amountService.calculateAmount(numOfLessons, lengthOfLesson)
    val dates = (0 until numOfLessons).toList.map(plusWeeks => {
      val localDateTime = LocalDateTime.parse(s"${date}T$time:00").plusWeeks(plusWeeks)
      localDateTime.format(formattedPattern)
    })
    studentRepository.getStudent(studentId).map(student => {
      val email = student.head.email
      Booking(studentId, email, numOfLessons, lengthOfLesson, dates, cost)
    })
  }
}

object BookingController {
  case class Booking(studentId: UUID, email: String, numberOfLessons: Int, lengthOfLesson: Int, dates: List[String], totalCost: Int)

  case class Contact(email: String, name: String, student: Option[String], level: Option[String], phone: String, notes: Option[String]) {
    override def toString: String = {
      s"""
         |Email: $email
         |Name: $name
         |Student Name: ${student.getOrElse("")}
         |Level: ${level.getOrElse("")}
         |Phone: $phone
         |Notes: ${notes.getOrElse("")}
         |""".stripMargin
    }
  }
}
