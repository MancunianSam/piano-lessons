//package controllers
//
//import com.stripe.model.PaymentIntent
//import configuration.StripeConfiguration
//import play.api.Configuration
//import play.api.Play.materializer
//import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{POST, contentAsJson, defaultAwaitTimeout}
//import repositories.StudentRepository
//import utils.PianoLessonsUtils
//
//class PaymentControllerSpec extends PianoLessonsUtils {
//  val stripeConfiguration: StripeConfiguration = new StripeConfiguration {
//    private def intent() = {
//      val paymentIntent = new PaymentIntent()
//      paymentIntent.setClientSecret("test-secret")
//      paymentIntent
//    }
//    override def paymentIntent(amount: Long): PaymentIntent = {
//      intent()
//    }
//
//    override def getPaymentIntent(paymentIntentId: String): PaymentIntent = intent()
//  }
//
//  "PaymentController paymentIntent" should {
//    "return the payment intent" in {
//      val studentRepository = new StudentRepository()
//      val configuration = Configuration.from(Map())
//      val controller = new PaymentController(authorisedSecurityComponents, stripeConfiguration, configuration)
//      val response = controller.paymentIntent().apply(FakeRequest(POST, "/payment-intent").withCSRFToken)
//      val a = contentAsJson(response)
//      (a \ "clientSecret").as[String] must be("test-secret")
//    }
//  }
//
//  "PaymentController pay" should {
//    "return the payment intent" in {
//      val configuration = Configuration.from(Map())
//      val controller = new PaymentController(authorisedSecurityComponents, stripeConfiguration, configuration)
//      val response = controller.paymentIntent().apply(FakeRequest(POST, "/payment-intent").withCSRFToken)
//      val a = contentAsJson(response)
//      (a \ "clientSecret").as[String] must be("test-secret")
//    }
//  }
//}
