package org.dreamwork.network.sshd.data.schema;

import org.dreamwork.persistence.*;

public class DeviceSchema extends DatabaseSchema {
	public DeviceSchema () {
		tableName = "t_device";
		fields = new String[] {"id", "name", "host", "port", "user", "password"};
	}

	@Override
	public String getPrimaryKeyName () {
		return "id";
	}

	@Override
	public String getCreateDDL () {
		return "CREATE TABLE t_device (" +
"    id              INTEGER     NOT NULL PRIMARY KEY," +
"    name            TEXT," +
"    host            TEXT," +
"    port            INTEGER," +
"    user            TEXT," +
"    password        TEXT" +
")";
	}
}