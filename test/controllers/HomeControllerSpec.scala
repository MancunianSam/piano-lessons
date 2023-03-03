package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, stubControllerComponents}
import utils.PianoLessonsUtils

class HomeControllerSpec extends PianoLessonsUtils {
  "HomeController contact" should {
    "render the home page" in {
      val controller = new ContactController(stubControllerComponents())
      val response = controller.contact()(FakeRequest(GET, "/contact"))
      contentAsString(response) must include("piano")
    }
  }
}
