package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, stubMessagesControllerComponents}
import utils.PianoLessonsUtils

class HomeControllerSpec extends PianoLessonsUtils {
  "HomeController contact" should {
    "render the home page" in {
      val controller = new HomeController(stubMessagesControllerComponents())
      val response = controller.index()(FakeRequest(GET, "/"))
      contentAsString(response) must include("Piano")
    }
  }
}
