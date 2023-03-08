package modules

import com.google.inject.AbstractModule
import configuration.{GoogleConfiguration, StripeConfiguration}
import services.{GoogleService, StripeService}

class ThirdPartyModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[GoogleConfiguration]).to(classOf[GoogleService])
    bind(classOf[StripeConfiguration]).to(classOf[StripeService])
  }
}
