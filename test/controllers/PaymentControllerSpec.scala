package controllers

import io.circe.syntax._
import io.circe.generic.auto._
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration
import controllers.BookingController.Contact
import modules.{LocalGoogleService, LocalStripeService}
import play.api.Configuration
import play.api.Play.materializer
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import repositories.{StudentRepository, TimesRepository}
import services.{AmountService, BookingService, CalendarService, EmailService}
import controllers.PaymentController._
import io.circe.Printer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar._
import services.EmailService.Email
import utils.{PianoLessonsUtils, TestContainersUtils}
import io.circe.syntax._
import io.circe.parser.decode
import io.circe.generic.auto._

import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.ListHasAsScala

class PaymentControllerSpec extends PianoLessonsUtils with TestContainersUtils {
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(50, Seconds)), interval = scaled(Span(100, Millis)))
  val stripeConfiguration: StripeConfiguration = new StripeConfiguration {
    private def intent(secret: String) = {
      val paymentIntent = new PaymentIntent()
      paymentIntent.setClientSecret(secret)
      paymentIntent
    }

    override def getPaymentIntent(paymentIntentId: String): PaymentIntent = intent("existing-secret")

    override def paymentIntent(amount: Long, studentId: UUID): PaymentIntent = {
      intent("new-secret")
    }
  }

  val sendGridServer = new WireMockServer(9002)

  override def beforeAll(): Unit = {
    sendGridServer.stubFor(post(urlEqualTo("/")).willReturn(ok()))
    sendGridServer.start()
  }

  override def afterAll(): Unit = {
    sendGridServer.stop()
  }

  "PaymentController paymentIntent" should {
    "return the existing payment intent for an existing student" in withContainers { container: PostgreSQLContainer =>
      val studentId = UUID.randomUUID()
      val connection = getConnection(container.jdbcUrl)
      createStudent(connection, studentId, Option("payment_intent_id"))
      val jsonInput = IntentInput(studentId, 1, 30).asJson.printWith(Printer.noSpaces)

      val fakeRequest =
        FakeRequest(POST, "/payment-intent")
          .withBody(jsonInput)
          .withHeaders(("Content-Type", "application/json"))
      val action = createController(container.mappedPort(5432))
        .paymentIntent()
      val response = call(action, fakeRequest)
      val json = contentAsJson(response)
      (json \ "clientSecret").as[String] must be("existing-secret")
    }

    "return a new payment intent for a non existent student" in withContainers { container: PostgreSQLContainer =>
      val studentId = UUID.randomUUID()
      val jsonInput = IntentInput(studentId, 1, 30).asJson.printWith(Printer.noSpaces)
      val fakeRequest =
        FakeRequest(POST, "/payment-intent")
          .withBody(jsonInput)
          .withHeaders(("Content-Type", "application/json"))
      val action = createController(container.mappedPort(5432))
        .paymentIntent()
      val response = call(action, fakeRequest)
      val json = contentAsJson(response)
      (json \ "clientSecret").as[String] must be("new-secret")
    }

    "return a new payment intent for an existing student with no stored intent id" in withContainers { container: PostgreSQLContainer =>
      val studentId = UUID.randomUUID()
      val connection = getConnection(container.jdbcUrl)
      createStudent(connection, studentId)
      val jsonInput = IntentInput(studentId, 1, 30).asJson.printWith(Printer.noSpaces)
      val fakeRequest =
        FakeRequest(POST, "/payment-intent")
          .withBody(jsonInput)
          .withHeaders(("Content-Type", "application/json"))
      val action = createController(container.mappedPort(5432))
        .paymentIntent()
      val response = call(action, fakeRequest)
      val json = contentAsJson(response)
      (json \ "clientSecret").as[String] must be("new-secret")
    }

    "return an error if the incoming json is invalid" in withContainers { container: PostgreSQLContainer =>
      val jsonInput = "{\"invalid\": 1}"
      val fakeRequest =
        FakeRequest(POST, "/payment-intent")
          .withBody(jsonInput)
          .withHeaders(("Content-Type", "application/json"))
      val action = createController(container.mappedPort(5432))
        .paymentIntent()
      val response = call(action, fakeRequest)
      val responseStatus = status(response)
      responseStatus must be(BAD_REQUEST)
    }
  }

  "PaymentController webhook" should {
    "add the event to the calendar and update the booking" in withContainers { container: PostgreSQLContainer =>
      val calendarService = mock[CalendarService]
      val studentId = UUID.randomUUID()
      val connection = getConnection(container.jdbcUrl)
      createStudent(connection, studentId, Option("intent"))
      createTimes(connection, studentId)
      val controller = createController(container.mappedPort(5432), calendarService)
      val action = controller.webhook()
      val request = FakeRequest(POST, "/webhook")
        .withHeaders(("Content-Type", "application/json"))
        .withBody(ChargeEvent("id", ChargeEventData(ChargeEventObject("intent")), "charge.succeeded").asJson.printWith(Printer.noSpaces))

      call(action, request).futureValue

      verify(calendarService, times(1)).putEvent(any[Timestamp], any[Timestamp], any[Contact])

      val serveEvents = sendGridServer.getAllServeEvents.asScala
      def getToAddress(serveEvent: ServeEvent): String =
        decode[Email](serveEvent.getRequest.getBodyAsString).toOption.get.personalizations.headOption.flatMap(_.to.headOption).get.email

      getToAddress(serveEvents.head) must equal("bookings@clairepalmerpiano.co.uk")
      getToAddress(serveEvents.last) must equal("test@test.com")

      getPaymentConfirmation(connection, studentId) must equal(true)
    }

    "return the original id if the type is not charge.succeeded" in withContainers { container: PostgreSQLContainer =>
      val controller = createController(container.mappedPort(5432))
      val action = controller.webhook()
      val request = FakeRequest(POST, "/webhook")
        .withHeaders(("Content-Type", "application/json"))
        .withBody(ChargeEvent("id", ChargeEventData(ChargeEventObject("intent")), "another.type").asJson.printWith(Printer.noSpaces))

      val res = call(action, request)

      val json = contentAsJson(res)
      (json \ "id").as[String] must be("id")
    }

    "return an error if the incoming json is invalid" in withContainers { container: PostgreSQLContainer =>
      val controller = createController(container.mappedPort(5432))
      val action = controller.webhook()
      val request = FakeRequest(POST, "/webhook")
        .withHeaders(("Content-Type", "application/json"))
        .withBody("{}")

      val res = call(action, request)

      val statusCode = status(res)
      statusCode must equal(400)
    }
  }

  "PaymentController paymentConfirmation" should {
    "render the payment confirmation page" in withContainers { container: PostgreSQLContainer =>
      val studentId = UUID.randomUUID()
      val intentId = Option("intent")
      val connection = getConnection(container.jdbcUrl)
      createStudent(connection, studentId, intentId)
      createTimes(connection, studentId)
      val controller = createController(container.mappedPort(5432))
      val action = controller.paymentConfirmation(intentId)
      val request = FakeRequest(GET, s"/payment-confirmation?payment_intent=intent")
      val response = call(action, request)
      val responseContent = contentAsString(response)
      responseContent must include("Your booking summary")
    }

    "redirect to home if no student is found" in withContainers { container: PostgreSQLContainer =>
      val intentId = Option("missing_student_intent")
      val controller = createController(container.mappedPort(5432))
      val action = controller.paymentConfirmation(intentId)
      val request = FakeRequest(GET, s"/payment-confirmation?payment_intent=intent")
      val response = action.apply(request)
      val statusValue = status(response)
      statusValue must equal(SEE_OTHER)
    }
  }

  private def createController(port: Int): PaymentController = {
    val calendarService = new CalendarService(new LocalGoogleService)
    createController(port, calendarService)
  }

  private def createController(port: Int, calendarService: CalendarService): PaymentController = {
    val amountService = new AmountService()
    val dbConfigProvider = createDbConfigProvider(port)
    val studentRepository = new StudentRepository(dbConfigProvider)
    val timesRepository = new TimesRepository(dbConfigProvider)
    val configuration = Configuration.load(app.environment)

    val emailService = new EmailService(configuration)
    val bookingService = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, new LocalStripeService)
    new PaymentController(
      stubMessagesControllerComponents(),
      stripeConfiguration,
      calendarService,
      studentRepository,
      timesRepository,
      amountService,
      emailService,
      bookingService
    )
  }
}
