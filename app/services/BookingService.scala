package services

import com.google.inject.Inject
import configuration.StripeConfiguration
import controllers.BookingController.Contact
import repositories.Tables._
import repositories.TimesRepository.formattedPattern
import repositories.{StudentRepository, Tables, TimesRepository}
import services.BookingService._
import services.EmailService._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BookingService @Inject() (
    val studentRepository: StudentRepository,
    val timesRepository: TimesRepository,
    val amountService: AmountService,
    val calendarService: CalendarService,
    val emailService: EmailService,
    val stripeConfiguration: StripeConfiguration
)(implicit executionContext: ExecutionContext) {

  def createBooking(numOfLessons: Int, lengthOfLesson: Int, date: String, time: String, studentId: UUID, cost: Int): Future[Booking] = {

    val dates = (0 until numOfLessons).toList.map(plusWeeks => {
      val localDateTime = LocalDateTime.parse(s"${date}T$time:00").plusWeeks(plusWeeks)
      localDateTime.format(formattedPattern)
    })
    studentRepository
      .getStudent(studentId)
      .map(student => {
        val email = student.head.email
        Booking(studentId, email, numOfLessons, lengthOfLesson, dates, cost)
      })
  }

  def createFreeBooking(numOfLessons: Int, date: String, time: String, contact: Contact): Future[Booking] = {
    for {
      student <- studentRepository.addStudent(contact, 0, chargeCompleted = true)
      booking <- createBooking(numOfLessons, 30, date, time, student.id, 0)
      times <- timesRepository.addTimes(booking)
      order = createOrder(student, times)
      _ <- emailService.sendOrderEmail(student.email, order)
      adminConfirmation = order.map(order => order.copy(title = "You have a new booking"))
      _ <- emailService.sendOrderEmail("bookings@clairepalmerpiano.co.uk", adminConfirmation)
    } yield {
      times.map(time => {
        val contact = createContact(student)
        calendarService.putEvent(time.startDate, time.endDate, contact)
      })
      booking
    }
  }

  def createContact(student: Tables.StudentRow): Contact = {
    Contact(student.email, student.name, student.student, student.level, student.phone, student.notes)
  }

  def createPaidBooking(numOfLessons: Int, lessonLength: Int, date: String, time: String, contact: Contact): Future[UUID] = {
    val amount = amountService.calculateAmount(numOfLessons, lessonLength)
    for {
      student <- studentRepository.addStudent(contact, amount, None)
      paymentIntent = stripeConfiguration.paymentIntent(amount, student.id)
      _ <- studentRepository.updatePaymentIntentId(student.id, paymentIntent.getId)
      booking <- createBooking(numOfLessons, lessonLength, date, time, student.id, amount)
      _ <- timesRepository.addTimes(booking)
    } yield {
      student.id
    }
  }

  def createOrder(student: StudentRow, times: Seq[TimesRow]): Option[Order] = {
    for {
      time <- times.headOption
      totalCost <- student.totalCost
    } yield {
      val dates = times
        .map(_.startDate.toLocalDateTime)
        .sorted
        .zipWithIndex
        .map { case (date, idx) =>
          OrderDates(idx + 1, date.format(formattedPattern))
        }
        .toList
      Order(
        student.name,
        student.student,
        student.email,
        student.phone,
        "Thank you for your order!",
        s"${time.lengthOfLessons} minutes",
        time.numberOfLessons,
        totalCost.toInt / 100,
        dates
      )
    }
  }
}

object BookingService {
  case class Booking(studentId: UUID, email: String, numberOfLessons: Int, lengthOfLesson: Int, dates: List[String], totalCost: Int)
}
