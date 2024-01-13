package services

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event, EventDateTime}
import configuration.GoogleConfiguration
import controllers.BookingController.Contact
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import services.CalendarService._

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, LocalTime}

class CalendarServiceSpec extends PlaySpec with TableDrivenPropertyChecks with ScalaFutures {
  val date = "2023-03-29"
  val startDate: Timestamp = Timestamp.valueOf(LocalDateTime.parse(s"${date}T09:00:00"))
  val endDate: Timestamp = Timestamp.valueOf(LocalDateTime.parse(s"${date}T10:00:00"))

  def testGoogleConfiguration(eventStartTimes: List[String] = Nil, increment: Int): GoogleConfiguration = new GoogleConfiguration {

    private def eventDateTime(hour: String) = {
      val eventDateTime = new EventDateTime
      val eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s"$date $hour")
      val dateTime = new DateTime(eventDate)
      eventDateTime.setDateTime(dateTime)
      eventDateTime
    }
    override def calendarListItems: List[CalendarListEntry] = {
      val calendarListEntry = new CalendarListEntry
      calendarListEntry.setId("clairelpalmer")
      calendarListEntry :: Nil
    }

    override def listEvents(calendarId: String, startTime: DateTime, endTime: DateTime): List[Event] = {
      eventStartTimes.map(s => {
        val event = new Event()
        val endDate = LocalTime.parse(s).plusMinutes(increment).format(DateTimeFormatter.ofPattern("HH:mm"))
        event.setStart(eventDateTime(s))
        event.setEnd(eventDateTime(endDate))
        event
      })
    }

    override def addEvent(calendarId: String, event: Event): Event = {
      event
    }
  }

  "CalendarService getAvailableSlots" should {
    val bookedSlotsHourLesson = Table(
      ("bookedSlots", "availableSlots"),
      (List(9, 11, 15), List(10, 12, 13, 14, 16, 17)),
      (List(), List(9, 10, 11, 12, 13, 14, 15, 16, 17)),
      (List(8, 20), List(9, 10, 11, 12, 13, 14, 15, 16, 17)),
      (List(9, 10, 11, 12, 13, 14, 15, 16, 17), List())
    )
    forAll(bookedSlotsHourLesson) { (booked, available) =>
      s"return the available slots for booked slots ${booked.mkString(",")} for an hour lesson" in {
        val service = new CalendarService(testGoogleConfiguration(booked.map(t => s"${f"$t%02d"}:00"), 60))
        val availableResponse = service.getAvailableSlots("2023-03-02", 1, 60, 18)
        availableResponse must be(available.map(i => s"${f"$i%02d"}:00"))
      }
    }

    val bookedSlotsHalfHourLesson = Table(
      ("bookedSlots", "availableSlots"),
      (List("09:30", "10:00", "11:00"), List("09:00", "10:30", "11:30")),
      (List(), List("09:00", "09:30", "10:00", "10:30", "11:00", "11:30")),
      (List("09:00", "09:30", "10:00", "10:30", "11:00", "11:30"), List())
    )

    forAll(bookedSlotsHalfHourLesson) { (booked, available) =>
      s"return the available slots for booked slots ${booked.mkString(",")} for a  half hour lesson" in {
        val service = new CalendarService(testGoogleConfiguration(booked, 30))
        val availableResponse = service.getAvailableSlots("2023-03-02", 1, 30, 12)
        availableResponse must be(available)
      }
    }
  }

  "CalendarService putEvent" should {
    "create an event in the calendar" in {
      val service = new CalendarService(testGoogleConfiguration(Nil, 30))
      val contact = Contact("test@test.com", "name", None, None, "phone", Option("notes"))
      val event = service.putEvent(startDate, endDate, contact)
      event.getStart.getTime().get.getHour must be(9)
      event.getEnd.getTime().get.getHour must be(10)
      event.getDescription must be("\nEmail: test@test.com\nName: name\nStudent Name: \nLevel: \nPhone: phone\nNotes: notes\n")
    }
  }
}
