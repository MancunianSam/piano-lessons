package services

import repositories.Tables.NewsRow
import repositories.{NewsRepository, Tables}
import services.NewsService.News
import java.sql.Date
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.{Locale, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NewsService @Inject() (val newsRepository: NewsRepository)(implicit executionContext: ExecutionContext) {

  private def getSuffix(day: Int): String = {
    if (day >= 11 && day <= 14) {
      "th"
    } else {
      day % 10 match {
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
      }
    }
  }

  def getNews: Future[Seq[News]] = {
    newsRepository.getNews.map(_.sortBy(_.date).reverse.map { newsRow =>
      val date = formatDate(newsRow)
      News(date, newsRow.title, newsRow.body.replaceAll("\r", "<br>"))
    })
  }

  def addNews(title: String, body: String): Future[NewsRow] = {
    val newsRow = NewsRow(UUID.randomUUID(), Date.valueOf(LocalDate.now()), title, body)
    newsRepository.addNews(newsRow)
  }

  private def formatDate(newsRow: Tables.NewsRow): String = {
    val localDate = newsRow.date.toLocalDate
    val dayOfMonth = localDate.getDayOfMonth
    val day = s"$dayOfMonth${getSuffix(dayOfMonth)}"
    val month = localDate.getMonth.getDisplayName(TextStyle.SHORT, Locale.UK)
    val year = localDate.getYear
    s"$day $month $year"
  }
}
object NewsService {
  case class News(date: String, title: String, body: String)
}
