package org.dreamwork.network.sshd.data.schema;

import org.dreamwork.persistence.DatabaseSchema;

/**
 * Created by seth.yang on 2019/11/13
 */
public class NATSchema extends DatabaseSchema {
    public NATSchema () {
        tableName   = "t_nat";
        fields      = new String[] {"id", "src_port", "dest_host", "dest_port", "auto_bind"};
    }

    @Override
    public String getCreateDDL () {
        return "CREATE TABLE t_nat (\n" +
                "    id              INTEGER     NOT NULL PRIMARY KEY," +
                "    src_port        INTEGER     NOT NULL," +
                "    dest_host       TEXT        NOT NULL," +
                "    dest_port       TEXT        NOT NULL," +
                "    auto_bind       INTEGER     NOT NULL DEFAULT 0" +
                ")";
    }

    @Override
    public String getPrimaryKeyName () {
        return "id";
    }
}
