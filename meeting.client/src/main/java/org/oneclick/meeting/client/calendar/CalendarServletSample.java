package org.oneclick.meeting.client.calendar;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oneclick.meeting.client.ClientSession;
import org.oneclick.meeting.client.common.UserAccessRequiredException;
import org.oneclick.meeting.client.google.api.GoogleApiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

/**
 * Mapped to /addGoogleCalendar
 *
 * @author djer
 *
 */
public class CalendarServletSample extends HttpServlet {

	// // AbstractUiServletRequestHandler ???

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(CalendarServletSample.class);

	private final GoogleApiHelper googleApiHelper = GoogleApiHelper.get();

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		LOG.info("Entering do get");

		Calendar service;
		try {
			service = this.googleApiHelper.getCalendarService(this.googleApiHelper.getCurrentUserId());
		} catch (final UserAccessRequiredException uare) {
			this.googleApiHelper.askUserCredential(request, response);
			return; // early break to let user handle google auth flow
		}

		final ClientSession clientSession = ClientSession.get();

		if (null != clientSession && clientSession.getSubject().getPrincipals().size() > 0) {
			System.out.println("Principals of current user : " + clientSession.getSubject().getPrincipals());
		} else {
			System.out.println("No principal for current user");
			return; // not possible to get calendar/token with anonymous
		}

		// List of calendars
		final CalendarList calendars = service.calendarList().list().execute();

		final List<CalendarListEntry> calendarsItems = calendars.getItems();

		if (calendarsItems.size() == 0) {
			System.out.println("No calendars found.");
		} else {
			System.out.println("Calendriers : ");
			for (final CalendarListEntry calendar : calendarsItems) {
				System.out.println(calendar.getSummary() + " : " + calendar.getId() + "(" + calendar.isPrimary() + ")");
			}
		}

		// List the next 10 events from the primary calendar.
		final DateTime now = new DateTime(System.currentTimeMillis());
		final Events events = service.events().list("primary").setMaxResults(10).setTimeMin(now).setOrderBy("startTime")
				.setSingleEvents(true).execute();
		final List<Event> items = events.getItems();
		if (items.size() == 0) {
			System.out.println("No upcoming events found.");
		} else {
			System.out.println("Upcoming events");
			for (final Event event : items) {
				DateTime start = event.getStart().getDateTime();
				if (start == null) {
					start = event.getStart().getDate();
				}
				System.out.printf("%s (%s)\n", event.getSummary(), start);
			}
		}

		// getNext period available for first calendar

		// check on other calendars,
		// if "ok" done
		// else get next available period

	}
}
