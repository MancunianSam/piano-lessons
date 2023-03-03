package controllers

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}

import javax.inject.Inject

class SignOutController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {
  def signedOut(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.signout())
  }
}
