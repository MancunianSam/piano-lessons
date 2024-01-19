package controllers

import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.PianoLessonsUtils

class PoliciesControllerSpec extends PianoLessonsUtils {

  "the policies page" should {
    "render the policies" in {
      val response = new PoliciesController(stubMessagesControllerComponents())
        .index()
        .apply(FakeRequest(GET, "/choose-length"))
      contentAsString(response) must include("Business Policies")
    }
  }

}
