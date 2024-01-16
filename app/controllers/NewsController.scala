package controllers

import auth.OidcSecurity
import controllers.NewsController.NewsForm
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.SecurityComponents
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import services.NewsService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.OptionConverters.RichOptional

class NewsController @Inject()(val newsService: NewsService, val controllerComponents: SecurityComponents)(implicit ec: ExecutionContext) extends OidcSecurity {

  private def newsForm: Form[NewsForm] = Form[NewsForm](
    mapping(
      "title" -> text.verifying("Please enter a title", a => a.nonEmpty),
      "body" -> text.verifying("Please enter news", a => a.nonEmpty),
    )(NewsForm.apply)(NewsForm.unapply)
  )

  private def pac4jToken(implicit request: Request[AnyContent]): String = {
    val webContext = new PlayWebContext(request)
    sessionStore.get(webContext, Pac4jConstants.CSRF_TOKEN).toScala.map(_.toString).orNull
  }

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    newsService.getNews.map { news =>
      Ok(views.html.news(news))
    }
  }

  def addNews(): Action[AnyContent] = Secure("Google2Client", "custom") { implicit request: Request[AnyContent] =>
    Ok(views.html.newsAdd(newsForm, pac4jToken))
  }

  def add(): Action[AnyContent] = Secure("Google2Client", "custom").async { implicit request: Request[AnyContent] =>
    newsForm.bindFromRequest().fold(
      errForm => Future.successful(BadRequest(views.html.newsAdd(errForm, pac4jToken))),
      newsForm => {
        newsService.addNews(newsForm.title, newsForm.body).map { _ =>
          Redirect(routes.NewsController.index())
        }
      }
    )
  }
}

object NewsController {
  case class NewsForm(title: String, body: String)
}
