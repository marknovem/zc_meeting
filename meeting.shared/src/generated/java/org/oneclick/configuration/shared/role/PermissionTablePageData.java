package org.oneclick.configuration.shared.role;

import javax.annotation.Generated;

import org.eclipse.scout.rt.shared.data.basic.table.AbstractTableRowData;
import org.eclipse.scout.rt.shared.data.page.AbstractTablePageData;

/**
 * <b>NOTE:</b><br>
 * This class is auto generated by the Scout SDK. No manual modifications
 * recommended.
 */
@Generated(value = "org.oneclick.configuration.client.role.PermissionTablePage", comments = "This class is auto generated by the Scout SDK. No manual modifications recommended.")
public class PermissionTablePageData extends AbstractTablePageData {

	private static final long serialVersionUID = 1L;

	@Override
	public PermissionTableRowData addRow() {
		return (PermissionTableRowData) super.addRow();
	}

	@Override
	public PermissionTableRowData addRow(int rowState) {
		return (PermissionTableRowData) super.addRow(rowState);
	}

	@Override
	public PermissionTableRowData createRow() {
		return new PermissionTableRowData();
	}

	@Override
	public Class<? extends AbstractTableRowData> getRowType() {
		return PermissionTableRowData.class;
	}

	@Override
	public PermissionTableRowData[] getRows() {
		return (PermissionTableRowData[]) super.getRows();
	}

	@Override
	public PermissionTableRowData rowAt(int index) {
		return (PermissionTableRowData) super.rowAt(index);
	}

	public void setRows(PermissionTableRowData[] rows) {
		super.setRows(rows);
	}

	public static class PermissionTableRowData extends AbstractTableRowData {

		private static final long serialVersionUID = 1L;
		public static final String permissionName = "permissionName";
		public static final String level = "level";
		private String m_permissionName;
		private Integer m_level;

		public String getPermissionName() {
			return m_permissionName;
		}

		public void setPermissionName(String newPermissionName) {
			m_permissionName = newPermissionName;
		}

		public Integer getLevel() {
			return m_level;
		}

		public void setLevel(Integer newLevel) {
			m_level = newLevel;
		}
	}
}
