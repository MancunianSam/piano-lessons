package controllers

import play.api.mvc._

import javax.inject.Inject

class ContactController @Inject()(val cc: ControllerComponents) extends AbstractController(cc) {

  def contact(): Action[AnyContent] = Action { implicit request: Request[Any] =>
    Ok("")
  }
}
