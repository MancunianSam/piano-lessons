package controllers

import com.google.api.services.calendar.model.Event
import controllers.BookingController.Contact
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Play.materializer
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.ws.WSResponse
import services.{CalendarService, EmailService}
import utils.PianoLessonsUtils

import scala.concurrent.{ExecutionContext, Future}

class BookingControllerSpec extends PianoLessonsUtils {
  implicit val ec: ExecutionContext = ExecutionContext.global
  private val date = "2023-03-02"
  private val time = "09:00"

  "BookingController book" should {

    "redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val book = controller.book("").apply(FakeRequest(GET, "/book/").withCSRFToken)

      status(book) mustBe FOUND
      redirectLocation(book).get must startWith("/auth/realms/id")
    }

    "render the booking page without the slots list if no date is passed" in {
      val controller = authorisedController()
      val book = controller.book("").apply(FakeRequest(GET, "/book/").withCSRFToken)

      status(book) mustBe OK
      contentType(book) mustBe Some("text/html")
      val content = contentAsString(book)
      content must include("Choose a date")
      content must include("Check availability")
      content must not include "list-group-item"
    }

    "render the booking page with the slots list if a date is passed" in {
      val calendarService = mock[CalendarService]
      when(calendarService.getAvailableSlots(any[String])).thenReturn(List("9:00"))
      val controller = authorisedController(calendarService)
      val book = controller.book("2022-03-02").apply(FakeRequest(GET, "/book/2022-03-02").withCSRFToken)

      status(book) mustBe OK
      contentType(book) mustBe Some("text/html")
      val content = contentAsString(book)
      content must include("Choose a date")
      content must include("2022-03-02")
      content must include("9:00")
      content must include("Check availability")
      content must include("list-group-item")

      verify(calendarService, atLeastOnce()).getAvailableSlots(any[String])
    }
  }

  "BookingController bookSlot" should {
    "return redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val bookSlot = controller.bookSlot().apply(FakeRequest(POST, "/book-slot")
        .withFormUrlEncodedBody()
        .withCSRFToken)

      status(bookSlot) mustBe SEE_OTHER
      redirectLocation(bookSlot).get must startWith("/auth/realms/id")
    }

    "render an error if the date is missing" in {
      val controller = authorisedController()
      val bookSlot = controller.bookSlot().apply(FakeRequest(POST, "/book-slot")
        .withFormUrlEncodedBody()
        .withCSRFToken
      )
      val content = contentAsString(bookSlot)
      content must include("error.required")
    }

    "redirect to the booking contact page if the date and slot are present" in {
      val controller = authorisedController()
      val request = FakeRequest(POST, "/book-slot")
        .withFormUrlEncodedBody(("date", date), ("slot", time))

      val bookSlot = controller.bookSlot().apply(request)

      status(bookSlot) mustBe SEE_OTHER
      redirectLocation(bookSlot).get must startWith(s"/book-contact/$date/$time")
    }

    "redirect to the booking page if the date is present and the slot is missing" in {
      val controller = authorisedController()
      val request = FakeRequest(POST, "/book-slot")
        .withFormUrlEncodedBody(("date", date))

      val bookSlot = controller.bookSlot().apply(request)

      status(bookSlot) mustBe SEE_OTHER
      redirectLocation(bookSlot).get must startWith(s"/book/$date")
    }
  }

  "BookingController availability" should {
    "return redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val bookSlot = controller.availability().apply(FakeRequest(POST, "/availability")
        .withFormUrlEncodedBody()
        .withCSRFToken)

      status(bookSlot) mustBe SEE_OTHER
      redirectLocation(bookSlot).get must startWith("/auth/realms/id")
    }

    "render an error if the date is missing" in {
      val controller = authorisedController()
      val bookSlot = controller.availability().apply(FakeRequest(POST, "/availability")
        .withFormUrlEncodedBody()
        .withCSRFToken
      )
      val content = contentAsString(bookSlot)
      content must include("error.required")
    }

    "redirect to the booking page if the date is present" in {
      val controller = authorisedController()
      val request = FakeRequest(POST, "/book-slot")
        .withFormUrlEncodedBody(("date", date))

      val bookSlot = controller.availability().apply(request)

      status(bookSlot) mustBe SEE_OTHER
      redirectLocation(bookSlot).get must startWith(s"/book/$date")
    }
  }

  "BookingController bookContact" should {
    "return redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val bookContact = controller.bookContact(date, time)
        .apply(FakeRequest(GET, s"/book-contact/$date/$time"))

      status(bookContact) mustBe FOUND
      redirectLocation(bookContact).get must startWith("/auth/realms/id")
    }

    "render the booking contact page" in {
      val controller = authorisedController()
      val bookContact = controller.bookContact(date, time)
        .apply(FakeRequest(GET, s"/book-contact/$date/$time").withCSRFToken)

      contentAsString(bookContact) must include("Email address")
      contentAsString(bookContact) must include("Name")
      contentAsString(bookContact) must include("Phone number")
      contentAsString(bookContact) must include("Notes")
    }
  }

  "BookingController submitContactDetails" should {
    "return redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val submitContactDetails = controller.submitContactDetails(date, time)
        .apply(FakeRequest(POST, s"/book-contact/$date/$time"))

      status(submitContactDetails) mustBe SEE_OTHER
      redirectLocation(submitContactDetails).get must startWith("/auth/realms/id")
    }

    "render an error if the phone number is missing" in {
      val controller = authorisedController()
      val request = FakeRequest(POST, s"/book-contact/$date/$time")
        .withFormUrlEncodedBody(("email", "test@test.com"), ("name", "test"), ("notes", "test"))
        .withCSRFToken
      val submitContactDetails = controller.submitContactDetails(date, time)(request)

      contentAsString(submitContactDetails) must include ("error.required")
    }

    "render an error if the name is missing" in {
      val controller = authorisedController()
      val request = FakeRequest(POST, s"/book-contact/$date/$time")
        .withFormUrlEncodedBody(("email", "test@test.com"), ("phone", "test"), ("notes", "test"))
        .withCSRFToken
      val submitContactDetails = controller.submitContactDetails(date, time)(request)

      contentAsString(submitContactDetails) must include("error.required")
    }

    "update the calendar if the form is complete" in {
      val controller = authorisedController()
      val contactCaptor: ArgumentCaptor[Contact] = ArgumentCaptor.forClass(classOf[Contact])
      when(controller.emailService.send(any[String], any[String]))
        .thenReturn(Future.successful(mock[WSResponse]))
      when(controller.calendarService.putEvent(any[String], any[String], contactCaptor.capture()))
        .thenReturn(Future.successful(new Event()))

      val request = FakeRequest(POST, s"/book-contact/$date/$time")
        .withFormUrlEncodedBody(("email", "test@test.com"), ("name", "name"), ("phone", "phone"), ("notes", "test"))


      controller.submitContactDetails(date, time)(request).futureValue

      verify(controller.calendarService, atLeastOnce()).putEvent(any[String], any[String], any[Contact])

      val responseContact = contactCaptor.getValue
      responseContact.email must be ("test@test.com")
      responseContact.name must be ("name")
      responseContact.phone must be ("phone")
      responseContact.notes.get must be ("test")
    }

    "send an email if the form is complete" in {
      val controller = authorisedController()
      val emailCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val bodyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      when(controller.emailService.send(emailCaptor.capture(), bodyCaptor.capture()))
        .thenReturn(Future.successful(mock[WSResponse]))
      when(controller.calendarService.putEvent(any[String], any[String], any[Contact]))
        .thenReturn(Future.successful(new Event()))

      val request = FakeRequest(POST, s"/book-contact/$date/$time")
        .withFormUrlEncodedBody(("email", "test@test.com"), ("name", "name"), ("phone", "phone"), ("notes", "test"))


      controller.submitContactDetails(date, time)(request).futureValue

      verify(controller.emailService, atLeastOnce()).send(any[String], any[String])

      emailCaptor.getValue must be("test@test.com")
      bodyCaptor.getValue must be("""
                                    |Email: test@test.com
                                    |Name: name
                                    |Phone: phone
                                    |Notes: test
                                    |""".stripMargin)
    }

    "redirect to the thanks page if the form is complete" in {
      val controller = authorisedController()

      when(controller.emailService.send(any[String], any[String]))
        .thenReturn(Future.successful(mock[WSResponse]))
      when(controller.calendarService.putEvent(any[String], any[String], any[Contact]))
        .thenReturn(Future.successful(new Event()))

      val request = FakeRequest(POST, s"/book-contact/$date/$time")
        .withFormUrlEncodedBody(("email", "test@test.com"), ("name", "name"), ("phone", "phone"), ("notes", "test"))


      val bookContact = controller.submitContactDetails(date, time)(request)

      status(bookContact) must be(SEE_OTHER)
      redirectLocation(bookContact).get must be("/thanks")
    }
  }

  "BookingController thanks" should {
    "return redirect to the login page if the user is not logged in" in {
      val controller = unauthorisedController()
      val bookContact = controller.thanks()
        .apply(FakeRequest(GET, s"/thanks"))

      status(bookContact) mustBe FOUND
      redirectLocation(bookContact).get must startWith("/auth/realms/id")
    }

    "render the thanks page" in {
      val controller = authorisedController()
      val bookContact = controller.thanks()
        .apply(FakeRequest(GET, s"/thanks").withCSRFToken)

      contentAsString(bookContact) must include("Thanks for your email")
    }
  }


  private def unauthorisedController(calendarService: CalendarService = mock[CalendarService], emailService: EmailService = mock[EmailService]) = {
    new BookingController(unauthorisedSecurityComponents, calendarService, emailService)
  }

  private def authorisedController(calendarService: CalendarService = mock[CalendarService], emailService: EmailService = mock[EmailService]) = {
    new BookingController(authorisedSecurityComponents, calendarService, emailService)
  }
}
