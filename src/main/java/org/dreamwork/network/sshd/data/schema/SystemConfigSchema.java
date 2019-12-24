package org.dreamwork.network.sshd.data.schema;

import org.dreamwork.persistence.DatabaseSchema;

/**
 * Created by seth.yang on 2019/12/24
 */
public class SystemConfigSchema extends DatabaseSchema {
    public SystemConfigSchema () {
        tableName = "t_sys_conf";
        fields    = new String[] {
                "id", "_value"
        };
    }

    @Override
    public String getCreateDDL () {
        return "CREATE TABLE t_sys_conf (\n" +
                "    id              TEXT        NOT NULL PRIMARY,\n" +
                "    _value          TEXT\n" +
                ")";
    }

    @Override
    public String getPrimaryKeyName () {
        return "id";
    }
}
