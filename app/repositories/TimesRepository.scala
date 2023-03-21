package repositories

import com.google.inject.Inject
import controllers.BookingController.Booking
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.Tables.TimesRow
import slick.jdbc.JdbcProfile
import repositories.Tables._
import repositories.TimesRepository.formattedPattern

import java.sql.{Date, Timestamp}
import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TimesRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val insertQuery = Times returning Times.map(_.id) into
    ((times, timesId) => times.copy(id = timesId))

  def addTimes(booking: Booking): Future[Seq[TimesRow]] = {
    val rows = booking.dates.map(d => {
      val localDateTime = LocalDateTime.parse(d, formattedPattern)
      val startTime = Timestamp.valueOf(localDateTime)
      val endTime = Timestamp.valueOf(localDateTime.plusMinutes(booking.lengthOfLesson))
      TimesRow(UUID.randomUUID(), booking.numberOfLessons, booking.lengthOfLesson, startTime, endTime, Option(booking.studentId))
    })
    db.run(insertQuery ++= rows)
  }

  def getTimes(paymentIntentId: String): Future[Seq[(TimesRow, StudentRow)]] = {
    val query = Times.join(Student).on(_.studentId === _.id)
      .filter(_._2.paymentIntentId === Option(paymentIntentId))
    db.run(query.result)
  }
}

object TimesRepository {
  val formattedPattern: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d LLLL yyyy HH mm")
}
