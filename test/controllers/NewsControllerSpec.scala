package controllers

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.pac4j.play.scala.SecurityComponents
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers.GET
import repositories.NewsRepository
import services.NewsService
import utils.{PianoLessonsUtils, TestContainersUtils}
import play.api.test.Helpers._
import play.api.test._

import java.time.{LocalDate, Month}
import scala.concurrent.ExecutionContext.Implicits.global

class NewsControllerSpec extends PianoLessonsUtils with TestContainersUtils {

  "the add news page" should {
    "return a redirect for an unauthorised user" in withContainers { container: PostgreSQLContainer =>
      val result = createController(container.mappedPort(5432), unauthorisedSecurityComponents)
        .addNews()(FakeRequest(GET, "/news/add").withCSRFToken)
      status(result) must equal(302)
    }

    "return the add news page for an authenticated user" in withContainers { container: PostgreSQLContainer =>
      val result = createController(container.mappedPort(5432))
        .addNews()(FakeRequest(GET, "/news/add").withCSRFToken)
      status(result) must equal(200)
      val pageContent = contentAsString(result)
      pageContent must include("""<input type="text" id="title" name="title" class="">""")
      pageContent must include("""<textarea type="text" id="body" name="body" class="body "></textarea>""")
    }
  }

  "the get news page" should {
    "return an empty page if there are no news items" in withContainers { container: PostgreSQLContainer =>
      val controller = createController(container.mappedPort(5432))
      val result = controller.index()(FakeRequest(GET, "/news"))
      contentAsString(result) mustNot include("""<li class="cards">""")
    }
  }

  "the get news page" should {
    "return news items where available" in withContainers { container: PostgreSQLContainer =>
      val controller = createController(container.mappedPort(5432))
      createNews(getConnection(container.jdbcUrl), "A title", "Some news text", LocalDate.of(2024, Month.JANUARY, 1))
      val result = controller.index()(FakeRequest(GET, "/news"))
      val content = contentAsString(result)
      content must include("""<h2>1st Jan 2024</h2>""")
      content must include("""<h2>A title</h2>""")
      content must include("""<p class="body-text">Some news text</p>""")
    }
  }

  "the add news endpoint" should {
    "add the news to the database" in withContainers { container: PostgreSQLContainer =>
      val form = Seq(("title", "A new title"), ("body", "A new body text"))
      val result = createController(container.mappedPort(5432))
        .add()(FakeRequest(POST, "/news/add").withFormUrlEncodedBody(form: _*))
      status(result) must equal(303)
      val news = getNews(getConnection(container.jdbcUrl))
      news.title must include("A new title")
      news.body must include("A new body text")
    }
  }

  private def createController(port: Int, securityComponents: SecurityComponents = authorisedSecurityComponents) = {
    val configProvider = createDbConfigProvider(port)
    val newsRepository = new NewsRepository(configProvider)
    val newsService = new NewsService(newsRepository)
    new NewsController(newsService, securityComponents)
  }

}
