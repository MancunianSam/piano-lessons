package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration
import controllers.PaymentController._
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Request}
import repositories.StudentRepository
import services.{AmountService, CalendarService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PaymentController @Inject()(val controllerComponents: SecurityComponents,
                                  stripeConfiguration: StripeConfiguration,
                                  calendarService: CalendarService,
                                  studentRepository: StudentRepository,
                                  amountService: AmountService
                                 )(implicit val executionContext: ExecutionContext) extends OidcSecurity {

  def paymentConfirmation(): Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    Ok(views.html.paymentConfirmation())
  }

  def webhook(): Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] =>
    request.body.validate[ChargeEvent].fold(
      errors => BadRequest(Json.obj("message" -> JsError.toJson(errors))),
      chargeEvent => {

        Ok(Json.obj("" -> chargeEvent.id))
      }
    )
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

  case class ChargeEvent(id: String)

  case class IntentInput(studentId: UUID, numberOfLessons: Int, lengthOfLesson: Int)

  implicit val chargeReads: Reads[ChargeEvent] = Json.reads[ChargeEvent]
  implicit val studentIdInputReads: Reads[IntentInput] = Json.reads[IntentInput]
  implicit val paymentResponseWrites: OWrites[CreatePaymentResponse] = Json.writes[CreatePaymentResponse]

}
