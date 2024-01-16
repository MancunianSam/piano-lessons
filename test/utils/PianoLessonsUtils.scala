package utils

import com.typesafe.config.ConfigFactory
import controllers.NewsController.NewsForm
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, doReturn}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultSecurityLogic
import org.pac4j.core.http.ajax.AjaxRequestResolver
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.oauth.client.Google2Client
import org.pac4j.oauth.profile.google2.Google2Profile
import org.pac4j.play.PlayWebContext
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.scala.SecurityComponents
import org.pac4j.play.store.PlayCacheSessionStore
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.Helpers.stubMessagesControllerComponents
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile

import java.sql._
import java.time.{Instant, LocalDate}
import java.util.{Properties, UUID}
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.reflect.ClassTag

trait PianoLessonsUtils extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with BeforeAndAfterAll {

  private def securityComponents(testConfig: Config, playCacheSessionStore: SessionStore): SecurityComponents = new SecurityComponents {
    override def components: ControllerComponents = stubMessagesControllerComponents()

    override def config: Config = testConfig

    override def sessionStore: SessionStore = playCacheSessionStore

    override def parser: BodyParsers.Default = new BodyParsers.Default()
  }

  def unauthorisedSecurityComponents: SecurityComponents = {
    val testConfig = new Config()
    val logic = DefaultSecurityLogic.INSTANCE
    logic setAuthorizationChecker ((_, _, _, _, _, _) => false)
    testConfig.setSecurityLogic(logic)

    // There is a null check for the action adaptor.
    testConfig.setHttpActionAdapter(new PlayHttpActionAdapter())

    // There is a check to see whether an OidcClient exists. The name matters and must match the string passed to Secure in the controller.
    val clients = new Clients()

    val resolver = mock[AjaxRequestResolver]
    doReturn(false).when(resolver).isAjax(any[PlayWebContext], any[SessionStore])

    // Create a concrete client
    val client = new Google2Client("id", "secret")
    client.setName("Google2Client")
    client.setAjaxRequestResolver(resolver)
    client.setCallbackUrl("test")

    clients.setClients(client)
    testConfig.setClients(clients)
    securityComponents(testConfig, mock[PlayCacheSessionStore])

  }

  def authorisedSecurityComponents: SecurityComponents = {
    val profile: Google2Profile = new Google2Profile()

    val profileMap: java.util.LinkedHashMap[String, Google2Profile] = new java.util.LinkedHashMap[String, Google2Profile]
    profileMap.put("OidcClient", profile)

    val playCacheSessionStore: SessionStore = mock[PlayCacheSessionStore]

    doAnswer(_ => java.util.Optional.of(profileMap))
      .when(playCacheSessionStore)
      .get(
        any[PlayWebContext](),
        org.mockito.ArgumentMatchers.eq[String](Pac4jConstants.USER_PROFILES)
      )

    val testConfig = new Config()

    val logic = DefaultSecurityLogic.INSTANCE
    logic.setAuthorizationChecker((_, _, _, _, _, _) => true)
    testConfig.setSecurityLogic(logic)

    testConfig.setHttpActionAdapter(new PlayHttpActionAdapter())

    val clients = new Clients()

    clients.setClients(new Google2Client())
    testConfig.setClients(clients)

    securityComponents(testConfig, playCacheSessionStore)
  }

  def createDbConfigProvider(port: Int): DatabaseConfigProvider = {
    new DatabaseConfigProvider {
      override def get[P <: BasicProfile]: DatabaseConfig[P] = {
        val cls: ClassTag[P] = ClassTag[P](classOf[JdbcProfile])
        val config = ConfigFactory
          .parseMap(Map("slick.dbs.default.db.url" -> s"jdbc:postgresql://localhost:$port/piano-lessons").asJava)
          .withFallback(ConfigFactory.load())
        DatabaseConfig.forConfig("slick.dbs.default", config)(cls)

      }
    }
  }

  def getConnection(url: String): Connection = {
    val properties = new Properties()
    properties.put("user", "piano")
    properties.put("password", "password")
    DriverManager.getConnection(url, properties)
  }

  def createNews(connection: Connection, title: String, body: String, date: LocalDate): Int = {
    val sql = "INSERT INTO news (id, date, title, body) VALUES (?, ?, ?, ?)"
    val ps: PreparedStatement = connection.prepareStatement(sql)
    ps.setObject(1, UUID.randomUUID(), Types.OTHER)
    ps.setDate(2, java.sql.Date.valueOf(date))
    ps.setString(3, title)
    ps.setString(4, body)
    ps.executeUpdate()
  }

  def createTimes(connection: Connection, studentId: UUID): Int = {
    val sql = "INSERT INTO times (id, number_of_lessons, length_of_lessons, start_date, end_date, student_id) VALUES (?, ?, ?, ?, ?, ?)"
    val ps: PreparedStatement = connection.prepareStatement(sql)
    ps.setObject(1, UUID.randomUUID(), Types.OTHER)
    ps.setInt(2, 1)
    ps.setInt(3, 2)
    ps.setTimestamp(4, Timestamp.from(Instant.now()))
    ps.setTimestamp(5, Timestamp.from(Instant.now()))
    ps.setObject(6, studentId, Types.OTHER)
    ps.executeUpdate()
  }

  def getNews(connection: Connection): NewsForm = {
    val sql = "SELECT title, body FROM news"
    val ps: PreparedStatement = connection.prepareStatement(sql)
    val rs = ps.executeQuery()
    rs.next()
    val title = rs.getString(1)
    val body = rs.getString(2)
    NewsForm(title, body)
  }

  def getPaymentConfirmation(connection: Connection, id: UUID): Boolean = {
    val sql = "SELECT payment_confirmed FROM student where id = ?"
    val ps: PreparedStatement = connection.prepareStatement(sql)
    ps.setObject(1, id, Types.OTHER)
    val rs = ps.executeQuery()
    rs.next()
    rs.getBoolean(1)
  }

  def createStudent(connection: Connection, id: UUID, paymentIntentId: Option[String] = None): Int = {
    val sql = if (paymentIntentId.isDefined) {
      "INSERT INTO student (id, email, name, phone, total_cost, payment_intent_id) VALUES (?, ?, ?, ?, ?, ?)"
    } else {
      "INSERT INTO student (id, email, name, phone, total_cost) VALUES (?, ?, ?, ?, ?)"
    }
    val ps: PreparedStatement = connection.prepareStatement(sql)
    ps.setObject(1, id, Types.OTHER)
    ps.setString(2, "test@test.com")
    ps.setString(3, "Name")
    ps.setString(4, "Phone")
    ps.setInt(5, 1000)
    paymentIntentId.foreach(id => ps.setString(6, id))
    ps.executeUpdate()
  }

}
