package org.dreamwork.network.service.impls;

import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.IDatabase;
import org.dreamwork.network.Context;
import org.dreamwork.network.service.ISystemConfigService;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemConfigServiceImpl implements ISystemConfigService {
    private static final Logger logger = LoggerFactory.getLogger (SystemConfigServiceImpl.class);

    private IDatabase database;

    public SystemConfigServiceImpl (IDatabase database) {
        this.database = database;
    }

    public static int getPort (IDatabase database, String name, int defaultValue) {
//        Context.db
        SystemConfig item = database.getByPK (SystemConfig.class, name);
        if (item != null) {
            String value = item.getValue ();
            if (value == null) {
                item.setValue (String.valueOf (defaultValue));
                database.update (item);
                return defaultValue;
            } else {
                try {
                    return Integer.parseInt (value.trim ());
                } catch (Exception ex) {
                    logger.warn (ex.getMessage (), ex);
                    item.setValue (String.valueOf (defaultValue));
                    database.update (item);
                    return defaultValue;
                }
            }
        } else {
            IConfiguration conf = Context.getConfiguration ("tunnel");
            int port = conf.getInt (name, defaultValue);
            item = new SystemConfig ();
            item.setId (name);
            item.setValue (String.valueOf (port));
            item.setEditable (true);
            database.save (item);
            return port;
        }
    }

    @Override
    public void write (String key, Object value, boolean createOnMissing) {
        if (value == null) {
            // removing the config from database
            if (logger.isTraceEnabled ()) {
                logger.trace ("the value of {} is null, it means to remove it", key);
            }

            database.delete (SystemConfig.class, key);
        } else {
            SystemConfig item = get (key);
            if (item == null) {
                if (createOnMissing) {
                    item = new SystemConfig ();
                    item.setId (key);
                    item.setValue (String.valueOf (value));
                    item.setEditable (true);
                    database.save (item);
                } else {
                    logger.warn ("item {} not found.", key);
                }
            } else {
                item.setValue (String.valueOf (value));
                database.update (item);
            }
        }
    }

    @Override
    public SystemConfig get (String key) {
        return database.getByPK (SystemConfig.class, key);
    }

    @Override
    public int getValue (String key, int defaultValue) {
        return getIntValue (key, defaultValue, false);
    }

    @Override
    public int getMergedValue (String key, int defaultValue) {
        return getIntValue (key, defaultValue, true);
    }

    private int getIntValue (String key, int defaultValue, boolean create) {
        SystemConfig item = get (key);
        if (item == null) {
            if (create)
                write (key, String.valueOf (defaultValue), true);
        } else {
            String value = item.getValue ();
            try {
                return Integer.parseInt (value);
            } catch (Exception ex) {
                logger.warn (ex.getMessage (), ex);
                if (create)
                    write (key, String.valueOf (defaultValue), true);
            }
        }
        return defaultValue;
    }
}
