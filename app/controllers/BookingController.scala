package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import controllers.BookingController.{Booking, Contact, Times}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request}
import services.{CalendarService, EmailService}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Calendar, Locale}
import scala.concurrent.{ExecutionContext, Future}

class BookingController @Inject()(
                                   val controllerComponents: SecurityComponents,
                                   val calendarService: CalendarService,
                                   val emailService: EmailService,
                                   configuration: Configuration
                                 ) extends OidcSecurity {


  def book(): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok(views.html.book())
  }

  def calendar(numberOfLessons: Int, monthOpt: Option[Int], yearOpt: Option[Int]): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val calendar = Calendar.getInstance()
    val thisMonth = calendar.get(Calendar.MONTH)
    val month = monthOpt.getOrElse(thisMonth)
    val year = yearOpt.getOrElse(calendar.get(Calendar.YEAR))
    if(month < thisMonth || month == 12) {
      Redirect(routes.BookingController.calendar(numberOfLessons, Option(thisMonth), yearOpt))
    } else {
      calendar.set(Calendar.MONTH, month)
      calendar.set(Calendar.DAY_OF_MONTH, 1)
      val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
      val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
      val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
      val allDays = (1 to maxDay).map(_.toString)
      val days: List[String] = List.fill(dayOfWeek)("") ++ allDays
      Ok(views.html.calendar(numberOfLessons, days, monthName, month, month != thisMonth, year))
    }
  }

  def times(numOfLessons: Int, date: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
//    val slots = calendarService.getAvailableSlots(date, numOfLessons)
    val slots =  List("09:00", "10:00", "11:00")
    Ok(views.html.times(numOfLessons, date, slots))

  }

  def bookingSummary(numOfLessons: Int, date: String, time: String): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val apiKey = configuration.get[String]("stripe.public")
    val cost = numOfLessons match {
      case 3 => 8500
      case 6 => 17000
      case i: Int => i * 3000
    }
    val dates = (0 until numOfLessons).toList.map(plusWeeks => {
      val localDateTime = LocalDateTime.parse(s"${date}T${time}:00").plusWeeks(plusWeeks)
      localDateTime.format(DateTimeFormatter.ofPattern(s"EEEE d LLLL yyyy HH mm"))
    })
    val booking = Booking(numOfLessons, dates, cost)
    Ok(views.html.bookingSummary(booking, apiKey))
  }

  def thanks(): Action[AnyContent] = secureAction { _ =>
    Ok(views.html.thanks())
  }
}

object BookingController {
  case class Booking(numberOfLessons: Int, dates: List[String], totalCost: Int)
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
