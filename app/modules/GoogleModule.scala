package modules

import com.google.inject.AbstractModule
import configuration.{GoogleConfiguration, LiveGoogleConfiguration}

class GoogleModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[GoogleConfiguration]).to(classOf[LiveGoogleConfiguration])
  }
}
