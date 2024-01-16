package services

import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import repositories.{NewsRepository, Tables}
import repositories.Tables.NewsRow

import java.sql.Date
import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NewsServiceSpec extends PlaySpec with TableDrivenPropertyChecks with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  def date(day: Int, month: Int): Date = Date.valueOf(LocalDate.of(2024, month, day))
  val newsRepository: NewsRepository = mock[NewsRepository]
  val dateTable: TableFor2[Date, String] = Table(
    ("date", "expectedFormat"),
    (date(1, 1), "1st Jan 2024"),
    (date(2, 1), "2nd Jan 2024"),
    (date(3, 1), "3rd Jan 2024"),
    (date(13, 1), "13th Jan 2024"),
    (date(21, 1), "21st Jan 2024"),
    (date(1, 3), "1st Mar 2024")
  )

  override def beforeEach(): Unit = {
    Mockito.reset(newsRepository)
  }

  private def newsRow(body: String): NewsRow = newsRow(Date.valueOf(LocalDate.now())).copy(body = body)
  private def newsRow(date: Date): NewsRow = NewsRow(UUID.randomUUID(), date, "", "")

  "get news" should {
    forAll(dateTable) { (date, expectedFormat) =>
      s"return $expectedFormat for the provided date" in {
        when(newsRepository.getNews).thenReturn(Future(Seq(newsRow(date))))

        val news = new NewsService(newsRepository).getNews.futureValue
        news.head.date must equal(expectedFormat)
      }
    }

    "sort the news into date order" in {
      val dates = List(date(1, 1), date(5, 2), date(6, 2), date(31, 12))
      when(newsRepository.getNews).thenReturn(Future(dates.map(newsRow)))

      val news = new NewsService(newsRepository).getNews.futureValue
      news.map(_.date) must equal(List("31st Dec 2024", "6th Feb 2024", "5th Feb 2024", "1st Jan 2024"))
    }

    "replace carriage return with <br>" in {
      when(newsRepository.getNews).thenReturn(Future(Seq(newsRow("Body \r with \r return"))))
      val news = new NewsService(newsRepository).getNews.futureValue

      news.head.body must equal("Body <br> with <br> return")
    }
  }

  "add news" should {
    "add the news item to the database" in {
      val rowCaptor: ArgumentCaptor[NewsRow] = ArgumentCaptor.forClass(classOf[NewsRow])
      when(newsRepository.addNews(rowCaptor.capture())).thenReturn(Future(newsRow("")))

      new NewsService(newsRepository).addNews("title", "body").futureValue

      val insertedValue = rowCaptor.getValue
      insertedValue.date.toLocalDate must equal(LocalDate.now())
      insertedValue.title must equal("title")
      insertedValue.body must equal("body")
    }
  }

}
