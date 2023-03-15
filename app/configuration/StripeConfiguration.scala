package configuration

import com.stripe.model.PaymentIntent

trait StripeConfiguration {
  def paymentIntent(numberOfLessons: Int, lengthOfLesson: Int): PaymentIntent
}
