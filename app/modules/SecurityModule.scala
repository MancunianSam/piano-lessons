package modules

import com.google.inject.{AbstractModule, Provides}
import controllers.CustomAuthoriser
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.core.profile.CommonProfile
import org.pac4j.oauth.client.Google2Client.Google2Scope
import org.pac4j.play.scala.{DefaultSecurityComponents, Pac4jScalaTemplateHelper, SecurityComponents}
import org.pac4j.play.store.PlayCacheSessionStore
import org.pac4j.play.{CallbackController, LogoutController}
import play.api.{Configuration, Environment}
import org.pac4j.oauth.client.{FacebookClient, Google2Client}

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  private val baseUrl = configuration.getOptional[String]("base.url").get
  private val callbackUrl = s"$baseUrl/callback"
  override def configure(): Unit = {
    bind(classOf[SessionStore]).to(classOf[PlayCacheSessionStore])

    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])

    bind(classOf[Pac4jScalaTemplateHelper[CommonProfile]])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    callbackController.setRenewSession(false)
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl(configuration.get[String]("logout.url"))
    // Logs out of the pac4j session. It does this by updating the pac4j class stored in redis
    logoutController.setLocalLogout(true)
    // Logs out of the keycloak session
    logoutController.setCentralLogout(true)
    bind(classOf[LogoutController]).toInstance(logoutController)
  }

  @Provides
  def provideGoogleClient: Google2Client = {
    val googleId = configuration.getOptional[String]("google.id").get
    val googleSecret = configuration.getOptional[String]("google.secret").get
    val client = new Google2Client(googleId, googleSecret)
    client.setCallbackUrl(callbackUrl)
    client.setScope(Google2Scope.EMAIL)
    client
  }

  @Provides
  def provideConfig(google2Client: Google2Client): Config = {
    val clients = new Clients(callbackUrl, google2Client)
    val config = new Config(clients)
    val callbackLogic = DefaultCallbackLogic.INSTANCE
    config.setCallbackLogic(callbackLogic)
    config.addAuthorizer("custom", new CustomAuthoriser())
    config
  }
}
