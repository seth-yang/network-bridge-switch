package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.TunnelClientSchema;
import org.dreamwork.persistence.ISchema;
import org.dreamwork.persistence.ISchemaField;

import java.util.Objects;

/**
 * Created by seth.yang on 2019/12/24
 */
@ISchema (TunnelClientSchema.class)
public class TunnelClient {
    @ISchemaField (name = "token", id = true)
    private String token;

    @ISchemaField (name = "name")
    private String name;

    @ISchemaField (name = "password")
    private String password;

    @ISchemaField (name = "last_conn")
    private String lastConnectTime;

    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getPassword () {
        return password;
    }

    public void setPassword (String password) {
        this.password = password;
    }

    public String getLastConnectTime () {
        return lastConnectTime;
    }

    public void setLastConnectTime (String lastConnectTime) {
        this.lastConnectTime = lastConnectTime;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass () != o.getClass ()) return false;
        TunnelClient that = (TunnelClient) o;
        return token != null && token.equals (that.getToken ());
    }

    @Override
    public int hashCode () {
        return Objects.hash (token);
    }
}
