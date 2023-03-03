package services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json
import play.api.test.WsTestClient.InternalWSClient

class EmailServiceSpec extends PlaySpec with ScalaFutures {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(100, Millis)))

  "EmailService sendEmail" should {
    val emailServer = new WireMockServer(9007)
    emailServer.stubFor(
      post(urlEqualTo("/email"))
        .willReturn(ok())
    )
    emailServer.start()
    "send an email" in {
      val config = Configuration.from(Map("sendgrid.key" -> "key", "sendgrid.url" -> "http://localhost:9007/email"))
      val wsClient = new InternalWSClient("http", 9007)
      val emailService = new EmailService(wsClient, config)
      emailService.send("test@test.com", "Test body").futureValue
      val body = Json.parse(emailServer.getAllServeEvents.get(0).getRequest.getBodyAsString)

      (body \ "personalizations" \ 0 \ "to" \ 0 \ "email").as[String] must be("test@test.com")
      (body \ "subject").as[String] must be("A new booking")
      (body \ "content" \ 0 \ "value").as[String] must be("Test body")
    }
  }
}
