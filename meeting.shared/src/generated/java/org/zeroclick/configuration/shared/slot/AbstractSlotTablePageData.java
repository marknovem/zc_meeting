package org.zeroclick.configuration.shared.slot;

import java.util.Date;

import javax.annotation.Generated;

import org.eclipse.scout.rt.shared.data.basic.table.AbstractTableRowData;
import org.eclipse.scout.rt.shared.data.page.AbstractTablePageData;

/**
 * <b>NOTE:</b><br>
 * This class is auto generated by the Scout SDK. No manual modifications
 * recommended.
 */
@Generated(value = "org.zeroclick.configuration.client.slot.AbstractSlotTablePage", comments = "This class is auto generated by the Scout SDK. No manual modifications recommended.")
public abstract class AbstractSlotTablePageData extends AbstractTablePageData {

	private static final long serialVersionUID = 1L;

	@Override
	public AbstractSlotTableRowData addRow() {
		return (AbstractSlotTableRowData) super.addRow();
	}

	@Override
	public AbstractSlotTableRowData addRow(int rowState) {
		return (AbstractSlotTableRowData) super.addRow(rowState);
	}

	@Override
	public abstract AbstractSlotTableRowData createRow();

	@Override
	public Class<? extends AbstractTableRowData> getRowType() {
		return AbstractSlotTableRowData.class;
	}

	@Override
	public AbstractSlotTableRowData[] getRows() {
		return (AbstractSlotTableRowData[]) super.getRows();
	}

	@Override
	public AbstractSlotTableRowData rowAt(int index) {
		return (AbstractSlotTableRowData) super.rowAt(index);
	}

	public void setRows(AbstractSlotTableRowData[] rows) {
		super.setRows(rows);
	}

	public abstract static class AbstractSlotTableRowData extends AbstractTableRowData {

		private static final long serialVersionUID = 1L;
		public static final String name = "name";
		public static final String dayDurationId = "dayDurationId";
		public static final String start = "start";
		public static final String end = "end";
		public static final String slot = "slot";
		public static final String slotId = "slotId";
		public static final String userId = "userId";
		public static final String monday = "monday";
		public static final String tuesday = "tuesday";
		public static final String wednesday = "wednesday";
		public static final String thursday = "thursday";
		public static final String friday = "friday";
		public static final String saturday = "saturday";
		public static final String sunday = "sunday";
		private String m_name;
		private Long m_dayDurationId;
		private Date m_start;
		private Date m_end;
		private Integer m_slot;
		private Long m_slotId;
		private Long m_userId;
		private Boolean m_monday;
		private Boolean m_tuesday;
		private Boolean m_wednesday;
		private Boolean m_thursday;
		private Boolean m_friday;
		private Boolean m_saturday;
		private Boolean m_sunday;

		public String getName() {
			return m_name;
		}

		public void setName(String newName) {
			m_name = newName;
		}

		public Long getDayDurationId() {
			return m_dayDurationId;
		}

		public void setDayDurationId(Long newDayDurationId) {
			m_dayDurationId = newDayDurationId;
		}

		public Date getStart() {
			return m_start;
		}

		public void setStart(Date newStart) {
			m_start = newStart;
		}

		public Date getEnd() {
			return m_end;
		}

		public void setEnd(Date newEnd) {
			m_end = newEnd;
		}

		public Integer getSlot() {
			return m_slot;
		}

		public void setSlot(Integer newSlot) {
			m_slot = newSlot;
		}

		public Long getSlotId() {
			return m_slotId;
		}

		public void setSlotId(Long newSlotId) {
			m_slotId = newSlotId;
		}

		public Long getUserId() {
			return m_userId;
		}

		public void setUserId(Long newUserId) {
			m_userId = newUserId;
		}

		public Boolean getMonday() {
			return m_monday;
		}

		public void setMonday(Boolean newMonday) {
			m_monday = newMonday;
		}

		public Boolean getTuesday() {
			return m_tuesday;
		}

		public void setTuesday(Boolean newTuesday) {
			m_tuesday = newTuesday;
		}

		public Boolean getWednesday() {
			return m_wednesday;
		}

		public void setWednesday(Boolean newWednesday) {
			m_wednesday = newWednesday;
		}

		public Boolean getThursday() {
			return m_thursday;
		}

		public void setThursday(Boolean newThursday) {
			m_thursday = newThursday;
		}

		public Boolean getFriday() {
			return m_friday;
		}

		public void setFriday(Boolean newFriday) {
			m_friday = newFriday;
		}

		public Boolean getSaturday() {
			return m_saturday;
		}

		public void setSaturday(Boolean newSaturday) {
			m_saturday = newSaturday;
		}

		public Boolean getSunday() {
			return m_sunday;
		}

		public void setSunday(Boolean newSunday) {
			m_sunday = newSunday;
		}
	}
}