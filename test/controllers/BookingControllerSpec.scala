package controllers

import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import com.typesafe.config.ConfigFactory
import modules.{LocalGoogleService, LocalStripeService}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.Play.materializer
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.Status.SEE_OTHER
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.Helpers._
import play.api.test._
import repositories.{StudentRepository, TimesRepository}
import services.AmountService.Prices
import services.{AmountService, BookingService, CalendarService, EmailService}
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile
import utils.PianoLessonsUtils

import java.sql.{DriverManager, PreparedStatement, Types}
import java.util.{Calendar, Locale, Properties, UUID}
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.reflect.ClassTag

class BookingControllerSpec extends PianoLessonsUtils with TestContainersForAll with TableDrivenPropertyChecks {
  implicit val ec: ExecutionContext = ExecutionContext.global

  val sendGridServer = new WireMockServer(9002)
  sendGridServer.stubFor(post(urlEqualTo("/")).willReturn(ok()))
  sendGridServer.start()

  val testGoogleService = new LocalGoogleService()
  val testStripeConfiguration = new LocalStripeService()


  "BookingController chooseLength" should {
    "render the choose lesson length page" in withContainers { container: PostgreSQLContainer =>
      val response = createController(container.mappedPort(5432))
        .chooseLength().apply(FakeRequest(GET, "/choose-length"))
      contentAsString(response) must include("30 MINUTE LESSON")
      contentAsString(response) must include("60 MINUTE LESSON")
      contentAsString(response) must include("FREE TRIAL LESSON")
    }
  }

  "BookingController book" should {
    val priceTable = Table(
      ("lessonLength", "expectedPrices"),
      (30, Prices(15, 45, 80)),
      (60, Prices(30, 90, 170)),
    )
    forAll(priceTable) { (lessonLength, prices) =>
      s"render the correct prices on the booking page for a $lessonLength minute lesson" in withContainers { container: PostgreSQLContainer =>
        val response = createController(container.mappedPort(5432))
          .book(lessonLength).apply(FakeRequest(GET, s"/book/$lessonLength"))
        contentAsString(response) must include(s"£${prices.oneLesson}")
        contentAsString(response) must include(s"£${prices.threeLessons}")
        contentAsString(response) must include(s"£${prices.sixLessons}")
      }
    }
  }

  "BookingController calendar" should {
    val monthTable = Table(
      ("month", "monthName"),
      (Option(2), "March"),
      (Option(5), "June"),
      (Option(8), "September"),
    )

    forAll(monthTable) { (month, monthName) =>
      s"render the correct month for $monthName" in withContainers { container: PostgreSQLContainer =>
        val year = Calendar.getInstance.get(Calendar.YEAR)
        val response = createController(container.mappedPort(5432))
          .calendar(1, 30, month, Option(year))
          .apply(FakeRequest(GET, "/calendar/"))
        contentAsString(response) must include(monthName)
      }
    }

    "redirect to the current year if trying to book for next year" in withContainers { container: PostgreSQLContainer =>
      val nextYear = Calendar.getInstance.get(Calendar.YEAR) + 1
      val response = createController(container.mappedPort(5432))
        .calendar(1, 30, Option(1), Option(nextYear))
        .apply(FakeRequest(GET, "/calendar/"))

      status(response) must equal(SEE_OTHER)
    }

    "render the current month if no month is passed" in withContainers { container: PostgreSQLContainer =>
      val currentMonth = Calendar.getInstance.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
      val response = createController(container.mappedPort(5432))
        .calendar(1, 30, None, None)
        .apply(FakeRequest(GET, "/calendar/"))

      contentAsString(response) must include(currentMonth)
    }
  }

  "BookingController bookingContactDetails" should {
    "render the contact details page" in withContainers { container: PostgreSQLContainer =>
      val numOfLessons = 1
      val lessonLength = 30
      val date = "2023-03-28"
      val time = "09:00"
      val response = createController(container.mappedPort(5432))
        .bookingContactDetails(numOfLessons, lessonLength, date, time)
        .apply(
          FakeRequest(GET, s"/booking-contact/$numOfLessons/$lessonLength/$date/$time")
            .withCSRFToken
        )
      val content = contentAsString(response)
      content must include("email")
      content must include("name")
      content must include("student")
      content must include("phone")
      content must include("notes")
    }
  }

