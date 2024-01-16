package controllers

import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlayCacheSessionStore
import org.scalatestplus.mockito.MockitoSugar
import utils.PianoLessonsUtils

import java.util

class CustomAuthoriserSpec extends PianoLessonsUtils with MockitoSugar {

  "the custom authoriser" should {
    "return true if the email is correct" in {
      val profile = new BasicUserProfile()
      val attributes: util.Map[String, Object] = util.Map.of("email", "clairelpalmer4@gmail.com")
      profile.build("id", attributes)
      val isAuthorised = new CustomAuthoriser().isProfileAuthorized(mock[PlayWebContext], mock[PlayCacheSessionStore], profile)
      isAuthorised must equal(true)
    }

    "return false if the email is incorrect" in {
      val profile = new BasicUserProfile()
      val attributes: util.Map[String, Object] = util.Map.of("email", "another@gmail.com")
      profile.build("id", attributes)
      val isAuthorised = new CustomAuthoriser().isProfileAuthorized(mock[PlayWebContext], mock[PlayCacheSessionStore], profile)
      isAuthorised must equal(false)
    }
  }

}
