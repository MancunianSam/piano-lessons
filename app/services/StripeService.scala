package services

import com.google.inject.Inject
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams
import configuration.StripeConfiguration
import play.api.Configuration

class StripeService @Inject()(configuration: Configuration) extends StripeConfiguration {
  Stripe.apiKey = configuration.get[String]("stripe.secret")

  override def paymentIntent(): PaymentIntent = {
    val automaticPaymentMethods = PaymentIntentCreateParams.AutomaticPaymentMethods.builder.setEnabled(true).build()
    val params = PaymentIntentCreateParams.builder()
      .setAmount(1000)
      .setCurrency("gbp")
      .setAutomaticPaymentMethods(automaticPaymentMethods).build()
    PaymentIntent.create(params)}
}
