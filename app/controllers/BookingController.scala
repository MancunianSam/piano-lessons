package controllers

import com.google.inject.Inject
import controllers.BookingController.Contact
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import services.{AmountService, BookingService, CalendarService}

import java.time.{DayOfWeek, LocalDate}
import java.util.{Calendar, Locale, UUID}
import scala.concurrent.{ExecutionContext, Future}

class BookingController @Inject()(
                                   val controllerComponents: MessagesControllerComponents,
                                   val calendarService: CalendarService,
                                   val amountService: AmountService,
                                   val bookingService: BookingService,
                                   configuration: Configuration
                                 )(implicit val executionContext: ExecutionContext) extends MessagesBaseController {

  def contactForm: Form[Contact] = Form[Contact](
    mapping(
      "email" -> email,
      "name" -> text.verifying("Please enter your name", a => a.nonEmpty),
      "student" -> optional(text),
      "level" -> optional(text),
      "phone" -> text.verifying("Please enter a phone number", a => a.nonEmpty),
      "notes" -> optional(text)
    )
    (Contact.apply)(Contact.unapply)
  )

  def chooseLength(): Action[AnyContent] = Action { implicit request: MessagesRequest[Any] =>
    Ok(views.html.chooseLength())
  }

  def book(lessonLength: Int): Action[AnyContent] = Action { implicit request: MessagesRequest[Any] =>
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

  def bookingContactDetails(numOfLessons: Int, lessonLength: Int, date: String, time: String): Action[AnyContent] = Action { implicit request: MessagesRequest[Any] =>
    Ok(views.html.bookingContactDetails(contactForm, numOfLessons, lessonLength, date, time))
  }

  def saveBookingContactDetails(numOfLessons: Int, lessonLength: Int, date: String, time: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[Any] =>
    contactForm.bindFromRequest().fold(err => {
      Future.successful(BadRequest(views.html.bookingContactDetails(err, numOfLessons, lessonLength, date, time)))
    }, contact => {
      if (lessonLength > 0) {
        bookingService.createPaidBooking(numOfLessons, lessonLength, date, time, contact).map(id => {
          Redirect(routes.BookingController.bookingSummary(numOfLessons, lessonLength, date, time, id))
        })
      } else {
        bookingService.createFreeBooking(1, date, time, contact).map(booking => {
          Ok(views.html.paymentConfirmation(booking))
        })
      }
    })
  }

  def times(numOfLessons: Int, lessonLength: Int, date: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val updateLessonLength = if (lessonLength == 0) 30 else lessonLength
    val endHour = if(isWeekend(date)) 12 else 20
    val slots = calendarService.getAvailableSlots(date, numOfLessons, updateLessonLength, endHour)
    Ok(views.html.times(numOfLessons, lessonLength, date, slots))
  }

  def bookingSummary(numOfLessons: Int, lengthOfLesson: Int, date: String, time: String, studentId: UUID): Action[AnyContent] = Action.async { implicit request: Request[Any] =>
    val apiKey = configuration.get[String]("stripe.public")
    val cost = amountService.calculateAmount(numOfLessons, lengthOfLesson)
    bookingService.createBooking(numOfLessons, lengthOfLesson, date, time, studentId, cost)
      .map(booking => Ok(views.html.bookingSummary(booking, apiKey)))
  }

  private def isWeekend(date: String): Boolean = {
    val localDate = LocalDate.parse(date)
    List(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).contains(localDate.getDayOfWeek)
  }
}
object BookingController {
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
