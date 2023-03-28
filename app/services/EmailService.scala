package services

import io.circe.Printer.noSpaces
import play.api.libs.ws.{WSClient, WSResponse}
import services.EmailService._
import io.circe.generic.auto._
import io.circe.syntax._
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.Future
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

class EmailService @Inject()(configuration: Configuration) {
  private val backend: SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()

  def sendOrderEmail(emailAddress: String, order: Option[Order]): Future[Response[Either[String, String]]] = {
    order match {
      case Some(value) =>
        val fromEmail = "noreply@clairepalmerpiano.co.uk"
        val templateId = configuration.get[String]("sendgrid.template")
        val personalisations = Personalisation(List(EmailRecipient(emailAddress)), OrderSummary(value)) :: Nil
        val email = Email(personalisations, EmailRecipient(fromEmail), templateId).asJson.printWith(noSpaces)
        val auth = s"Bearer ${configuration.get[String]("sendgrid.key")}"
        basicRequest
          .body(email)
          .headers(Map("Authorization" -> auth, "Content-Type" -> "application/json"))
          .post(uri"${configuration.get[String]("sendgrid.url")}").send(backend)
      case None => Future.failed(new RuntimeException(s"Missing data for student"))
    }

  }
}
object EmailService {
  case class OrderDates(number: Int, date: String)
  case class OrderSummary(order: Order)
  case class Order(name: String, studentName: Option[String], email: String, phone: String, title: String, lengthOfLessons: String, numberOfLessons: Int, totalCost: Int, dates: List[OrderDates])
  case class EmailRecipient(email: String)
  case class Personalisation(to: List[EmailRecipient], dynamic_template_data: OrderSummary)
  case class Email(personalizations: List[Personalisation], from: EmailRecipient, template_id: String)
}
