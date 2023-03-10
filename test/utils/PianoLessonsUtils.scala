package utils

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, doNothing, doReturn}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultSecurityLogic
import org.pac4j.core.http.ajax.AjaxRequestResolver
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.{OidcProfile, OidcProfileDefinition}
import org.pac4j.oidc.redirect.OidcRedirectionActionBuilder
import org.pac4j.play.PlayWebContext
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.scala.SecurityComponents
import org.pac4j.play.store.PlayCacheSessionStore
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Play.materializer
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.Helpers.stubMessagesControllerComponents

import java.net.URI
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

trait PianoLessonsUtils extends PlaySpec with GuiceOneAppPerTest with ScalaFutures {
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
    val configuration = mock[OidcConfiguration]

    // Mock the init method to stop it calling out to the keycloak server
    doNothing().when(configuration).init()

    // Set some configuration parameters
    doReturn("id").when(configuration).getClientId
    doReturn("code").when(configuration).getResponseType
    doReturn(true).when(configuration).isUseNonce
    val providerMetadata = mock[OIDCProviderMetadata]
    doReturn(URI.create("/auth/realms/id/protocol/openid-connect/auth")).when(providerMetadata).getAuthorizationEndpointURI
    doReturn(providerMetadata).when(configuration).getProviderMetadata

    val resolver = mock[AjaxRequestResolver]
    doReturn(false).when(resolver).isAjax(any[PlayWebContext], any[SessionStore])

    // Create a concrete client
    val client = new OidcClient(configuration)
    client.setName("OidcClient")
    client.setAjaxRequestResolver(resolver)
    client.setRedirectionActionBuilder(new OidcRedirectionActionBuilder(client))
    client.setCallbackUrl("test")

    clients.setClients(client)
    testConfig.setClients(clients)
    securityComponents(testConfig, mock[PlayCacheSessionStore])

  }

  def authorisedSecurityComponents: SecurityComponents = {
    // Pac4j checks the session to see if there any profiles stored there. If there are, the request is authenticated.

    // Create the profile and add to the map
    val profile: OidcProfile = new OidcProfile()

    profile.addAttribute(OidcProfileDefinition.EXPIRATION, Date.from(LocalDateTime.now().plusDays(10).toInstant(ZoneOffset.UTC)))

    val profileMap: java.util.LinkedHashMap[String, OidcProfile] = new java.util.LinkedHashMap[String, OidcProfile]
    profileMap.put("OidcClient", profile)

    val playCacheSessionStore: SessionStore = mock[PlayCacheSessionStore]

    // Mock the get method to return the expected map.
    doAnswer(_ => java.util.Optional.of(profileMap))
      .when(playCacheSessionStore)
      .get(
        any[PlayWebContext](),
        org.mockito.ArgumentMatchers.eq[String](Pac4jConstants.USER_PROFILES)
      )

    val testConfig = new Config()

    // Return true on the isAuthorized method
    val logic = DefaultSecurityLogic.INSTANCE
    logic.setAuthorizationChecker((_, _, _, _, _, _) => true)
    testConfig.setSecurityLogic(logic)

    // There is a null check for the action adaptor.
    testConfig.setHttpActionAdapter(new PlayHttpActionAdapter())

    // There is a check to see whether an OidcClient exists. The name matters and must match the string passed to Secure in the controller.
    val clients = new Clients()
    val configuration = mock[OidcConfiguration]
    doNothing().when(configuration).init()

    clients.setClients(new OidcClient(configuration))
    testConfig.setClients(clients)

    // Create a new controller with the session store and config. The parser and components don't affect the tests.
    securityComponents(testConfig, playCacheSessionStore)
  }
}
