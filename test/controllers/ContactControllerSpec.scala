package controllers

import play.api.Configuration
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, stubControllerComponents}
import utils.PianoLessonsUtils

class ContactControllerSpec extends PianoLessonsUtils {
  "ContactController contact" should {
    "render the contact page" in {
      val configuration = Configuration.from(Map("contact.phone" -> "12345678"))
      val controller = new ContactController(stubControllerComponents(), configuration)
      val response = controller.contact()(FakeRequest(GET, "/contact"))
      contentAsString(response) must include("12345678")
    }
  }
}
