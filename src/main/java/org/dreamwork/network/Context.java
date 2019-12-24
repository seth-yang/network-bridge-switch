package org.dreamwork.network;

import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.SQLite;
import org.dreamwork.network.sshd.data.SystemConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by seth.yang on 2019/11/6
 */
public class Context {
    public static SQLite db;
    public static IConfiguration conf;

    public static Map<String, IConfiguration> configs = new HashMap<> ();

    public static String getSystemConfig (String key) {
        if (db == null) {
            return null;
        }

        SystemConfig config = db.getByPK (SystemConfig.class, key);
        return config == null ? null : config.getValue ();
    }
}
