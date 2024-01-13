package modules

import com.google.inject.AbstractModule
import configuration.{GoogleConfiguration, StripeConfiguration}
import play.api.{Configuration, Environment, Mode}
import services.{GoogleService, StripeService}

import scala.annotation.unused

class ThirdPartyModule(environment: Environment, @unused configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    if (environment.mode == Mode.Prod) {
      bind(classOf[GoogleConfiguration]).to(classOf[GoogleService])
      bind(classOf[StripeConfiguration]).to(classOf[StripeService])
    } else {
      bind(classOf[GoogleConfiguration]).to(classOf[LocalGoogleService])
      bind(classOf[StripeConfiguration]).to(classOf[LocalStripeService])
    }

  }
}
