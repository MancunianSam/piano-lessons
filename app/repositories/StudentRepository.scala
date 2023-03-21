package repositories

import com.google.inject.Inject
import controllers.BookingController.Contact
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.Tables._
import slick.jdbc.JdbcProfile

import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class StudentRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val insertQuery = Student returning Student.map(_.id) into
    ((student, studentId) => student.copy(id = studentId))

  def addStudent(contact: Contact, totalCost: Long, paymentIntendId: Option[String] = None): Future[StudentRow] = {
    val insert = insertQuery += StudentRow(UUID.randomUUID(), contact.email, contact.name, contact.student, contact.level, contact.phone, contact.notes, paymentIntendId, Option(totalCost), Option(false))
    db.run(insert)
  }

  def updatePaymentIntentId(studentId: UUID, paymentIntentId: String): Future[Int] = {
    val update = Student.filter(_.id === studentId).map(s => (s.paymentIntentId)).update(Option(paymentIntentId))
    db.run(update)
  }

  def getStudent(id: UUID): Future[Seq[StudentRow]] = {
    val query = Student.filter(_.id === id)
    db.run(query.result)
  }

  def updateChargeCompleted(paymentIntentId: String): Future[Int] = {
    val update = Student.filter(_.paymentIntentId === Option(paymentIntentId))
      .map(_.paymentConfirmed)
      .update(Option(true))
    db.run(update)
  }
}
