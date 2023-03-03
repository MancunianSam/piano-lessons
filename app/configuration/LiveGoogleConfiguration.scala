package configuration

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event}
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class LiveGoogleConfiguration extends GoogleConfiguration {
  private val transport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
  private val jsonFactory = GsonFactory.getDefaultInstance
  val credentials: GoogleCredentials = GoogleCredentials.getApplicationDefault()
    .createScoped(CalendarScopes.CALENDAR_EVENTS, CalendarScopes.CALENDAR)

  def calendarListItems: List[CalendarListEntry] = {
    calendarApi.calendarList().list().execute().getItems.asScala.toList
  }

  def listEvents(calendarId: String, startTime: DateTime, endTime: DateTime): List[Event] = {
    calendarApi.events().list(calendarId)
      .setTimeMin(startTime)
      .setTimeMax(endTime)
      .execute()
      .getItems.asScala.toList
  }

  def addEvent(calendarId: String, event: Event): Future[Event] =
    Future.successful(calendarApi.events().insert(calendarId, event).execute())

  private def calendarApi: Calendar = {
    credentials.refresh()
    val adapter = new HttpCredentialsAdapter(credentials)
    new Calendar.Builder(transport, jsonFactory, adapter).build()
  }

}
