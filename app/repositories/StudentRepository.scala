package repositories

import com.google.inject.Inject
import controllers.BookingController.Contact
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repositories.Tables._
import slick.jdbc.JdbcProfile

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class StudentRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  def addStudent(contact: Contact, paymentIntendId: String): Future[Int] = {
    val insert = Student += StudentRow(UUID.randomUUID(), contact.email, contact.name, contact.phone, Option(paymentIntendId))
    db.run(insert)
  }

  def getStudent(email: String): Future[Seq[StudentRow]] = {
    val query = Student.filter(_.email === email)
    db.run(query.result)
  }
}
