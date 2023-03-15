package controllers

import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration
import play.api.Configuration
import play.api.Play.materializer
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsJson, defaultAwaitTimeout}
import utils.PianoLessonsUtils

class PaymentControllerSpec extends PianoLessonsUtils {
  val stripeConfiguration: StripeConfiguration = (n, a) => {
    val paymentIntent = new PaymentIntent()
    paymentIntent.setClientSecret("test-secret")
    paymentIntent
  }

  "PaymentController paymentIntent" should {
    "return the payment intent" in {
      val configuration = Configuration.from(Map())
      val controller = new PaymentController(authorisedSecurityComponents, stripeConfiguration, configuration)
      val response = controller.paymentIntent().apply(FakeRequest(POST, "/payment-intent").withCSRFToken)
      val a = contentAsJson(response)
      (a \ "clientSecret").as[String] must be("test-secret")
    }
  }

  "PaymentController pay" should {
    "return the payment intent" in {
      val configuration = Configuration.from(Map())
      val controller = new PaymentController(authorisedSecurityComponents, stripeConfiguration, configuration)
      val response = controller.paymentIntent().apply(FakeRequest(POST, "/payment-intent").withCSRFToken)
      val a = contentAsJson(response)
      (a \ "clientSecret").as[String] must be("test-secret")
    }
  }
}
