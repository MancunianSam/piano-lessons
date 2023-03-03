package controllers

import play.api.mvc._
import javax.inject._

@Singleton
class HomeController @Inject()(val controllerComponents: MessagesControllerComponents) extends MessagesBaseController {

  def index() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.index())
  }
}
