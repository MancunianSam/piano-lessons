package controllers

import org.pac4j.core.authorization.authorizer.ProfileAuthorizer
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.UserProfile

import java.util

class CustomAuthoriser extends ProfileAuthorizer {

  implicit class ProfileUtils(profile: UserProfile) {
    def matchesEmail(email: String): Boolean = Option(profile).map(_.getAttribute("email")).contains(email)
  }

  override def isProfileAuthorized(context: WebContext, sessionStore: SessionStore, profile: UserProfile): Boolean =
    profile.matchesEmail("clairelpalmer4@gmail.com") || profile.matchesEmail("sam.palmer2@gmail.com")

  override def isAuthorized(context: WebContext, sessionStore: SessionStore, profiles: util.List[UserProfile]): Boolean =
    isAnyAuthorized(context, sessionStore, profiles)
}
