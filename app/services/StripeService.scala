package services

import com.google.inject.Inject
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams
import configuration.StripeConfiguration
import play.api.Configuration

class StripeService @Inject()(configuration: Configuration) extends StripeConfiguration {
  Stripe.apiKey = configuration.get[String]("stripe.secret")

  override def paymentIntent(numberOfLessons: Int, lengthOfLesson: Int): PaymentIntent = {
    val price = lengthOfLesson match {
      case 30 => 15
      case 45 => 25
      case 60 => 30
    }
    val total = numberOfLessons match {
      case 3 => (numberOfLessons * price) - 10
      case 6 => (numberOfLessons * price) - 20
      case _ => numberOfLessons * price
    }
    val automaticPaymentMethods = PaymentIntentCreateParams.AutomaticPaymentMethods.builder.setEnabled(true).build()
    val params = PaymentIntentCreateParams.builder()
      .setAmount(total)
      .setCurrency("gbp")
      .setAutomaticPaymentMethods(automaticPaymentMethods).build()
    PaymentIntent.create(params)}
}