  "BookingController saveBookingContactDetails" should {
    val numOfLessons = 1
    val lessonLength = 30
    val date = "2023-03-28"
    val time = "09:00"
    "render the payment page on successful form submit" in withContainers { container: PostgreSQLContainer =>
      val form = Seq(("email", "test@test.com"), ("name", "Name"), ("phone", "Phone"))
      val response = createController(container.mappedPort(5432))
        .saveBookingContactDetails(numOfLessons, lessonLength, date, time)
        .apply(
          FakeRequest(POST, s"/booking-contact/$numOfLessons/$lessonLength/$date/$time")
            .withFormUrlEncodedBody(form: _*)
        )
      status(response) must equal(SEE_OTHER)
    }

    "render the booking confirmation page for a free lesson" in withContainers { container: PostgreSQLContainer =>
      val form = Seq(("email", "test@test.com"), ("name", "Name"), ("phone", "Phone"))
      val response = createController(container.mappedPort(5432))
        .saveBookingContactDetails(numOfLessons, 0, date, time)
        .apply(
          FakeRequest(POST, s"/booking-contact/$numOfLessons/0/$date/$time")
            .withFormUrlEncodedBody(form: _*)
        )
      val content = contentAsString(response)
      content must include("Thanks for your booking")
    }

    "render the errors if mandatory fields aren't filled in" in withContainers { container: PostgreSQLContainer =>
      val form = Seq(("name", "Name"), ("phone", "Phone"))
      val response = createController(container.mappedPort(5432))
        .saveBookingContactDetails(numOfLessons, 0, date, time)
        .apply(
          FakeRequest(POST, s"/booking-contact/$numOfLessons/0/$date/$time")
            .withFormUrlEncodedBody(form: _*)
            .withCSRFToken
        )
      val content = contentAsString(response)
      content must include("error.required")
      content must include("#email")
    }
  }

  "BookingController times" should {
    "render the times page" in withContainers { container: PostgreSQLContainer =>
      val numOfLessons = 1
      val lessonLength = 30
      val date = "2023-03-28"
      val response = createController(container.mappedPort(5432))
        .times(numOfLessons, lessonLength, date)
        .apply(FakeRequest(GET, s"/times/$numOfLessons/$lessonLength/$date"))
      val content = contentAsString(response)
      content must include("09:00")
    }
  }

  "BookingController bookingSummary" should {
    "render the summary and payment page" in withContainers { container: PostgreSQLContainer =>
      val numOfLessons = 1
      val lessonLength = 30
      val date = "2023-03-28"
      val time = "09:00"
      val id = UUID.randomUUID()
      createStudent(container.jdbcUrl, id)
      val response = createController(container.mappedPort(5432))
        .bookingSummary(numOfLessons, lessonLength, date, time, id)
        .apply(
          FakeRequest(GET, s"/booking-summary/$numOfLessons/$lessonLength/$date/$time/$id")
            .withCSRFToken
        )
      val content = contentAsString(response)
      content must include("payment-form")
    }
  }

  private def createStudent(url: String, id: UUID): Int = {
    val properties = new Properties()
    properties.put("user", "piano")
    properties.put("password", "password")
    val connection = DriverManager.getConnection(url, properties)

    val sql = "INSERT INTO student (id, email, name, phone) VALUES (?, ?, ?, ?)"
    val ps: PreparedStatement = connection.prepareStatement(sql)
    ps.setObject(1, id, Types.OTHER)
    ps.setString(2, "test@test.com")
    ps.setString(3, "Name")
    ps.setString(4, "Phone")
    ps.executeUpdate()
  }

  private def createController(port: Int) = {
    val calendarService = new CalendarService(testGoogleService)
    val amountService = new AmountService()

    val dbConfigProvider = new DatabaseConfigProvider {

      override def get[P <: BasicProfile]: DatabaseConfig[P] = {
        val cls: ClassTag[P] = ClassTag[P](classOf[JdbcProfile])
        val config = ConfigFactory.parseMap(Map("slick.dbs.default.db.url" -> s"jdbc:postgresql://localhost:$port/piano-lessons").asJava)
          .withFallback(ConfigFactory.load())
        DatabaseConfig.forConfig("slick.dbs.default", config)(cls)
      }
    }
    val studentRepository = new StudentRepository(dbConfigProvider)
    val timesRepository = new TimesRepository(dbConfigProvider)
    val configuration = Configuration.load(app.environment)

    val emailService = new EmailService(configuration)
    val bookingService = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, testStripeConfiguration)
    new BookingController(stubMessagesControllerComponents(), calendarService, amountService, bookingService, configuration)
  }

  override type Containers = PostgreSQLContainer

  override def startContainers(): PostgreSQLContainer = {
    val container = PostgreSQLContainer.Def(
      databaseName = "piano-lessons",
      username = "piano",
      password = "password",
      commonJdbcParams = CommonParams(initScriptPath = Option("init.sql"))
    ).createContainer()

    container.start()
    container
  }

  override def afterContainersStart(containers: PostgreSQLContainer): Unit = {
    super.afterContainersStart(containers)
  }
}
