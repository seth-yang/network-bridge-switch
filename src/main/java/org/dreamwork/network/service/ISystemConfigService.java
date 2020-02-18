package org.dreamwork.network.service;

import org.dreamwork.network.sshd.data.SystemConfig;

public interface ISystemConfigService extends IService {
    default void write (String key, Object value) {
        write (key, value, false);
    }
    void write (String key, Object value, boolean createOnMissing);
    SystemConfig get (String key);

    default String getValue (String key) {
        SystemConfig item = get (key);
        return item == null ? null : item.getValue ();
    }

    default String getMergedValue (String key, String defaultValue) {
        SystemConfig item = get (key);
        if (item == null) {
            write (key, defaultValue, true);
            return defaultValue;
        } else {
            return item.getValue ();
        }
    }

    int getValue (String key, int defaultValue);

    int getMergedValue (String key, int defaultValue);
}
