package modules

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event}
import configuration.GoogleConfiguration

class LocalGoogleService extends GoogleConfiguration {
  override def calendarListItems: List[CalendarListEntry] = {
    val listEntry = new CalendarListEntry()
    listEntry.setId("clairelpalmer")
    List(listEntry)
  }

  override def listEvents(calendarId: String, startTime: DateTime, endTime: DateTime): List[Event] = Nil

  override def addEvent(calendarId: String, event: Event): Event = new Event()
}
