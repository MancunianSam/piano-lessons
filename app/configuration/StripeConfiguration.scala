package configuration

import com.stripe.model.PaymentIntent

import java.util.UUID

trait StripeConfiguration {
  def paymentIntent(amount: Long, studentId: UUID): PaymentIntent
  def getPaymentIntent(paymentIntentId: String): PaymentIntent
}
