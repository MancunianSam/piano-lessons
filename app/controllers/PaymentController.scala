package controllers

import auth.OidcSecurity
import services.EmailService.{Order, OrderDates}
import com.google.inject.Inject
import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration
import controllers.PaymentController._
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Request}
import repositories.{StudentRepository, Tables, TimesRepository}
import services.{AmountService, CalendarService, EmailService}
import controllers.BookingController.{Booking, Contact}
import play.api.libs.ws.WSResponse
import repositories.TimesRepository.formattedPattern

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PaymentController @Inject()(val controllerComponents: SecurityComponents,
                                  stripeConfiguration: StripeConfiguration,
                                  calendarService: CalendarService,
                                  studentRepository: StudentRepository,
                                  timesRepository: TimesRepository,
                                  amountService: AmountService,
                                  emailService: EmailService
                                 )(implicit val executionContext: ExecutionContext) extends OidcSecurity {

  def paymentConfirmation(paymentIntentId: Option[String]): Action[AnyContent] = Action.async { implicit request: Request[Any] =>
    timesRepository.getTimes(paymentIntentId.getOrElse("")).map(times => {
      val dates = times.map(_._1)
        .map(_.startDate.toLocalDateTime.format(formattedPattern))
        .toList
      (for {
        student <- times.map(_._2).headOption
        time <- times.headOption.map(_._1)
        totalCost <- student.totalCost
      } yield {
        val lengthOfLesson = time.lengthOfLessons
        val numberOfLessons = time.numberOfLessons
        val booking = Booking(student.id, student.email, numberOfLessons, lengthOfLesson, dates, totalCost.toInt)
        Ok(views.html.paymentConfirmation(booking))
      }).getOrElse(Redirect(routes.HomeController.index()))
    })
  }

  def webhook(): Action[JsValue] = Action.async(parse.json) { implicit request: Request[JsValue] =>
    request.body.validate[ChargeEvent].fold(
      errors => Future(BadRequest(Json.obj("message" -> JsError.toJson(errors)))),
      chargeEvent => {
        if (chargeEvent.`type` == "charge.succeeded") {
          val paymentIntentId = chargeEvent.data.`object`.payment_intent
          (for {
            _ <- studentRepository.updateChargeCompleted(paymentIntentId)
            rows <- timesRepository.getTimes(paymentIntentId)
            _ <- sendOrderEmail(rows)
          } yield {
            rows.map(row => {
              val (times, student) = row
              val contact = Contact(student.email, student.name, student.student, student.level, student.phone, student.notes)
              calendarService.putEvent(times.startDate, times.endDate, contact)
            })
          }).map(_ => Ok(Json.obj("" -> chargeEvent.id)))
        } else {
          Future.successful(Ok(Json.obj("" -> chargeEvent.id)))
        }
      }
    )
  }

  private def sendOrderEmail(rows: Seq[(Tables.TimesRow, Tables.StudentRow)]): Future[WSResponse] = {
    for {
      time <- rows.headOption.map(_._1)
      student <- rows.headOption.map(_._2)
      totalCost <- student.totalCost
    } yield {
      val dates = rows.map(_._1).map(_.startDate.toLocalDateTime).
        sorted.zipWithIndex.map { case (date, idx) =>
        OrderDates(idx + 1, date.format(formattedPattern))
      }.toList
      val order = Order(s"${time.lengthOfLessons} minutes", time.numberOfLessons, totalCost.toInt / 100, dates)
      emailService.sendOrderEmail(student.email, order)
    }
  } match {
    case Some(value) => value
    case None => Future.failed(new RuntimeException("Email sending failed"))
  }

  def paymentIntent(): Action[JsValue] = Action.async(parse.json) { implicit request: Request[JsValue] =>
    request.body.validate[IntentInput].fold(
      errors => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors)))),
      intentInput => {
        studentRepository.getStudent(intentInput.studentId).map(studentList => {
          val intentId = for {
            student <- studentList.headOption
            id <- student.paymentIntentId
          } yield id

          val paymentIntent: PaymentIntent = intentId match {
            case Some(intentId) =>
              stripeConfiguration.getPaymentIntent(intentId)
            case None =>
              stripeConfiguration.paymentIntent(amountService.calculateAmount(intentInput.numberOfLessons, intentInput.lengthOfLesson), studentList.head.id)
          }
          val response = Json.toJson(CreatePaymentResponse(paymentIntent.getClientSecret))
          Ok(response)
        })
      }
    )
  }
}

object PaymentController {
  case class CreatePaymentResponse(clientSecret: String)

  case class ChargeEventObject(payment_intent: String)

  case class ChargeEventData(`object`: ChargeEventObject)

  case class ChargeEvent(id: String, data: ChargeEventData, `type`: String)

  case class IntentInput(studentId: UUID, numberOfLessons: Int, lengthOfLesson: Int)

  implicit val chargeObjectReads: Reads[ChargeEventObject] = Json.reads[ChargeEventObject]
  implicit val chargeDataReads: Reads[ChargeEventData] = Json.reads[ChargeEventData]
  implicit val chargeReads: Reads[ChargeEvent] = Json.reads[ChargeEvent]
  implicit val studentIdInputReads: Reads[IntentInput] = Json.reads[IntentInput]
  implicit val paymentResponseWrites: OWrites[CreatePaymentResponse] = Json.writes[CreatePaymentResponse]

}
