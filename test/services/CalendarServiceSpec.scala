//package services
//
//import com.google.api.client.util.DateTime
//import com.google.api.services.calendar.model.{CalendarListEntry, Event, EventDateTime}
//import configuration.GoogleConfiguration
//import controllers.BookingController.Contact
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.prop.TableDrivenPropertyChecks
//import org.scalatestplus.play.PlaySpec
//import services.CalendarService._
//import java.text.SimpleDateFormat
//import scala.concurrent.Future
//
//class CalendarServiceSpec extends PlaySpec with TableDrivenPropertyChecks with ScalaFutures {
//  val date = "2023-03-02"
//  def testGoogleConfiguration(eventStartTimes: List[Int] = Nil): GoogleConfiguration = new GoogleConfiguration {
//
//    private def eventDateTime(hour: Int) = {
//      val eventDateTime = new EventDateTime
//      val eventDate = new SimpleDateFormat("yyyy-MM-dd HH").parse(s"$date $hour")
//      val dateTime = new DateTime(eventDate)
//      eventDateTime.setDateTime(dateTime)
//      eventDateTime
//    }
//    override def calendarListItems: List[CalendarListEntry] = {
//      val calendarListEntry = new CalendarListEntry
//      calendarListEntry.setId("id")
//      calendarListEntry :: Nil
//    }
//
//    override def listEvents(calendarId: String, startTime: DateTime, endTime: DateTime): List[Event] = {
//      eventStartTimes.map(s => {
//        val event = new Event()
//        event.setStart(eventDateTime(s))
//        event.setEnd(eventDateTime(s + 1))
//        event
//      })
//    }
//
//    override def addEvent(calendarId: String, event: Event): Future[Event] = {
//      Future.successful(event)
//    }
//  }
//
//  "CalendarService getAvailableSlots" should {
//    val bookedSlots = Table(
//      ("bookedSlots", "availableSlots"),
//      (List(9, 11, 15), List(10, 12, 13, 14, 16, 17)),
//      (List(), List(9, 10, 11, 12, 13, 14, 15, 16, 17)),
//      (List(8, 20), List(9, 10, 11, 12, 13, 14, 15, 16, 17)),
//      (List(9, 10, 11, 12, 13, 14, 15, 16, 17), List())
//    )
//    forAll(bookedSlots) { (booked, available) =>
//      s"return the available slots for booked slots ${booked.mkString(",")}" in {
//        val service = new CalendarService(testGoogleConfiguration(booked))
//        val availableResponse = service.getAvailableSlots("2023-03-02", 2)
//        availableResponse must be(available.map(i => s"$i:00"))
//      }
//    }
//  }
//
//  "CalendarService putEvent" should {
//    "create an event in the calendar" in {
//      val service = new CalendarService(testGoogleConfiguration())
//      val contact = Contact("test@test.com", "name", "phone", Option("notes"))
//      val event = service.putEvent(date, "09:00", contact).futureValue
//      event.getStart.getTime(false).get must be (9)
//      event.getEnd.getTime(false).get must be (10)
//      event.getDescription must be ("\nEmail: test@test.com\nName: name\nPhone: phone\nNotes: notes\n")
//    }
//  }
//}
