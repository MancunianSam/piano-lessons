package services

import io.circe.Printer.noSpaces
import play.api.libs.ws.{WSClient, WSResponse}
import services.EmailService._
import io.circe.generic.auto._
import io.circe.syntax._
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.Future

class EmailService @Inject()(val ws: WSClient, configuration: Configuration) {

  def sendOrderEmail(emailAddress: String, order: Order): Future[WSResponse] = {
    val fromEmail = "noreply@clairepalmerpiano.co.uk"
    val templateId = configuration.get[String]("sendgrid.template")
    val personalisations = Personalisation(List(EmailRecipient(emailAddress)), OrderSummary(order)) :: Nil
    val email = Email(personalisations, EmailRecipient(fromEmail), templateId).asJson.printWith(noSpaces)
    val auth = s"Bearer ${configuration.get[String]("sendgrid.key")}"
    ws.url(configuration.get[String]("sendgrid.url"))
      .addHttpHeaders(("Authorization", auth), ("Content-Type", "application/json"))
      .post(email)
  }
}
object EmailService {
  case class OrderDates(number: Int, date: String)
  case class OrderSummary(order: Order)
  case class Order(lengthOfLessons: String, numberOfLessons: Int, totalCost: Int, dates: List[OrderDates])
  case class EmailRecipient(email: String)
  case class Personalisation(to: List[EmailRecipient], dynamic_template_data: OrderSummary)
  case class Email(personalizations: List[Personalisation], from: EmailRecipient, template_id: String)
}
