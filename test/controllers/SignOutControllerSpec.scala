package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, stubControllerComponents}
import utils.PianoLessonsUtils

class SignOutControllerSpec extends PianoLessonsUtils {
  "SignOutController signedOut" should {
    "render the sign out page" in {
      val controller = new SignOutController(stubControllerComponents())
      val response = controller.signedOut()(FakeRequest(GET, "/signed-out"))
      contentAsString(response) must include ("You have signed out")
    }
  }
}
