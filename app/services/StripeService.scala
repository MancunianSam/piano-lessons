package services

import com.google.inject.Inject
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.param.{PaymentIntentCreateParams, PaymentIntentRetrieveParams}
import configuration.StripeConfiguration
import play.api.Configuration

import java.util.UUID

class StripeService @Inject()(configuration: Configuration) extends StripeConfiguration {
  Stripe.apiKey = configuration.get[String]("stripe.secret")

  override def paymentIntent(amount: Long, studentId: UUID): PaymentIntent = {
    val automaticPaymentMethods = PaymentIntentCreateParams.AutomaticPaymentMethods.builder.setEnabled(true).build()
    val params = PaymentIntentCreateParams.builder()
      .setAmount(amount)
      .setCurrency("gbp")
      .setAutomaticPaymentMethods(automaticPaymentMethods)
      .putMetadata("studentId", studentId.toString)
      .build()
    PaymentIntent.create(params)}

  override def getPaymentIntent(paymentIntentId: String): PaymentIntent = {
    PaymentIntent.retrieve(paymentIntentId)
  }
}
