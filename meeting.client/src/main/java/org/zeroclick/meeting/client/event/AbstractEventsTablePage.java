package org.zeroclick.meeting.client.event;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.rt.client.dto.Data;
import org.eclipse.scout.rt.client.ui.MouseButton;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractLongColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractSmartColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithTable;
import org.eclipse.scout.rt.client.ui.form.FormEvent;
import org.eclipse.scout.rt.client.ui.form.FormListener;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.platform.util.CompareUtility;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.notification.INotificationListener;
import org.eclipse.scout.rt.shared.services.common.code.ICode;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.shared.services.common.security.ACCESS;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroclick.common.params.AppParamsFormData;
import org.zeroclick.common.params.ParamCreatedNotificationHandler;
import org.zeroclick.common.params.ParamModifiedNotificationHandler;
import org.zeroclick.comon.date.DateHelper;
import org.zeroclick.comon.user.AppUserHelper;
import org.zeroclick.configuration.client.api.ApiCreatedNotificationHandler;
import org.zeroclick.configuration.client.slot.DayDurationModifiedNotificationHandler;
import org.zeroclick.configuration.client.user.UserModifiedNotificationHandler;
import org.zeroclick.configuration.shared.api.ApiCreatedNotification;
import org.zeroclick.configuration.shared.api.ApiDeletedNotification;
import org.zeroclick.configuration.shared.duration.DurationCodeType;
import org.zeroclick.configuration.shared.params.IAppParamsService;
import org.zeroclick.configuration.shared.params.ParamCreatedNotification;
import org.zeroclick.configuration.shared.params.ParamModifiedNotification;
import org.zeroclick.configuration.shared.slot.DayDurationFormData;
import org.zeroclick.configuration.shared.slot.DayDurationModifiedNotification;
import org.zeroclick.configuration.shared.slot.SlotCodeType;
import org.zeroclick.configuration.shared.user.IUserService;
import org.zeroclick.configuration.shared.user.UserFormData;
import org.zeroclick.configuration.shared.user.UserModifiedNotification;
import org.zeroclick.configuration.shared.venue.VenueLookupCall;
import org.zeroclick.meeting.client.NotificationHelper;
import org.zeroclick.meeting.client.calendar.CalendarConfigurationCreatedNotificationHandler;
import org.zeroclick.meeting.client.calendar.CalendarConfigurationModifiedNotificationHandler;
import org.zeroclick.meeting.client.calendar.CalendarsConfigurationCreatedNotificationHandler;
import org.zeroclick.meeting.client.calendar.CalendarsConfigurationModifiedNotificationHandler;
import org.zeroclick.meeting.client.common.CallTrackerService;
import org.zeroclick.meeting.client.event.EventTablePage.Table.NewEventMenu;
import org.zeroclick.meeting.client.google.api.GoogleApiHelper;
import org.zeroclick.meeting.shared.calendar.ApiFormData;
import org.zeroclick.meeting.shared.calendar.CalendarConfigurationCreatedNotification;
import org.zeroclick.meeting.shared.calendar.CalendarConfigurationFormData;
import org.zeroclick.meeting.shared.calendar.CalendarConfigurationModifiedNotification;
import org.zeroclick.meeting.shared.calendar.CalendarsConfigurationCreatedNotification;
import org.zeroclick.meeting.shared.calendar.CalendarsConfigurationFormData;
import org.zeroclick.meeting.shared.calendar.CalendarsConfigurationModifiedNotification;
import org.zeroclick.meeting.shared.event.EventCreatedNotification;
import org.zeroclick.meeting.shared.event.EventFormData;
import org.zeroclick.meeting.shared.event.EventModifiedNotification;
import org.zeroclick.meeting.shared.event.IEventService;
import org.zeroclick.meeting.shared.event.ReadEventExtendedPropsPermission;
import org.zeroclick.meeting.shared.event.StateCodeType;
import org.zeroclick.meeting.shared.event.UpdateEventPermission;
import org.zeroclick.meeting.shared.eventb.AbstractEventsTablePageData;
import org.zeroclick.meeting.shared.security.AccessControlService;
import org.zeroclick.ui.action.menu.AbstractCancelMenu;
import org.zeroclick.ui.form.columns.zoneddatecolumn.AbstractZonedDateColumn;

