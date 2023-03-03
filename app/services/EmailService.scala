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

  def send(emailAddress: String, body: String): Future[WSResponse] = {
    val fromEmail = "noreply@clairepalmerpiano.co.uk"
    val personalisations = Personalisation(List(EmailRecipient(emailAddress))) :: Nil
    val content = EmailContent("text/plain", body) :: Nil
    val email = Email(personalisations, EmailRecipient(fromEmail), "A new booking", content).asJson.printWith(noSpaces)
    val auth = s"Bearer ${configuration.get[String]("sendgrid.key")}"
    ws.url(configuration.get[String]("sendgrid.url"))
      .addHttpHeaders(("Authorization", auth), ("Content-Type", "application/json"))
      .post(email)
  }
}
object EmailService {
  case class EmailRecipient(email: String)
  case class Personalisation(to: List[EmailRecipient])
  case class EmailContent(`type`: String, value: String)
  case class Email(personalizations: List[Personalisation], from: EmailRecipient, subject: String, content: List[EmailContent])
}
