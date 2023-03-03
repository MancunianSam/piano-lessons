package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import controllers.BookingController.{Booking, Contact}
import org.pac4j.play.scala.SecurityComponents
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request}
import services.{CalendarService, EmailService}

import scala.concurrent.{ExecutionContext, Future}

class BookingController @Inject()(
                                   val controllerComponents: SecurityComponents,
                                   val calendarService: CalendarService,
                                   val emailService: EmailService
                                 )(implicit ec: ExecutionContext) extends OidcSecurity {
  val contactForm: Form[Contact] = Form(
    mapping(
      "email" -> email,
      "name" -> nonEmptyText,
      "phone" -> nonEmptyText,
      "notes" -> optional(text)
    )(Contact.apply)(Contact.unapply)
  )

  val bookingForm: Form[Booking] = Form(
    mapping(
      "date" -> text,
      "slot" -> optional(text)
    )(Booking.apply)(Booking.unapply))

  def book(date: String): Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    val availableSlots = if(date.isEmpty) {
      Nil
    } else {
      calendarService.getAvailableSlots(date)
    }
    Ok(views.html.book(bookingForm.fill(Booking(date)), availableSlots))
  }

  def availability(): Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    bookingForm.bindFromRequest().fold(formWithErrors => {
      Ok(views.html.book(formWithErrors, Nil))
    }, booking => {
      Redirect(routes.BookingController.book(booking.date))
    })
  }

  def bookSlot(): Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    bookingForm.bindFromRequest().fold(formWithErrors => {
      Ok(views.html.book(formWithErrors, Nil))
    }, booking => {
      println("ok")
      booking.slot.map(slot => {
        Redirect(routes.BookingController.bookContact(booking.date, slot))
      }).getOrElse(Redirect(routes.BookingController.book(booking.date)))

    })
  }

  def bookContact(date: String, hour: String) : Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    Ok(views.html.bookingDetails(contactForm, date, hour))
  }

  def submitContactDetails(date: String, hour: String): Action[AnyContent] = secureAction.async { implicit request: Request[Any] =>
    contactForm.bindFromRequest().fold(formWithErrors => {
      Future.successful(BadRequest(views.html.bookingDetails(formWithErrors, date, hour)))
    }, contactForm => for {
      _ <- calendarService.putEvent(date, hour, contactForm)
      _ <- emailService.send(contactForm.email, contactForm.toString)
    } yield Redirect(routes.BookingController.thanks()))
  }

  def thanks(): Action[AnyContent] = secureAction { _ =>
    Ok(views.html.thanks())
  }
}
object BookingController {
  case class Booking(date: String, slot: Option[String] = None)

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
