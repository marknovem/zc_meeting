/**
 * Copyright 2017 Djer13

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
limitations under the License.
 */
package org.zeroclick.meeting.server.sql.migrate.data;

import org.eclipse.scout.rt.server.jdbc.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroclick.meeting.server.sql.SQLs;
import org.zeroclick.meeting.server.sql.migrate.AbstractDataPatcher;

import com.github.zafarkhaja.semver.Version;

/**
 * @author djer
 *
 */
public class PatchCreateParamsTable extends AbstractDataPatcher {

	public static final String APP_PARAMS_TABLE_NAME = "app_params";
	public static final String APP_PARAMS_ID_SEQ = "APP_PARAMS_ID_SEQ";
	private static final Logger LOG = LoggerFactory.getLogger(PatchCreateParamsTable.class);

	public PatchCreateParamsTable() {
		this.setDescription("Create APP_PARAMS table and default required params key/value");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.zeroclick.meeting.server.sql.migrate.IDataPatcher#getVersion()
	 */
	@Override
	public Version getVersion() {
		return Version.valueOf("1.1.0");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.zeroclick.meeting.server.sql.migrate.AbstractDataPatcher#execute()
	 */
	@Override
	protected void execute() {
		if (super.canMigrate()) {
			LOG.info("Create param table will be apply to the data");
			final Boolean strtcureAltered = this.migrateStrucutre();
			if (strtcureAltered) {
				this.migrateData();
			}
		}
	}

	private Boolean migrateStrucutre() {
		LOG.info("Create param table upgrading data strcuture");
		Boolean structureAltered = Boolean.FALSE;
		if (!this.getDatabaseHelper().isSequenceExists(APP_PARAMS_ID_SEQ)) {
			this.getDatabaseHelper().createSequence(APP_PARAMS_ID_SEQ);
			structureAltered = Boolean.TRUE;
		}

		if (!this.getDatabaseHelper().existTable(APP_PARAMS_TABLE_NAME)) {
			SQL.insert(SQLs.PARAMS_CREATE_TABLE);
			structureAltered = Boolean.TRUE;
		}

		// as it create a Table force a refresh of Table Cache
		this.getDatabaseHelper().resetExistingTablesCache();

		return structureAltered;
	}

	private void migrateData() {
		LOG.info("Create param table upgraing default data");
		SQL.insert(SQLs.PARAMS_INSERT_SAMPLE + SQLs.PARAMS_INSERT_VALUES_DATAVERSION);
	}

	@Override
	public void undo() {
		LOG.info("Create param table downgrading data strcuture");
		this.getDatabaseHelper().dropSequence(APP_PARAMS_ID_SEQ);
		this.getDatabaseHelper().dropTable(APP_PARAMS_TABLE_NAME);
	}

}
