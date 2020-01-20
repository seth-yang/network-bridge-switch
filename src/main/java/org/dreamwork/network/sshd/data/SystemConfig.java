package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.SystemConfigSchema;
import org.dreamwork.persistence.ISchema;
import org.dreamwork.persistence.ISchemaField;

import java.util.Objects;

/**
 * Created by seth.yang on 2019/12/24
 */
@ISchema (SystemConfigSchema.class)
public class SystemConfig {
    @ISchemaField (name = "id", id = true)
    private String id;

    @ISchemaField (name = "_value")
    private String value;

    @ISchemaField (name = "editable")
    private Boolean editable;

    public String getId () {
        return id;
    }

    public void setId (String id) {
        this.id = id;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
    }

    public boolean isEditable () {
        return editable == null ? false : editable;
    }

    public void setEditable (Boolean editable) {
        this.editable = editable;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass () != o.getClass ()) return false;
        SystemConfig that = (SystemConfig) o;
        return id != null && id.equals (that.getId ());
    }

    @Override
    public int hashCode () {
        return Objects.hash (id);
    }
}