@Data(AbstractEventsTablePageData.class)
public abstract class AbstractEventsTablePage<T extends AbstractEventsTablePage<T>.Table>
		extends AbstractPageWithTable<T> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEventsTablePage.class);

	protected CallTrackerService<Long> callTracker;

	/**
	 * To avoid event processed twice, when no date available
	 */
	private final Set<Long> processedEventWithoutPossibleDates = new HashSet<>(3);

	private DateHelper dateHelper;
	private AppUserHelper appUserHelper;
	private EventMessageHelper eventMessageHelper;

	// TODO Djer13 is caching here smart ?
	private final Map<Long, String> cachedUserTimeZone = new HashMap<>();

	protected INotificationListener<ParamCreatedNotification> paramCreatedListener;
	protected INotificationListener<ParamModifiedNotification> paramModifiedListener;

	protected boolean isUserCalendarConfigured() {
		return BEANS.get(GoogleApiHelper.class).isCalendarConfigured();
	}

	protected boolean isOrganizerCalendarConfigured(final ITableRow row) {
		return BEANS.get(GoogleApiHelper.class)
				.isCalendarConfigured(this.getTable().getOrganizerColumn().getValue(row));
	}

	@Override
	protected String getConfiguredTitle() {
		return TEXTS.get("zc.meeting.events");
	}

	@Override
	protected boolean getConfiguredLeaf() {
		return Boolean.TRUE;
	}

	@Override
	protected boolean getConfiguredTableStatusVisible() {
		return Boolean.FALSE;
	}

	@Override
	protected void execInitPage() {
		final IAppParamsService appParamService = BEANS.get(IAppParamsService.class);
		Integer maxNbCall = 20;
		Integer maxDuration = 3;

		final String maxNbCallParam = appParamService.getValue(IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_MAX);
		final String maxDurationParam = appParamService
				.getValue(IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_DURATION);

		try {
			if (null != maxNbCallParam) {
				maxNbCall = Integer.valueOf(maxNbCallParam);
			}
		} catch (final NumberFormatException nfe) {
			LOG.warn("No params aviable for Calltracker event max ("
					+ IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_MAX + ") FallBack to default value");
		}

		try {
			maxDuration = Integer.valueOf(maxDurationParam);
		} catch (final NumberFormatException nfe) {
			LOG.warn("No params aviable for Calltracker event Duration ("
					+ IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_DURATION + ") FallBack to default value");
		}

		this.callTracker = new CallTrackerService<>(maxNbCall, Duration.ofMinutes(maxDuration), "Get calendar Events");

		final ParamCreatedNotificationHandler paramCreatedNotifHand = BEANS.get(ParamCreatedNotificationHandler.class);
		paramCreatedNotifHand.addListener(this.createParamCreatedListener());

		final ParamModifiedNotificationHandler paramModifiedNotifHand = BEANS
				.get(ParamModifiedNotificationHandler.class);
		paramModifiedNotifHand.addListener(this.createParamModifiedListener());

		super.execInitPage();
	}

	@Override
	protected void execDisposePage() {
		final ParamCreatedNotificationHandler paramCreatedNotifHand = BEANS.get(ParamCreatedNotificationHandler.class);
		paramCreatedNotifHand.removeListener(this.paramCreatedListener);

		final ParamModifiedNotificationHandler paramModifiedNotifHand = BEANS
				.get(ParamModifiedNotificationHandler.class);
		paramModifiedNotifHand.removeListener(this.paramModifiedListener);

		super.execDisposePage();
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final EventCreatedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @param previousStateRow
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final EventModifiedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final ApiCreatedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final ApiDeletedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final CalendarConfigurationCreatedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @param previousStateRow
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final CalendarConfigurationModifiedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @param previousStateRow
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final CalendarsConfigurationModifiedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Allow subClass to defined if they want to handle notification for this
	 * kind of event
	 *
	 * @param formdata
	 * @param previousStateRow
	 * @return true if this (sub) tablePage want handle the event
	 */
	protected Boolean canHandle(final CalendarsConfigurationCreatedNotification notification) {
		return Boolean.FALSE;
	}

	/**
	 * Propagate new Events to child page if implemented.Default do Nothing
	 *
	 * @param formData
	 */
	protected void onNewEvent(final EventFormData formData) {

	}

	/**
	 * Propagate modified Events to child page if implemented.Default do Nothing
	 *
	 * @param formData
	 */
	protected void onModifiedEvent(final EventFormData formData, final String previousStateRow,
			final ITableRow modifiedRow) {

	}

	/**
	 * Search for Locale Key
	 *
	 * @param formData
	 *            event Data to search for the key (mostly "state" is used)
	 * @return the key to use {@link TEXTS.get()}. <br />
	 *         Samples : <br/>
	 *         zc.meeting.notification.modifiedEvent.send (default for user who
	 *         modify the event)<br />
	 *         zc.meeting.notification.modifiedEvent.receive (default for user
	 *         who is informed of the modification)<br />
	 *         zc.meeting.notification.modifiedEvent.send.asked<br />
	 *         zc.meeting.notification.modifiedEvent.send.refused<br />
	 */
	protected String getDesktopNotificationModifiedEventKey(final EventFormData formData) {
		final Boolean isSender = this.getEventMessageHelper().isCurerntUserActor(formData);
		return this.getDesktopNotificationModifiedEventKey(formData, isSender);
	}

	/**
	 * @see getDesktopNotificationModifiedEventKey(EventFormData)
	 *
	 * @param formData
	 * @param isSender
	 *            does this message for user who modify the event (sender) or
	 *            the other (receiver) ?
	 * @return
	 */
	protected String getDesktopNotificationModifiedEventKey(final EventFormData formData, final Boolean isSender) {
		final String currentState = formData.getState().getValue().toLowerCase();
		final StringBuilder builder = new StringBuilder(60);

		builder.append("zc.meeting.notification.modifiedEvent");
		if (isSender) {
			builder.append(".send");
		} else {
			builder.append(".receive");
		}

		if (null != currentState && !currentState.isEmpty()) {
			builder.append('.');
			builder.append(currentState);
		}

		return builder.toString();
	}

	protected void addEventProcessedWithoutDates(final Long eventId) {
		this.processedEventWithoutPossibleDates.add(eventId);
	}

	private Boolean isEventProcessed(final Long eventId) {
		return this.processedEventWithoutPossibleDates.contains(eventId);
	}

	protected INotificationListener<ParamCreatedNotification> createParamCreatedListener() {
		this.paramCreatedListener = new INotificationListener<ParamCreatedNotification>() {
			@Override
			public void handleNotification(final ParamCreatedNotification notification) {

				final AppParamsFormData paramForm = notification.getFormData();
				AbstractEventsTablePage.this.updateEventCallTracker(paramForm);
			}
		};
		return this.paramCreatedListener;
	}

	protected INotificationListener<ParamModifiedNotification> createParamModifiedListener() {
		this.paramModifiedListener = new INotificationListener<ParamModifiedNotification>() {
			@Override
			public void handleNotification(final ParamModifiedNotification notification) {

				final AppParamsFormData paramForm = notification.getFormData();
				AbstractEventsTablePage.this.updateEventCallTracker(paramForm);
			}
		};
		return this.paramModifiedListener;
	}

	private void updateEventCallTracker(final AppParamsFormData paramForm) {
		final String appParamKey = paramForm.getKey().getValue();
		final String appParamValue = paramForm.getValue().getValue();
		if (LOG.isDebugEnabled()) {
			LOG.debug(new StringBuilder().append("New Param prepare to update Event Configuration (in ")
					.append(AbstractEventsTablePage.this.getConfiguredTitle()).append(") for param Id : ")
					.append(paramForm.getParamId()).append(" with key : ").append(appParamKey).toString());
		}
		try {

			if (null != appParamKey) {
				if (appParamKey.equals(IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_MAX)) {
					LOG.info("Updatding Events CallTracker max try with value : " + appParamValue);
					Integer maxSuccesivCall = null;
					try {
						maxSuccesivCall = Integer.valueOf(appParamValue);
						AbstractEventsTablePage.this.callTracker.setMaxSuccessiveCall(maxSuccesivCall);
					} catch (final NumberFormatException nfe) {
						LOG.warn("Cannot update Event Call Tracker max successive Call with invalid value : "
								+ appParamValue, nfe);
					}

				} else if (appParamKey.equals(IAppParamsService.APP_PARAM_KEY_EVENT_CALL_TRACKER_DURATION)) {
					LOG.info("Updatding Events CallTracker duration try with value : " + appParamValue);

					Integer ttl = null;
					try {
						ttl = Integer.valueOf(appParamValue);
						AbstractEventsTablePage.this.callTracker.setTimeToLive(Duration.ofMinutes(ttl));
					} catch (final NumberFormatException nfe) {
						LOG.warn("Cannot update Event Call Tracker ttl with invalid value : " + appParamValue, nfe);
					}
				}
			}

		} catch (final RuntimeException e) {
			LOG.error("Could not update Event configuration (new AppPAram). ("
					+ AbstractEventsTablePage.this.getConfiguredTitle() + ")", e);
		}
	}

	public class Table extends AbstractTable {

		protected INotificationListener<EventCreatedNotification> eventCreatedListener;
		protected INotificationListener<EventModifiedNotification> eventModifiedListener;
		protected INotificationListener<ApiCreatedNotification> apiCreatedListener;
		protected INotificationListener<UserModifiedNotification> userModifiedListener;
		protected INotificationListener<DayDurationModifiedNotification> dayDurationModifiedListener;

		private INotificationListener<CalendarsConfigurationModifiedNotification> calendarsConfiModifiedListener;
		private INotificationListener<CalendarsConfigurationCreatedNotification> calendarsConfiCreatedListener;
		private INotificationListener<CalendarConfigurationModifiedNotification> calendarConfigModifiedListener;
		private INotificationListener<CalendarConfigurationCreatedNotification> calendarConfiCreatedListener;

		@Override
		protected boolean getConfiguredHeaderEnabled() {
			return Boolean.FALSE;
		}

		@Override
		protected boolean getConfiguredSortEnabled() {
			return Boolean.FALSE;
		}

		@Override
		protected boolean getConfiguredTableStatusVisible() {
			return Boolean.FALSE;
		}

		@Override
		protected boolean getConfiguredAutoResizeColumns() {
			return Boolean.TRUE;
		}

		@Override
		protected void execInitTable() {
			super.execInitTable();
			this.setTableStatusVisible(Boolean.FALSE);
		}

		@Override
		protected void initConfig() {
			super.initConfig();
			this.setRowIconVisible(Boolean.FALSE);
			this.getEventIdColumn().setVisiblePermission(new ReadEventExtendedPropsPermission());
			this.getExternalIdOrganizerColumn().setVisiblePermission(new ReadEventExtendedPropsPermission());
			this.getExternalIdRecipientColumn().setVisiblePermission(new ReadEventExtendedPropsPermission());

			final EventCreatedNotificationHandler eventCreatedNotifHand = BEANS
					.get(EventCreatedNotificationHandler.class);
			eventCreatedNotifHand.addListener(this.createEventCreatedListener());

			final EventModifiedNotificationHandler eventModifiedNotifHand = BEANS
					.get(EventModifiedNotificationHandler.class);
			eventModifiedNotifHand.addListener(this.createEventModifiedListener());

			final ApiCreatedNotificationHandler apiCreatedNotifHand = BEANS.get(ApiCreatedNotificationHandler.class);
			apiCreatedNotifHand.addListener(this.createApiCreatedListener());

			final UserModifiedNotificationHandler userModifiedNotifHand = BEANS
					.get(UserModifiedNotificationHandler.class);
			userModifiedNotifHand.addListener(this.createUserModifiedListener());

			final DayDurationModifiedNotificationHandler dayDurationModifiedNotifHand = BEANS
					.get(DayDurationModifiedNotificationHandler.class);
			dayDurationModifiedNotifHand.addListener(this.createDayDurationModifiedListener());

			final CalendarsConfigurationModifiedNotificationHandler calendarsConfigModifiedNotifHand = BEANS
					.get(CalendarsConfigurationModifiedNotificationHandler.class);
			calendarsConfigModifiedNotifHand.addListener(this.createCalendarsConfigrationModifiedListener());

			final CalendarConfigurationModifiedNotificationHandler calendarConfiModifiedNotifHand = BEANS
					.get(CalendarConfigurationModifiedNotificationHandler.class);
			calendarConfiModifiedNotifHand.addListener(this.createCalendarConfigrationModifiedListener());

			final CalendarsConfigurationCreatedNotificationHandler calendarsConfigCreatedNotifHand = BEANS
					.get(CalendarsConfigurationCreatedNotificationHandler.class);
			calendarsConfigCreatedNotifHand.addListener(this.createCalendarsConfigrationCreatedListener());

			final CalendarConfigurationCreatedNotificationHandler calendarConfigCreatedNotifHand = BEANS
					.get(CalendarConfigurationCreatedNotificationHandler.class);
			calendarConfigCreatedNotifHand.addListener(this.createCalendarConfigrationCreatedListener());

		}

		protected INotificationListener<EventCreatedNotification> createEventCreatedListener() {
			this.eventCreatedListener = new INotificationListener<EventCreatedNotification>() {
				@Override
				public void handleNotification(final EventCreatedNotification notification) {

					if (!AbstractEventsTablePage.this.canHandle(notification)) {
						return; // Early Break
					}

					final EventFormData eventForm = notification.getFormData();
					if (LOG.isDebugEnabled()) {
						LOG.debug(new StringBuffer().append("New event prepare to add to table (in ")
								.append(Table.this.getTitle()).append(") for event Id : ")
								.append(eventForm.getEventId()).toString());
					}
					try {
						final ITableRow newRow = AbstractEventsTablePage.this.getTable()
								.addRow(AbstractEventsTablePage.this.getTable().createTableRowFromForm(eventForm));
						AbstractEventsTablePage.this.getTable().applyRowFilters();

						AbstractEventsTablePage.this.onNewEvent(eventForm);
						Table.this.autoFillDates(newRow);

						final NotificationHelper notificationHelper = BEANS.get(NotificationHelper.class);
						notificationHelper.addProccessedNotification(
								AbstractEventsTablePage.this.getDesktopNotificationModifiedEventKey(eventForm),
								AbstractEventsTablePage.this.getEventMessageHelper()
										.buildValuesForLocaleMessages(eventForm, Table.this.getCurrentUserId()));

					} catch (final RuntimeException e) {
						LOG.error("Could not add new event. (" + Table.this.getTitle() + ")", e);
					}
				}
			};
			return this.eventCreatedListener;
		}

		protected INotificationListener<EventModifiedNotification> createEventModifiedListener() {
			this.eventModifiedListener = new INotificationListener<EventModifiedNotification>() {
				@Override
				public void handleNotification(final EventModifiedNotification notification) {
					final EventFormData eventForm = notification.getFormData();
					if (!AbstractEventsTablePage.this.canHandle(notification)) {
						// remove row if exists
						final ITableRow row = AbstractEventsTablePage.this.getTable().getRow(eventForm.getEventId());
						if (null != row) {
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer().append("Modified event prepare to remove table row (in ")
										.append(Table.this.getTitle()).append(") for event Id : ")
										.append(eventForm.getEventId()).toString());
							}
							AbstractEventsTablePage.this.getTable().deleteRow(row);

							// TODO Djer13 not really a "modified" event, just a
							// row removed
							AbstractEventsTablePage.this.onModifiedEvent(eventForm, eventForm.getPreviousState(), row);
						}
						return; // early break
					}

					try {
						ITableRow row = AbstractEventsTablePage.this.getTable().getRow(eventForm.getEventId());
						if (null == row) {
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer().append("Modified event prepare to ADD table row (in ")
										.append(Table.this.getTitle()).append(") for event Id : ")
										.append(eventForm.getEventId()).toString());
							}
							row = AbstractEventsTablePage.this.getTable()
									.addRow(AbstractEventsTablePage.this.getTable().createTableRowFromForm(eventForm));
						}
						if (null != row) {
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer().append("Modified event prepare to modify table row (in ")
										.append(Table.this.getTitle()).append(") for event Id : ")
										.append(eventForm.getEventId()).toString());
							}
							// if row is null, this table instance should not
							// handle this event. We can safely ignore.
							final String previousStateRow = eventForm.getPreviousState();

							Table.this.updateTableRowFromForm(row, eventForm);
							AbstractEventsTablePage.this.getTable().applyRowFilters();
							AbstractEventsTablePage.this.onModifiedEvent(eventForm, previousStateRow, row);

							final NotificationHelper notificationHelper = BEANS.get(NotificationHelper.class);
							notificationHelper.addProccessedNotification(
									AbstractEventsTablePage.this.getDesktopNotificationModifiedEventKey(eventForm),
									AbstractEventsTablePage.this.getEventMessageHelper()
											.buildValuesForLocaleMessages(eventForm, Table.this.getCurrentUserId()));

						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer()
										.append("Modified event ignored because it's not a current table row (in ")
										.append(Table.this.getTitle()).append(") for event Id : ")
										.append(eventForm.getEventId()).toString());
							}
						}

					} catch (final RuntimeException e) {
						LOG.error("Could not update event. (" + Table.this.getTitle() + ") for event Id : "
								+ eventForm.getEventId(), e);
					}
				}
			};
			return this.eventModifiedListener;
		}

		private INotificationListener<ApiCreatedNotification> createApiCreatedListener() {
			this.apiCreatedListener = new INotificationListener<ApiCreatedNotification>() {
				@Override
				public void handleNotification(final ApiCreatedNotification notification) {

					if (!AbstractEventsTablePage.this.canHandle(notification)) {
						return; // Early Break
					}
					try {
						final ApiFormData eventForm = notification.getFormData();
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append("Created Api prepare to modify menus (")
									.append(this.getClass().getName()).append(") : ").append(eventForm.getUserId())
									.toString());
						}
						Table.this.reloadMenus();
						// calculate start/end meeting
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append("Created Api prepare to calculate start/end date (")
									.append(this.getClass().getName()).append(") : ").append(eventForm.getUserId())
									.toString());
						}
						final List<ITableRow> rows = Table.this.getRows();
						for (final ITableRow row : rows) {
							Table.this.refreshAutoFillDate(row);
						}
					} catch (final RuntimeException e) {
						LOG.error("Could not handle new api. (" + Table.this.getTitle() + ")", e);
					}
				}
			};

			return this.apiCreatedListener;
		}

		private INotificationListener<UserModifiedNotification> createUserModifiedListener() {
			this.userModifiedListener = new INotificationListener<UserModifiedNotification>() {
				@Override
				public void handleNotification(final UserModifiedNotification notification) {
					try {
						final UserFormData userForm = notification.getFormData();
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append("User modified prepare to reset cached TimeZone (")
									.append(Table.this.getTitle()).append(") : ").append(userForm.getUserId())
									.toString());
						}
						final List<ITableRow> rows = Table.this.getRows();
						for (final ITableRow row : rows) {
							Table.this.refreshAutoFillDate(row);
						}
					} catch (final RuntimeException e) {
						LOG.error("Could not handle modified User. (" + Table.this.getTitle() + ")", e);
					}
				}
			};

			return this.userModifiedListener;
		}

		private INotificationListener<DayDurationModifiedNotification> createDayDurationModifiedListener() {
			this.dayDurationModifiedListener = new INotificationListener<DayDurationModifiedNotification>() {
				@Override
				public void handleNotification(final DayDurationModifiedNotification notification) {
					try {
						final DayDurationFormData dayDurationForm = notification.getFormData();
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer()
									.append("Day Duration modified prepare to reset invalid date proposal (")
									.append(Table.this.getTitle()).append(") for slotCode : ")
									.append(notification.getFormData().getSlotCode())
									.append(" ( ID " + dayDurationForm.getDayDurationId()).append(")").toString());
						}
						Table.this.resetInvalidatesEvent(notification.getFormData().getSlotCode());

					} catch (final RuntimeException e) {
						LOG.error("Could not handle modified DayDuration. (" + Table.this.getTitle() + ")", e);
					}
				}
			};

			return this.dayDurationModifiedListener;
		}

		private INotificationListener<CalendarsConfigurationModifiedNotification> createCalendarsConfigrationModifiedListener() {
			this.calendarsConfiModifiedListener = new INotificationListener<CalendarsConfigurationModifiedNotification>() {
				@Override
				public void handleNotification(final CalendarsConfigurationModifiedNotification notification) {
					if (AbstractEventsTablePage.this.canHandle(notification)) {
						try {

							final CalendarsConfigurationFormData calendarsConfigurationFormData = notification
									.getFormData();
							Long userId = null;
							if (calendarsConfigurationFormData.getCalendarConfigTable().getRowCount() > 0) {
								userId = calendarsConfigurationFormData.getCalendarConfigTable().getRows()[0]
										.getUserId();
							}
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer()
										.append("Calendars Configurations modified prepare to refresh event (")
										.append(this.getClass().getName()).append(") : (first UserId)").append(userId)
										.toString());
							}

							if (Table.this.isMySelf(userId)) {
								final GoogleApiHelper googleApiHelper = BEANS.get(GoogleApiHelper.class);
								googleApiHelper.autoConfigureCalendars();

								final NotificationHelper notificationHelper = BEANS.get(NotificationHelper.class);
								notificationHelper.addProccessedNotification(
										"zc.meeting.calendar.notification.modifiedCalendarsConfig",
										googleApiHelper.getAccountsEmail(
												calendarsConfigurationFormData.getCalendarConfigTable().getRows()));
							}

							Table.this.refreshAutoFillDate();

						} catch (final RuntimeException e) {
							LOG.error("Could not handle modified Calendars Configurations. ("
									+ this.getClass().getName() + ")", e);
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append(this.getClass().getName())
									.append(" don't handle notification : ").append(notification).toString());
						}
					}
				}
			};

			return this.calendarsConfiModifiedListener;
		}

		private INotificationListener<CalendarConfigurationModifiedNotification> createCalendarConfigrationModifiedListener() {
			this.calendarConfigModifiedListener = new INotificationListener<CalendarConfigurationModifiedNotification>() {
				@Override
				public void handleNotification(final CalendarConfigurationModifiedNotification notification) {
					if (AbstractEventsTablePage.this.canHandle(notification)) {
						try {
							final CalendarConfigurationFormData calendarConfigurationFormData = notification
									.getFormData();
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer()
										.append("Calendar Configuration modified prepare to refresh event (")
										.append(this.getClass().getName()).append("), modified calendar : ")
										.append(calendarConfigurationFormData.getName().getValue()).append(" : ")
										.append(calendarConfigurationFormData.getUserId().getValue()).toString());
							}

							if (Table.this.isMySelf(calendarConfigurationFormData.getUserId().getValue())) {
								final GoogleApiHelper googleApiHelper = BEANS.get(GoogleApiHelper.class);
								googleApiHelper.autoConfigureCalendars();

								final NotificationHelper notificationHelper = BEANS.get(NotificationHelper.class);
								notificationHelper.addProccessedNotification(
										"zc.meeting.calendar.notification.modifiedCalendarConfig",
										calendarConfigurationFormData.getName().getValue());
							}

							Table.this.refreshAutoFillDate();

						} catch (final RuntimeException e) {
							LOG.error("Could not handle modified Calendar Configuration. (" + this.getClass().getName()
									+ ")", e);
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append(this.getClass().getName())
									.append(" don't handle notification : ").append(notification).toString());
						}
					}
				}
			};

			return this.calendarConfigModifiedListener;
		}

		private INotificationListener<CalendarsConfigurationCreatedNotification> createCalendarsConfigrationCreatedListener() {
			this.calendarsConfiCreatedListener = new INotificationListener<CalendarsConfigurationCreatedNotification>() {
				@Override
				public void handleNotification(final CalendarsConfigurationCreatedNotification notification) {
					if (AbstractEventsTablePage.this.canHandle(notification)) {
						try {
							final CalendarsConfigurationFormData calendarsConfigurationFormData = notification
									.getFormData();
							Long userId = null;
							if (calendarsConfigurationFormData.getCalendarConfigTable().getRowCount() > 0) {
								userId = calendarsConfigurationFormData.getCalendarConfigTable().getRows()[0]
										.getUserId();
							}
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer()
										.append("Calendars Configurations created prepare to refresh event (")
										.append(this.getClass().getName()).append(") : ").append(userId).toString());
							}

							Table.this.refreshAutoFillDate();

						} catch (final RuntimeException e) {
							LOG.error("Could not handle created Calendars Configuration. (" + this.getClass().getName()
									+ ")", e);
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append(this.getClass().getName())
									.append(" don't handle notification : ").append(notification).toString());
						}
					}
				}
			};

			return this.calendarsConfiCreatedListener;
		}

		private INotificationListener<CalendarConfigurationCreatedNotification> createCalendarConfigrationCreatedListener() {
			this.calendarConfiCreatedListener = new INotificationListener<CalendarConfigurationCreatedNotification>() {
				@Override
				public void handleNotification(final CalendarConfigurationCreatedNotification notification) {
					if (AbstractEventsTablePage.this.canHandle(notification)) {
						try {
							final CalendarConfigurationFormData calendarConfigurationFormData = notification
									.getFormData();
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuffer()
										.append("Calendar Configuration created prepare to refresh event (")
										.append(this.getClass().getName()).append(") : ")
										.append(calendarConfigurationFormData.getUserId().getValue()).toString());
							}

							if (Table.this.isMySelf(calendarConfigurationFormData.getUserId().getValue())) {
								final NotificationHelper notificationHelper = BEANS.get(NotificationHelper.class);
								notificationHelper.addProccessedNotification(
										"zc.meeting.calendar.notification.createdCalendarConfig",
										calendarConfigurationFormData.getName().getValue());
							}

							Table.this.refreshAutoFillDate();

						} catch (final RuntimeException e) {
							LOG.error("Could not handle created Calendar Configuration. (" + this.getClass().getName()
									+ ")", e);
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuffer().append(this.getClass().getName())
									.append(" don't handle notification : ").append(notification).toString());
						}
					}
				}
			};

			return this.calendarConfiCreatedListener;
		}

		protected void autoFillDates(final ITableRow row) {
			// TODO Auto-generated method stub
		}

		protected void autoFillDates() {
			final List<ITableRow> rows = this.getRows();
			for (final ITableRow row : rows) {
				this.autoFillDates(row);
			}
		}

		protected void resetInvalidatesEvent(final ZonedDateTime start, final ZonedDateTime end) {
			this.resetInvalidatesEvent(start, end, null);
		}

		protected void resetInvalidatesEvent(final ZonedDateTime start, final ZonedDateTime end,
				final Long exeptEventId) {
			final List<ITableRow> rows = this.getRows();

			for (final ITableRow row : rows) {
				if (this.getEventIdColumn().getValue(row).equals(exeptEventId)) {
					continue;
				}
				this.invalidateIfSlotAlreadyUsed(row, start, end);
			}
		}

		private void invalidateIfSlotAlreadyUsed(final ITableRow row, final ZonedDateTime newAcceptedEventstart,
				final ZonedDateTime newAcceptedEventEnd) {
			final ZonedDateTime rowStart = this.getStartDateColumn().getZonedValue(row.getRowIndex());
			final ZonedDateTime rowEnd = this.getEndDateColumn().getZonedValue(row.getRowIndex());
			final Long rowEventId = this.getEventIdColumn().getValue(row);
			final String rowState = this.getStateColumn().getValue(row);

			final Boolean isAsked = StateCodeType.AskedCode.ID.equals(rowState);

			if (null != rowStart && null != rowEnd && isAsked) {

				if (AbstractEventsTablePage.this.getDateHelper().isPeriodOverlap(rowStart, rowEnd,
						newAcceptedEventstart, newAcceptedEventEnd)) {
					LOG.info("Reseting start/end (" + rowStart + " / " + rowEnd + ") date for event id : " + rowEventId
							+ " because an other event was in the start/end period (" + newAcceptedEventstart + " / "
							+ newAcceptedEventEnd + ")");
					this.getStartDateColumn().setValue(row.getRowIndex(), (Date) null);
					this.getEndDateColumn().setValue(row.getRowIndex(), (Date) null);
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("No reset needed for event id : " + rowEventId);
					}
				}
			}
		}

		protected void resetInvalidatesEvent(final String slotCode) {
			final List<ITableRow> rows = this.getRows();
			for (final ITableRow row : rows) {
				this.invalidateIfSlotMatch(row, slotCode);
			}
		}

		private void invalidateIfSlotMatch(final ITableRow row, final String slotCode) {
			final Long rowSlotCode = this.getSlotColumn().getValue(row.getRowIndex());

			if (null != rowSlotCode && rowSlotCode.equals(Long.valueOf(slotCode))) {
				this.refreshAutoFillDate(row);
			}
		}

		protected void refreshAutoFillDate() {
			final List<ITableRow> rows = this.getRows();
			for (final ITableRow row : rows) {
				this.refreshAutoFillDate(row);
			}
		}

		protected void refreshAutoFillDate(final ITableRow row) {
			this.getStartDateColumn().setValue(row.getRowIndex(), (Date) null);
			this.getEndDateColumn().setValue(row.getRowIndex(), (Date) null);
			this.autoFillDates(row);
		}

		protected Boolean isTimeZoneValid(final Long userId) {
			Boolean timeZoneValid = Boolean.FALSE;

			if (!AbstractEventsTablePage.this.cachedUserTimeZone.containsKey(userId)) {
				final IUserService userService = BEANS.get(IUserService.class);
				final String currentUserTimeZone = userService.getUserTimeZone(userId);

				if (null == currentUserTimeZone) {
					LOG.warn("User " + userId + " hasen't set is timezone !");
				} else {
					AbstractEventsTablePage.this.cachedUserTimeZone.put(userId, currentUserTimeZone);
				}
			}

			timeZoneValid = null != AbstractEventsTablePage.this.cachedUserTimeZone.get(userId);

			return timeZoneValid;
		}

		protected Boolean isUserTimeZoneValid() {
			final Long currentUserId = AbstractEventsTablePage.this.getAppUserHelper().getCurrentUserId();

			return this.isTimeZoneValid(currentUserId);
		}

		public void resetCacheUserTimeZone(final Long userId) {
			AbstractEventsTablePage.this.cachedUserTimeZone.remove(userId);
		}

		protected Boolean canAutofillDates(final ITableRow row) {

			final Long eventId = this.getEventIdColumn().getValue(row.getRowIndex());
			final Long hostId = this.getOrganizerColumn().getValue(row.getRowIndex());
			final Long attendeeId = this.getGuestIdColumn().getValue(row.getRowIndex());
			final Boolean startDateEmpty = null == this.getStartDateColumn().getValue(row.getRowIndex());
			final Boolean endDateEmpty = null == this.getEndDateColumn().getValue(row.getRowIndex());
			final String rowState = this.getStateColumn().getValue(row.getRowIndex());
			final Boolean alreadyProcessed = AbstractEventsTablePage.this.isEventProcessed(eventId);
			return null != row && !alreadyProcessed && CompareUtility.equals(StateCodeType.AskedCode.ID, rowState)
					&& startDateEmpty && endDateEmpty && BEANS.get(GoogleApiHelper.class).isCalendarConfigured(hostId)
					// &&
					// BEANS.get(GoogleApiHelper.class).isCalendarConfigured(attendeeId)
					&& this.isTimeZoneValid(attendeeId) && this.isTimeZoneValid(hostId) && this.isGuestCurrentUser(row);
		}

		@Override
		protected void execDisposeTable() {
			final EventCreatedNotificationHandler eventCreatedNotifHand = BEANS
					.get(EventCreatedNotificationHandler.class);
			eventCreatedNotifHand.removeListener(this.eventCreatedListener);
			final EventModifiedNotificationHandler eventModifiedNotifHand = BEANS
					.get(EventModifiedNotificationHandler.class);
			eventModifiedNotifHand.removeListener(this.eventModifiedListener);

			final ApiCreatedNotificationHandler apiCreatedNotificationHandler = BEANS
					.get(ApiCreatedNotificationHandler.class);
			apiCreatedNotificationHandler.removeListener(this.apiCreatedListener);

			final UserModifiedNotificationHandler userModifeidNotifHand = BEANS
					.get(UserModifiedNotificationHandler.class);
			userModifeidNotifHand.removeListener(this.userModifiedListener);

			final DayDurationModifiedNotificationHandler dayDurationModifiedNotifHand = BEANS
					.get(DayDurationModifiedNotificationHandler.class);
			dayDurationModifiedNotifHand.removeListener(this.dayDurationModifiedListener);

			final CalendarsConfigurationModifiedNotificationHandler calendarsConfigModifiedNotifHand = BEANS
					.get(CalendarsConfigurationModifiedNotificationHandler.class);
			calendarsConfigModifiedNotifHand.removeListener(this.calendarsConfiModifiedListener);

			final CalendarConfigurationModifiedNotificationHandler calendarConfigModifiedNotifHand = BEANS
					.get(CalendarConfigurationModifiedNotificationHandler.class);
			calendarConfigModifiedNotifHand.removeListener(this.calendarConfigModifiedListener);

			final CalendarsConfigurationCreatedNotificationHandler calendarsConfiCreatedNotifHand = BEANS
					.get(CalendarsConfigurationCreatedNotificationHandler.class);
			calendarsConfiCreatedNotifHand.removeListener(this.calendarsConfiCreatedListener);

			final CalendarConfigurationCreatedNotificationHandler calendarConfigCreatedNotifHand = BEANS
					.get(CalendarConfigurationCreatedNotificationHandler.class);
			calendarConfigCreatedNotifHand.removeListener(this.calendarConfiCreatedListener);

			super.execDisposeTable();
		}

		protected ITableRow getRow(final Long eventId) {
			final List<ITableRow> currentRows = this.getRows();
			for (final ITableRow aRow : currentRows) {
				if (eventId.equals(aRow.getCell(this.getEventIdColumn()).getValue())) {
					return aRow;
				}
			}

			return null;
		}

		@Override
		protected void execDecorateRow(final ITableRow row) {

			if (this.isHeldByCurrentUser(row)) {
				// row.setIconId(Icons.AngleDoubleLeft);
				this.getOrganizerEmailColumn().updateDisplayText(row, TEXTS.get("zc.common.me"));
			}

			if (this.isGuestCurrentUser(row)) {
				// row.setIconId(Icons.AngleDoubleRight);
				this.getEmailColumn().updateDisplayText(row, TEXTS.get("zc.common.me"));
			}

			final ZonedDateTime currentStartDate = this.getStartDateColumn().getZonedValue(row.getRowIndex());
			this.getStartDateColumn().updateDisplayText(row,
					AbstractEventsTablePage.this.getDateHelper().toUserDate(currentStartDate));

			final ZonedDateTime currentEndDate = this.getEndDateColumn().getZonedValue(row.getRowIndex());
			this.getEndDateColumn().updateDisplayText(row,
					AbstractEventsTablePage.this.getDateHelper().toUserDate(currentEndDate));
		}

		private List<Object> getListFromForm(final EventFormData formData) {
			final List<Object> datas = new ArrayList<>();
			datas.add(formData.getEventId());
			datas.add(formData.getOrganizer().getValue());
			datas.add(formData.getOrganizerEmail().getValue());
			datas.add(formData.getCreatedDate().getValue());
			datas.add(formData.getGuestId().getValue());
			datas.add(formData.getEmail().getValue());
			datas.add(formData.getSubject().getValue());
			datas.add(formData.getSlot().getValue());
			datas.add(formData.getMinimalStartDate().getValue());
			datas.add(formData.getMaximalStartDate().getValue());
			datas.add(formData.getDuration().getValue());
			datas.add(formData.getVenue().getValue());
			datas.add(formData.getState().getValue());
			datas.add(formData.getStartDate().getValue());
			datas.add(formData.getEndDate().getValue());
			datas.add(formData.getExternalIdOrganizer());
			datas.add(formData.getExternalIdRecipient());
			datas.add(formData.getReason().getValue());
			return datas;
		}

		protected ITableRow createTableRowFromForm(final EventFormData formData) {
			return new TableRow(this.getColumnSet(), this.getListFromForm(formData));
		}

		protected void updateTableRowFromForm(final ITableRow row, final EventFormData formData) {
			if (null != row) {
				final List<Object> datas = this.getListFromForm(formData);
				for (int i = 0; i < datas.size(); i++) {
					final Object propertyFormData = datas.get(i);
					final ICell cell = row.getCell(i);
					if (propertyFormData != cell) {
						// TODO enable validation ??
						// row.getTable().getColumns().get(i).setValue(row,
						// propertyFormData);
						row.setCellValue(i, propertyFormData);
					}
				}
			}
		}

		protected ITableRow getOwnerAsTableRow(final Object newOwnerValue) {
			if (newOwnerValue instanceof List) {
				final Object firstOwner = ((List) newOwnerValue).get(0);
				if (firstOwner instanceof ITableRow) {
					return (ITableRow) firstOwner;
				}
			}
			return null;
		}

		/**
		 * Is row Held by CurrentUser ?
		 *
		 * @param row
		 * @return
		 */
		protected Boolean isHeldByCurrentUser(final ITableRow row) {
			return AbstractEventsTablePage.this.getEventMessageHelper()
					.isOrganizer(this.getOrganizerColumn().getValue(row.getRowIndex()));
		}

		/**
		 * Is current user Guest for row ?
		 *
		 * @param row
		 * @return
		 */
		protected Boolean isGuestCurrentUser(final ITableRow row) {
			String guestEmail = null;
			final String rowEmail = this.getEmailColumn().getValue(row.getRowIndex());
			if (null != rowEmail) {
				guestEmail = rowEmail.toLowerCase();
			}
			return this.isGuest(guestEmail);
		}

		protected Boolean isGuest(final String email) {
			final IUserService userService = BEANS.get(IUserService.class);
			final UserFormData userDetails = userService.getCurrentUserDetails();

			final String currentUserEmail = userDetails.getEmail().getValue();

			return currentUserEmail.equals(email);
		}

		protected Boolean isMySelf(final Long userId) {
			final Long currentUser = AbstractEventsTablePage.this.getAppUserHelper().getCurrentUserId();
			return currentUser.equals(userId);
		}

		protected String getState(final ITableRow row) {
			return Table.this.getStateColumn().getValue(row.getRowIndex());
		}

		protected Boolean userCanUpdate(final ITableRow row) {
			final Long currentEventId = Table.this.getEventIdColumn().getValue(row.getRowIndex());

			// BEANS.get(AccessControlService.class).clearCacheOfCurrentUser();
			return ACCESS.check(new UpdateEventPermission(currentEventId));
		}

		protected Boolean userCanChooseDate(final ITableRow row) {
			// TODO Djer13 create a specific "ChooseDateEventPermisison" ?
			return this.userCanUpdate(row);
		}

		protected Boolean userCanAccept(final ITableRow row) {
			// TODO Djer13 create a specific "AcceptEventPermisison" ?
			return this.userCanUpdate(row);
		}

		protected Boolean userCanReject(final ITableRow row) {
			// TODO Djer13 create a specific "RejectEventPermisison" ?
			return this.userCanUpdate(row);
		}

		protected Boolean userCanCancel(final ITableRow row) {
			// TODO Djer13 create a specific "CancelEventPermisison" ?
			return this.userCanUpdate(row);
		}

		protected List<String> getCurrentUserEmails() {
			final List<String> emails = new ArrayList<>();
			final IUserService userService = BEANS.get(IUserService.class);
			final UserFormData userDetails = userService.getCurrentUserDetails();

			emails.add(userDetails.getEmail().getValue());

			return emails;
		}

		protected Long getCurrentUserId() {
			final AccessControlService acs = BEANS.get(AccessControlService.class);
			return acs.getZeroClickUserIdOfCurrentSubject();
		}

		@Override
		protected void execRowClick(final ITableRow row, final MouseButton mouseButton) {
			this.reloadMenus(row);
		}

		/**
		 * Allow a "refresh" of menu even if no "owner" has changed
		 *
		 * @param row
		 *            the selected row (owner) may be null;
		 */
		protected void reloadMenus(final ITableRow row) {
			final List<IMenu> menus = this.getMenus();
			for (final IMenu menu : menus) {
				// menu.initAction();
				menu.handleOwnerValueChanged(row);
			}
		}

		protected void reloadMenus() {
			final ITableRow currentRow = this.getSelectedRow();
			if (null != currentRow) {
				this.reloadMenus(currentRow);
			} else {
				final IMenu newEventMenu = this.getMenuByClass(NewEventMenu.class);
				if (null != newEventMenu) {
					newEventMenu.setVisible(Boolean.TRUE);
				}
			}
		}

		/**
		 * save the current selected row to the database
		 *
		 * @return the saved event Datas
		 */
		protected EventFormData saveEventCurrentRow() {
			final EventFormData formData = this.saveEvent(this.getSelectedRow());
			this.reloadMenus(this.getSelectedRow());

			return formData;
		}

		private EventFormData saveEvent(final ITableRow selectedRow) {
			EventFormData eventFormData = new EventFormData();
			final Integer rowIndex = selectedRow.getRowIndex();
			final IEventService service = BEANS.get(IEventService.class);

			eventFormData.setEventId(this.getEventIdColumn().getValue(rowIndex));

			eventFormData = service.load(eventFormData);

			eventFormData.getStartDate().setValue(this.getStartDateColumn().getValue(rowIndex));
			eventFormData.getEndDate().setValue(this.getEndDateColumn().getValue(rowIndex));
			eventFormData.setExternalIdRecipient(this.getExternalIdRecipientColumn().getValue(rowIndex));
			eventFormData.setExternalIdOrganizer(this.getExternalIdOrganizerColumn().getValue(rowIndex));
			eventFormData.getState().setValue(this.getStateColumn().getValue(rowIndex));

			return service.store(eventFormData);

		}

		public abstract class AbstractRowAwareMenu extends AbstractMenu {
			public abstract void handleRowChanged(final ITableRow row);
		}

		@Order(3000)
		public class RefuseMenu extends AbstractCancelMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("zc.meeting.refuse");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			private Boolean isWorkflowVisible(final String currentState) {

				Boolean isVisible = Boolean.FALSE;
				if (CompareUtility.equals(StateCodeType.AskedCode.ID, currentState)
						&& Table.this.isGuestCurrentUser(Table.this.getSelectedRow())) {
					isVisible = Boolean.TRUE;
				} else if (CompareUtility.equals(StateCodeType.AcceptedCode.ID, currentState)) {
					isVisible = Boolean.TRUE;
				}
				return isVisible;
			}

			@Override
			protected void execOwnerValueChanged(final Object newOwnerValue) {
				final ITableRow row = Table.this.getOwnerAsTableRow(newOwnerValue);
				if (null != row) {
					this.setVisible(this.isWorkflowVisible(Table.this.getState(row)) && Table.this.userCanReject(row));
					// this.setEnabled(AbstractEventsTablePage.this.isUserCalendarConfigured());
					// TODO Djer13 hide if date.start and date.end are in the
					// past ?
				}
			}

			@Override
			protected void execAction() {

				final RejectEventForm form = new RejectEventForm();
				final Long currentEventId = Table.this.getEventIdColumn().getSelectedValue();
				final ZonedDateTime start = Table.this.getStartDateColumn().getSelectedZonedValue();
				final ZonedDateTime end = Table.this.getEndDateColumn().getSelectedZonedValue();
				form.setEventId(currentEventId);
				form.setStart(start);
				form.setEnd(end);
				form.setEnabledPermission(new UpdateEventPermission(currentEventId));
				form.addFormListener(
						org.zeroclick.meeting.client.event.AbstractEventsTablePage.Table.this.refuseCancelFormListener);
				// start the form using its modify handler
				form.startReject(Table.this.isHeldByCurrentUser(Table.this.getSelectedRow()));

			}

			@Override
			protected String getConfiguredKeyStroke() {
				return combineKeyStrokes(IKeyStroke.SHIFT, "r");
			}
		}

		public FormListener refuseCancelFormListener = new FormListener() {

			@Override
			public void formChanged(final FormEvent event) {
				if (FormEvent.TYPE_CLOSED == event.getType() && event.getForm().isFormStored()) {
					try {
						final RejectEventForm rejectEventForm = (RejectEventForm) event.getForm();
						if (null == rejectEventForm.getStart()) {
							LOG.warn(
									"Cannot re-calculate start/end date after cancel/reject meeting because start date was NULL for event : "
											+ rejectEventForm.getEventId());
						} else if (null == rejectEventForm.getEnd()) {
							LOG.warn(
									"Cannot re-calculate start/end date after cancel/reject meeting because end date was NULL for event : "
											+ rejectEventForm.getEventId());
						} else {
							Table.this.resetInvalidatesEvent(rejectEventForm.getStart(), rejectEventForm.getEnd(),
									rejectEventForm.getEventId());
							Table.this.autoFillDates();
							// Autofill handled by EventModifiedNotification
						}
					} catch (final ClassCastException cce) {
						LOG.warn("Cannot re-calculate start/end date after cancel/reject meeting", cce);
					}
				}

			}
		};

		@Order(4000)
		public class CancelEventMenu extends AbstractCancelMenu {
			@Override
			protected String getConfiguredText() {
				return TEXTS.get("Cancel");
			}

			@Override
			protected boolean getConfiguredVisible() {
				// to avoid button blink
				return Boolean.FALSE;
			}

			private Boolean isWorkflowVisible(final String currentState) {
				Boolean isVisible = Boolean.FALSE;
				if (CompareUtility.equals(StateCodeType.AskedCode.ID, currentState)
						&& Table.this.isHeldByCurrentUser(Table.this.getSelectedRow())) {
					isVisible = Boolean.TRUE;
				}
				return isVisible;
			}

			@Override
			protected void execOwnerValueChanged(final Object newOwnerValue) {
				final ITableRow row = Table.this.getOwnerAsTableRow(newOwnerValue);
				if (null != row) {
					this.setVisible(this.isWorkflowVisible(Table.this.getState(row)) && Table.this.userCanCancel(row));
					// this.setEnabled(AbstractEventsTablePage.this.isUserCalendarConfigured());
					// TODO Djer13 hide if date.start and date.end are in the
					// past ?
				}
			}

			@Override
			protected void execAction() {
				final RejectEventForm form = new RejectEventForm();
				final Long currentEventId = Table.this.getEventIdColumn().getSelectedValue();
				form.setEventId(currentEventId);
				form.setEnabledPermission(new UpdateEventPermission(currentEventId));
				// form.addFormListener(
				// org.zeroclick.meeting.client.event.AbstractEventsTablePage.Table.this.refuseCancelFormListener);
				form.startCancel(Table.this.isHeldByCurrentUser(Table.this.getSelectedRow()));
			}
		}

		public AbstractEventsTablePage<?>.Table.DurationColumn getDurationColumn() {
			return this.getColumnSet().getColumnByClass(DurationColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.EmailColumn getEmailColumn() {
			return this.getColumnSet().getColumnByClass(EmailColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.StartDateColumn getStartDateColumn() {
			return this.getColumnSet().getColumnByClass(StartDateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.EndDateColumn getEndDateColumn() {
			return this.getColumnSet().getColumnByClass(EndDateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.StateColumn getStateColumn() {
			return this.getColumnSet().getColumnByClass(StateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.ExternalIdOrganizerColumn getExternalIdOrganizerColumn() {
			return this.getColumnSet().getColumnByClass(ExternalIdOrganizerColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.ExternalIdRecipientColumn getExternalIdRecipientColumn() {
			return this.getColumnSet().getColumnByClass(ExternalIdRecipientColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.OrganizerEmailColumn getOrganizerEmailColumn() {
			return this.getColumnSet().getColumnByClass(OrganizerEmailColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.GuestIdColumn getGuestIdColumn() {
			return this.getColumnSet().getColumnByClass(GuestIdColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.SubjectColumn getSubjectColumn() {
			return this.getColumnSet().getColumnByClass(SubjectColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.ReasonColumn getReasonColumn() {
			return this.getColumnSet().getColumnByClass(ReasonColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.VenueColumn getVenueColumn() {
			return this.getColumnSet().getColumnByClass(VenueColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.CreatedDateColumn getCreatedDateColumn() {
			return this.getColumnSet().getColumnByClass(CreatedDateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.MinimalStartDateColumn getMinimalStartDateColumn() {
			return this.getColumnSet().getColumnByClass(MinimalStartDateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.MaximalStartDateColumn getMaximalStartDateColumn() {
			return this.getColumnSet().getColumnByClass(MaximalStartDateColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.OrganizerColumn getOrganizerColumn() {
			return this.getColumnSet().getColumnByClass(OrganizerColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.SlotColumn getSlotColumn() {
			return this.getColumnSet().getColumnByClass(SlotColumn.class);
		}

		public AbstractEventsTablePage<?>.Table.EventIdColumn getEventIdColumn() {
			return this.getColumnSet().getColumnByClass(EventIdColumn.class);
		}

		@Order(1)
		public class EventIdColumn extends AbstractLongColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.common.id");
			}

			@Override
			protected boolean getConfiguredDisplayable() {
				return Boolean.TRUE;
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected boolean getConfiguredPrimaryKey() {
				return true;
			}
		}

		@Order(25)
		public class OrganizerColumn extends AbstractLongColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.hostId");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 128;
			}
		}

		@Order(30)
		public class OrganizerEmailColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.host");
			}

			@Override
			protected boolean getConfiguredSummary() {
				return Boolean.TRUE;
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 150;
			}
		}

		@Order(35)
		public class CreatedDateColumn extends AbstractZonedDateColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.createdDate");
			}

			@Override
			protected int getConfiguredSortIndex() {
				return 0;
			}

			@Override
			protected boolean getConfiguredSortAscending() {
				return Boolean.TRUE;
			}

			@Override
			protected boolean getConfiguredHasTime() {
				return Boolean.TRUE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(40)
		public class GuestIdColumn extends AbstractLongColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.attendeeId");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 128;
			}
		}

		@Order(50)
		public class EmailColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.attendee");
			}

			@Override
			protected boolean getConfiguredSummary() {
				return Boolean.TRUE;
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 150;
			}
		}

		@Order(60)
		public class SubjectColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.event.subject");
			}

			@Override
			protected boolean getConfiguredSummary() {
				return Boolean.TRUE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 256;
			}
		}

		@Order(100)
		public class SlotColumn extends AbstractSmartColumn<Long> {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.slot");
			}

			@Override
			protected int getConfiguredWidth() {
				return 150;
			}

			@Override
			protected Class<? extends ICodeType<Long, Long>> getConfiguredCodeType() {
				return SlotCodeType.class;
			}
		}

		@Order(150)
		public class MinimalStartDateColumn extends AbstractZonedDateColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.minimalStartDate");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected boolean getConfiguredHasTime() {
				return Boolean.TRUE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(175)
		public class MaximalStartDateColumn extends AbstractZonedDateColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.maximalStartDate");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected boolean getConfiguredHasTime() {
				return Boolean.TRUE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(200)
		public class DurationColumn extends AbstractSmartColumn<Long> {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.duration");
			}

			@Override
			protected int getConfiguredWidth() {
				return 96;
			}

			@Override
			protected Class<? extends ICodeType<Long, Long>> getConfiguredCodeType() {
				return DurationCodeType.class;
			}
		}

		@Order(300)
		public class VenueColumn extends AbstractSmartColumn<String> {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.venue");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}

			@Override
			protected Class<? extends ILookupCall<String>> getConfiguredLookupCall() {
				return VenueLookupCall.class;
			}
		}

		@Order(1100)
		public class StateColumn extends AbstractSmartColumn<String> {
			// In User context, so texts are translated
			final StateCodeType eventStateCodes = new StateCodeType();

			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.state");
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected void execDecorateCell(final Cell cell, final ITableRow row) {
				super.execDecorateCell(cell, row);

				final String stateColumnValue = (String) cell.getValue();

				final ICode<String> currentStateCode = this.eventStateCodes.getCode(stateColumnValue);
				cell.setIconId(currentStateCode.getIconId());
				cell.setBackgroundColor(currentStateCode.getBackgroundColor());
				cell.setForegroundColor(currentStateCode.getForegroundColor());
				cell.setText(currentStateCode.getText());
			}

			@Override
			protected Class<? extends ICodeType<Long, String>> getConfiguredCodeType() {
				return StateCodeType.class;
			}
		}

		@Order(2000)
		public class StartDateColumn extends AbstractZonedDateColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.start");
			}
		}

		@Order(3000)
		public class EndDateColumn extends AbstractZonedDateColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.end");
			}
		}

		@Order(4500)
		public class ExternalIdOrganizerColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.externalId");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(5000)
		public class ExternalIdRecipientColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.externalId");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		@Order(6000)
		public class ReasonColumn extends AbstractStringColumn {
			@Override
			protected String getConfiguredHeaderText() {
				return TEXTS.get("zc.meeting.rejectReason");
			}

			@Override
			protected boolean getConfiguredVisible() {
				return Boolean.FALSE;
			}

			@Override
			protected int getConfiguredWidth() {
				return 100;
			}
		}

		// StartFrom
	}

	protected DateHelper getDateHelper() {
		if (null == this.dateHelper) {
			this.dateHelper = BEANS.get(DateHelper.class);
		}
		return this.dateHelper;
	}

	protected AppUserHelper getAppUserHelper() {
		if (null == this.appUserHelper) {
			this.appUserHelper = BEANS.get(AppUserHelper.class);
		}
		return this.appUserHelper;
	}

	protected EventMessageHelper getEventMessageHelper() {
		if (null == this.eventMessageHelper) {
			this.eventMessageHelper = BEANS.get(EventMessageHelper.class);
		}
		return this.eventMessageHelper;
	}

}
