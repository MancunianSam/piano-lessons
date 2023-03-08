package configuration

import com.stripe.model.PaymentIntent

trait StripeConfiguration {
  def paymentIntent(): PaymentIntent
}
