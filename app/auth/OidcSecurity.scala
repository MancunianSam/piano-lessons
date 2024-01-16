package auth

import org.pac4j.core.authorization.authorizer.DefaultAuthorizers
import org.pac4j.core.profile.CommonProfile
import org.pac4j.play.scala.{SecureAction, Security}
import play.api.i18n.I18nSupport
import play.api.mvc.AnyContent

trait OidcSecurity extends Security[CommonProfile] with I18nSupport {
  val secureAction: SecureAction[CommonProfile, AnyContent, AuthenticatedRequest] = Secure("Google2Client", authorizers = DefaultAuthorizers.NONE)
}
