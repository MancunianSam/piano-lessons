package services

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event, EventDateTime}
import com.google.inject.Inject
import configuration.GoogleConfiguration
import controllers.BookingController.Contact
import services.CalendarService.{EventRange, EventUtils}

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.Date
import scala.annotation.tailrec
import scala.concurrent.Future

class CalendarService @Inject()(googleConfiguration: GoogleConfiguration) {

  private val startOfDay = 9

  private def calendarId: String = {
    googleConfiguration.calendarListItems.filter(_.getId.contains("clairelpalmer")).head.getId
  }

  @tailrec
  private def getAllTimesForDay(increment: Int, allTimes: List[EventRange], endOfDay: Int): List[EventRange] = {
    val currentTime = allTimes.head.end
    if (currentTime.getHour == endOfDay) {
      allTimes
    } else {
      getAllTimesForDay(increment, EventRange(currentTime, currentTime.plusMinutes(increment)) :: allTimes, endOfDay)
    }
  }

  def getAvailableSlots(date: String, numberOfLessons: Int, lengthOfLesson: Int, endOfDay: Int): List[String] = {
    val startTime = LocalTime.of(startOfDay, 0)
    val firstRange = EventRange(startTime, startTime.plusMinutes(lengthOfLesson))
    val allTimes = getAllTimesForDay(lengthOfLesson, firstRange :: Nil, endOfDay)
    val allLessonTimes = (0 until numberOfLessons).toList.map((plusWeeks: Int) => {
      val now = LocalDate.parse(date).plusWeeks(plusWeeks)
      now -> allTimes
    }).toMap
    val timesWithNoEvent = allLessonTimes.map { case (date, times) =>
      val start = new DateTime(Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)))
      val end = new DateTime(Date.from(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
      val allEvents = googleConfiguration.listEvents(calendarId, start, end)
        .flatMap(ev => {
          for {
            start <- ev.getStart.getTime()
            end <- ev.getEnd.getTime()
          } yield EventRange(start, end)
        })
      date -> times.filter(time => !allEvents.exists(ev => ev.overlaps(time)))
    }
    val filtered = allTimes.filter(eachTime => timesWithNoEvent.count(_._2.contains(eachTime)) == numberOfLessons).sortBy(_.start)
    filtered.map(_.start.format(DateTimeFormatter.ofPattern("HH:mm")))
  }

  def putEvent(startTime: Timestamp, endTime: Timestamp, contactForm: Contact): Event = {
    val event = new Event()
    event.setStart(new EventDateTime().setTime(startTime))
    event.setEnd(new EventDateTime().setTime(endTime))
    event.setSummary(s"Booking for ${contactForm.email}")
    event.setDescription(contactForm.toString)
    googleConfiguration.addEvent(calendarId, event)
  }
}

object CalendarService {
  case class EventRange(start: LocalTime, end: LocalTime) {
    def overlaps(range: EventRange): Boolean = {
      range.start.isBefore(end) && start.isBefore(range.end)
    }
  }

  implicit class EventUtils(eventDateTime: EventDateTime) {
    def getTime(): Option[LocalTime] = {
      Option(eventDateTime.getDateTime).map(dateTime => {
        LocalDateTime.parse(dateTime.toStringRfc3339, DateTimeFormatter.ISO_DATE_TIME).toLocalTime
      })
    }

    def setTime(date: Timestamp): EventDateTime = {
      val dateTime = new DateTime(date.getTime)
      eventDateTime.setDateTime(dateTime)
      eventDateTime
    }
  }
}
