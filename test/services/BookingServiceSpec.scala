package services

import com.google.api.services.calendar.model.Event
import com.stripe.model.PaymentIntent
import configuration.StripeConfiguration
import controllers.BookingController.Contact
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2, TableFor3}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import repositories.Tables.{StudentRow, TimesRow}
import repositories.{StudentRepository, Tables, TimesRepository}
import services.BookingService.Booking
import services.EmailService.{Order, OrderDates}

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BookingServiceSpec extends PlaySpec with TableDrivenPropertyChecks with ScalaFutures with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = ExecutionContext.global
  val date = "2023-03-30"
  val time = "09:00"

  val studentRow: Tables.StudentRow = StudentRow(UUID.randomUUID(), "test@test.com", "name", None, None, "phone", totalCost = Option(BigDecimal.valueOf(1000)))
  val timesRow: Tables.TimesRow = TimesRow(UUID.randomUUID(), 1, 30, Timestamp.valueOf(LocalDateTime.parse(s"${date}T$time:00")), Timestamp.valueOf(LocalDateTime.now()))
  val contact: Contact = Contact("test@test.com", "name", None, None, "phone", None)
  val order: Order = Order("name", None, "test@test.com", "phone", "Thank you for your order!", "30 minutes", 1, 10, List(OrderDates(1, "Thursday 30 March 2023 09 00")))

  val studentRepository: StudentRepository = mock[StudentRepository]
  val timesRepository: TimesRepository = mock[TimesRepository]
  val amountService: AmountService = mock[AmountService]
  val calendarService: CalendarService = mock[CalendarService]
  val emailService: EmailService = mock[EmailService]
  val stripeConfiguration: StripeConfiguration = mock[StripeConfiguration]

  override def beforeEach(): Unit = {
    Mockito.reset(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)
  }

  val datesTable: TableFor2[Int, List[String]] = Table(
    ("numberOfLessons", "expectedDates"),
    (1, List("Thursday 30 March 2023 09 00")),
    (3, List("Thursday 30 March 2023 09 00", "Thursday 6 April 2023 09 00", "Thursday 13 April 2023 09 00")),
    (6, List("Thursday 30 March 2023 09 00", "Thursday 6 April 2023 09 00", "Thursday 13 April 2023 09 00", "Thursday 20 April 2023 09 00", "Thursday 27 April 2023 09 00", "Thursday 4 May 2023 09 00")),
  )

  "BookingService createBooking" should {
    forAll(datesTable) { (numberOfLessons, expectedDates) =>
      s"return the correct dates for $numberOfLessons lessons" in {
        when(studentRepository.getStudent(any[UUID])).thenReturn(Future(Seq(studentRow)))
        val service = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)
        val booking = service.createBooking(numberOfLessons, 30, date, time, UUID.randomUUID(), 1000).futureValue
        booking.dates must equal(expectedDates)
      }
    }
  }

  "BookingService createFreeBooking" should {
    "create a free booking" in {
      val studentRepository: StudentRepository = mock[StudentRepository]
      val timesRepository: TimesRepository = mock[TimesRepository]
      val amountService: AmountService = mock[AmountService]
      val calendarService: CalendarService = mock[CalendarService]
      val emailService: EmailService = mock[EmailService]
      val stripeConfiguration: StripeConfiguration = mock[StripeConfiguration]

      when(studentRepository.getStudent(any[UUID])).thenReturn(Future(Seq(studentRow)))
      when(studentRepository.addStudent(any[Contact], any[Int], any[Option[String]], any[Boolean])).thenReturn(Future(studentRow))
      when(timesRepository.addTimes(any[Booking])).thenReturn(Future(Seq(timesRow)))
      when(emailService.sendOrderEmail(any[String], any[Option[Order]])).thenReturn(Future(1))
      when(calendarService.putEvent(any[Timestamp], any[Timestamp], any[Contact])).thenReturn(new Event())

      val service = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)
      service.createFreeBooking(1, date, time, contact).futureValue

      verify(studentRepository, times(1)).addStudent(any[Contact], any[Int], any[Option[String]], any[Boolean])
      verify(studentRepository, times(1)).getStudent(any[UUID])
      verify(timesRepository, times(1)).addTimes(any[Booking])
      verify(emailService, times(2)).sendOrderEmail(any[String], any[Option[Order]])
      verify(calendarService, times(1)).putEvent(any[Timestamp], any[Timestamp], any[Contact])
    }
  }

  "BookingService createPaidBooking" should {
    "create a paid booking" in {
      val paymentIntent = new PaymentIntent
      paymentIntent.setId("id")

      when(studentRepository.getStudent(any[UUID])).thenReturn(Future(Seq(studentRow)))
      when(studentRepository.addStudent(any[Contact], any[Int], any[Option[String]], any[Boolean])).thenReturn(Future(studentRow))
      when(studentRepository.updatePaymentIntentId(any[UUID], any[String])).thenReturn(Future(1))
      when(timesRepository.addTimes(any[Booking])).thenReturn(Future(Seq(timesRow)))
      when(stripeConfiguration.paymentIntent(any[Long], any[UUID])).thenReturn(paymentIntent)
      when(amountService.calculateAmount(any[Int], any[Int])).thenReturn(1)

      val service = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)

      service.createPaidBooking(1, 30, date, time, contact).futureValue

      verify(studentRepository, times(1)).addStudent(any[Contact], any[Int], any[Option[String]], any[Boolean])
      verify(studentRepository, times(1)).getStudent(any[UUID])
      verify(studentRepository, times(1)).updatePaymentIntentId(any[UUID], any[String])
      verify(timesRepository, times(1)).addTimes(any[Booking])
      verify(stripeConfiguration, times(1)).paymentIntent(any[Long], any[UUID])
      verify(amountService, times(1)).calculateAmount(any[Int], any[Int])
    }
  }

  val createOrderTable: TableFor3[List[Tables.TimesRow], Option[BigDecimal], Option[Order]] = Table(
    ("times", "totalCost", "expectedResponse"),
    (Nil, None, None),
    (List(timesRow), None, None),
    (List(timesRow), Option(BigDecimal.valueOf(1000)), Option(order))
  )

  "BookingService createOrder" should {
    forAll(createOrderTable) { (times, totalCost, expectedResponse) =>
      s"return the correct order for times length ${times.size} and total cost $totalCost" in {
        val service = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)
        val response = service.createOrder(studentRow.copy(totalCost = totalCost), times)
        response must equal(expectedResponse)
      }

    }
  }

  "BookingService createContact" should {
    "create a contact" in {
      val service = new BookingService(studentRepository, timesRepository, amountService, calendarService, emailService, stripeConfiguration)

      val contact = service.createContact(studentRow)

      contact.name must equal(studentRow.name)
      contact.email must equal(studentRow.email)
      contact.phone must equal(studentRow.phone)
      contact.level must equal(studentRow.level)
      contact.student must equal(studentRow.student)
      contact.notes must equal(studentRow.notes)
    }
  }

}
