package org.zeroclick.meeting.server.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.holders.NVPair;
import org.zeroclick.meeting.server.sql.AbstractCombinedMultiSqlLookupService;
import org.zeroclick.meeting.server.sql.SQLs;
import org.zeroclick.meeting.shared.event.IKnowEmailLookupService;
import org.zeroclick.meeting.shared.security.AccessControlService;

public class KnowEmailLookupService extends AbstractCombinedMultiSqlLookupService<String>
		implements IKnowEmailLookupService {

	@Override
	protected List<String> getConfiguredSqlSelects() {
		final List<String> sqls = new ArrayList<>();
		sqls.add(SQLs.EVENT_SELECT_KNOWN_ATTENDEE_LOOKUP);
		sqls.add(SQLs.EVENT_SELECT_KNOWN_HOST_LOOKUP);
		return sqls;
	}

	@Override
	protected Boolean getConfiguredRemoveDuplicate() {
		return Boolean.TRUE;
	}

	@Override
	protected Object getConfiguredBindBase() {
		final AccessControlService acs = BEANS.get(AccessControlService.class);
		final Long currentUserId = acs.getZeroClickUserIdOfCurrentSubject();
		return new NVPair("currentUser", currentUserId);
	}
}
