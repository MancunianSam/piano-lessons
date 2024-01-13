package controllers

import play.api.Configuration
import play.api.mvc._

import javax.inject.Inject

class ContactController @Inject() (val cc: ControllerComponents, configuration: Configuration) extends AbstractController(cc) {

  def contact(): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok(views.html.contact(configuration.get[String]("contact.phone")))
  }
}
