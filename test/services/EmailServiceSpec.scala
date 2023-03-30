package services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json
import services.EmailService._

import scala.concurrent.ExecutionContext

class EmailServiceSpec extends PlaySpec with ScalaFutures {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(100, Millis)))
  implicit val ec: ExecutionContext = ExecutionContext.global

  "EmailService sendEmail" should {
    val emailServer = new WireMockServer(9007)
    emailServer.stubFor(
      post(urlEqualTo("/"))
        .willReturn(ok())
    )
    emailServer.start()
    "send an order email" in {
      val config = Configuration.from(Map("sendgrid.key" -> "key", "sendgrid.url" -> "http://localhost:9007/", "sendgrid.template" -> "template"))
      val emailService = new EmailService(config)
      val order = Order("name", None, "test@test.com", "phone", "title", "30 minutes", 1, 1000, OrderDates(1, "23 March") :: Nil)
      emailService.sendOrderEmail("test@test.com", Option(order)).futureValue
      val body = Json.parse(emailServer.getAllServeEvents.get(0).getRequest.getBodyAsString)

      (body \ "personalizations" \ 0 \ "to" \ 0 \ "email").as[String] must be("test@test.com")

      val orderJson = (body \ "personalizations" \ 0 \ "dynamic_template_data" \ "order")
      (orderJson \ "name").as[String] must be("name")
      (orderJson \ "email").as[String] must equal("test@test.com")
      (orderJson \ "phone").as[String] must equal("phone")
      (orderJson \ "title").as[String] must equal("title")
      (orderJson \ "lengthOfLessons").as[String] must equal("30 minutes")
      (orderJson \ "numberOfLessons").as[Int] must equal(1)
      (orderJson \ "totalCost").as[Int] must equal(1000)
    }

    "throw an exception if the order is empty" in {
      val config = Configuration.from(Map("sendgrid.key" -> "key", "sendgrid.url" -> "http://localhost:9007/", "sendgrid.template" -> "template"))
      val emailService = new EmailService(config)
      val ex = emailService.sendOrderEmail("test@test.com", None).failed.futureValue
      ex.getMessage must equal("Missing data for student")
    }
  }
}
