package configuration

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event}

import scala.concurrent.Future

trait GoogleConfiguration {
  def calendarListItems: List[CalendarListEntry]

  def listEvents(calendarId: String, startTime: DateTime, endTime: DateTime): List[Event]

  def addEvent(calendarId: String, event: Event): Event
}
