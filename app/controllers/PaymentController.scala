package controllers

import auth.OidcSecurity
import com.google.inject.Inject
import configuration.StripeConfiguration
import controllers.PaymentController._
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.libs.json.Writes._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Request}

class PaymentController @Inject()(val controllerComponents: SecurityComponents,
                                  stripeConfiguration: StripeConfiguration,
                                  configuration: Configuration
                                 ) extends OidcSecurity {

  def pay(): Action[AnyContent] = secureAction { implicit request: Request[Any] =>
    Ok(views.html.pay(configuration.get[String]("stripe.public")))
  }

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

  def paymentIntent(): Action[AnyContent] = Action { implicit request: Request[Any] =>
    val paymentIntent = stripeConfiguration.paymentIntent()
    val response = Json.toJson(CreatePaymentResponse(paymentIntent.getClientSecret))
    Ok(Json.stringify(response))
  }
}

object PaymentController {
  case class CreatePaymentResponse(clientSecret: String)

  case class ChargeEvent(id: String)

  implicit val reads: Reads[ChargeEvent] = Json.reads[ChargeEvent]
  implicit val writes: OWrites[CreatePaymentResponse] = Json.writes[CreatePaymentResponse]

}
