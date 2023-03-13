package services

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{Event, EventDateTime}
import com.google.inject.Inject
import configuration.GoogleConfiguration
import controllers.BookingController.Contact
import services.CalendarService.EventUtils

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.Date
import scala.concurrent.Future

class CalendarService @Inject()(googleConfiguration: GoogleConfiguration) {

  def getAvailableSlots(date: String, numberOfLessons: Int): List[String] = {
    val allHours = 9 to 17
    val calendarId = googleConfiguration.calendarListItems.head.getId

    val hours = (0 until numberOfLessons).toSet.flatMap((plusWeeks: Int) => {
      val now = LocalDate.parse(date).plusWeeks(plusWeeks)


      val start = new DateTime(Date.from(now.atStartOfDay().toInstant(ZoneOffset.UTC)))
      val end = new DateTime(Date.from(now.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
      googleConfiguration.listEvents(calendarId, start, end)
        .flatMap(ev => {
          (for {
            start <- ev.getStart.getHour(false)
            end <- ev.getEnd.getHour(true)
          } yield Set(start, end)).getOrElse(Nil)
        }).toSet
    })
    allHours.toSet.diff(hours).toList.sorted.map(h => s"$h:00")
  }

  def putEvent(date: String, slot: String, contactForm: Contact): Future[Event] = {
    val hour = slot.split(":").head.toInt
    val calendarId = googleConfiguration.calendarListItems.head.getId
    val event = new Event()
    event.setStart(new EventDateTime().setTime(date, hour))
    event.setEnd(new EventDateTime().setTime(date, hour + 1))
    event.setSummary(s"Booking for ${contactForm.email}")
    event.setDescription(contactForm.toString)
    googleConfiguration.addEvent(calendarId, event)
  }
}

object CalendarService {

  implicit class EventUtils(eventDateTime: EventDateTime) {
    def getHour(minusOneSecond: Boolean): Option[Int] = {
      Option(eventDateTime.getDateTime).map(dateTime => {
        val localTime = LocalTime.parse(dateTime.toStringRfc3339, DateTimeFormatter.ISO_DATE_TIME)
        val adjustedTime = if (minusOneSecond) {
          localTime.minusSeconds(1)
        } else localTime
        adjustedTime.getHour
      }
      )
    }

    def setTime(date: String, hour: Int): EventDateTime = {
      val eventDate = new SimpleDateFormat("yyyy-MM-dd HH").parse(s"$date $hour")
      val dateTime = new DateTime(eventDate)
      eventDateTime.setDateTime(dateTime)
      eventDateTime
    }
  }
}
