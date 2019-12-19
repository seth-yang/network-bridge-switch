package org.dreamwork.network;

import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.SQLite;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by seth.yang on 2019/11/6
 */
public class Context {
    public static SQLite db;
    public static IConfiguration conf;

    public static Map<String, IConfiguration> configs = new HashMap<> ();
}
