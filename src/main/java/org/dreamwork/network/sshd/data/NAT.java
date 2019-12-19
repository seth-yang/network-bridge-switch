package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.NATSchema;
import org.dreamwork.persistence.ISchema;
import org.dreamwork.persistence.ISchemaField;

/**
 * Created by seth.yang on 2019/11/13
 */
@ISchema (NATSchema.class)
public class NAT {
    @ISchemaField (name = "id", id = true, autoincrement = true)
    private Long id;

    @ISchemaField (name = "src_port")
    private Integer localPort;

    @ISchemaField (name = "dest_host")
    private String remoteHost;

    @ISchemaField (name = "dest_port")
    private Integer remotePort;

    @ISchemaField (name = "auto_bind")
    private boolean autoBind;

    public Long getId () {
        return id;
    }

    public void setId (Long id) {
        this.id = id;
    }

    public Integer getLocalPort () {
        return localPort;
    }

    public void setLocalPort (Integer localPort) {
        this.localPort = localPort;
    }

    public String getRemoteHost () {
        return remoteHost;
    }

    public void setRemoteHost (String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public Integer getRemotePort () {
        return remotePort;
    }

    public void setRemotePort (Integer remotePort) {
        this.remotePort = remotePort;
    }

    public boolean isAutoBind () {
        return autoBind;
    }

    public void setAutoBind (boolean autoBind) {
        this.autoBind = autoBind;
    }

    @Override
    public int hashCode () {
        return id == null ? 0 : id.hashCode ();
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass () != o.getClass ()) return false;
        NAT that = (NAT) o;
        return id != null && id.equals (that.getId ());
    }
}