package org.dreamwork.network.bridge.data.schema;

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
                "    id              INTEGER     NOT NULL PRIMARY KEY,\n" +
                "    src_port        INTEGER     NOT NULL,\n" +
                "    dest_host       TEXT        NOT NULL,\n" +
                "    dest_port       TEXT        NOT NULL,\n" +
                "    auto_bind       INTEGER     NOT NULL DEFAULT 0\n" +
                ")";
    }

    @Override
    public String getPrimaryKeyName () {
        return "id";
    }
}
