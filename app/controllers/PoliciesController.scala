package controllers

import play.api.mvc.{Action, AnyContent, MessagesBaseController, MessagesControllerComponents, MessagesRequest}

import javax.inject.Inject

class PoliciesController @Inject() (val controllerComponents: MessagesControllerComponents) extends MessagesBaseController {

  def index(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.policies())
  }
}
