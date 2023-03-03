package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, stubControllerComponents}
import utils.PianoLessonsUtils

class ContactControllerSpec extends PianoLessonsUtils{
  "ContactController contact" should {
    "render the contact page" in {
      val controller = new ContactController(stubControllerComponents())
      val response = controller.contact()(FakeRequest(GET, "/contact"))
      contentAsString(response) must include("contact")
    }
  }
}
