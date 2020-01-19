package org.dreamwork.network.sshd.data.schema;

import org.dreamwork.persistence.DatabaseSchema;

/**
 * Created by seth.yang on 2019/12/24
 */
public class TunnelClientSchema extends DatabaseSchema {
    public TunnelClientSchema () {
        tableName = "t_tunnel_client";
        fields    = new String[] {
                "token", "name", "password", "last_conn"
        };
    }

    @Override
    public String getCreateDDL () {
        return "CREATE TABLE t_tunnel_client (" +
                "    token           TEXT       NOT NULL PRIMARY KEY," +
                "    name            TEXT       NOT NULL," +
                "    password        TEXT       NOT NULL," +
                "    last_conn       TEXT" +
                ")";
    }

    @Override
    public String getPrimaryKeyName () {
        return "token";
    }
}
