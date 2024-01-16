package repositories

import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.Tables._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class NewsRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val insertQuery = News returning News.map(_.id) into
    ((news, newsId) => news.copy(id = newsId))

  def getNews: Future[Seq[NewsRow]] = {
    db.run(News.result)
  }

  def addNews(newsRow: NewsRow): Future[NewsRow] = {
    db.run(insertQuery += newsRow)
  }
}
