package modules

import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration

import java.util.UUID

class LocalStripeService extends StripeConfiguration {

  private def intent = {
    val intent = new PaymentIntent()
    intent.setId("id")
    intent
  }

  override def paymentIntent(amount: Long, studentId: UUID): PaymentIntent = intent

  override def getPaymentIntent(paymentIntentId: String): PaymentIntent = intent

}
