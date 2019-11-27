package org.dreamwork.network.bridge;

import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.bridge.data.Schema;
import org.dreamwork.network.bridge.data.User;
import org.dreamwork.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by seth.yang on 2019/11/13
 */
class Configuration {
    private static final Properties props = new Properties ();

    static void load (String path) throws IOException {
        if (!StringUtil.isEmpty (path)) {
            Path p = Paths.get (path);
            if (Files.exists (p) && Files.isRegularFile (p) && Files.isReadable (p)) {
                try (InputStream in = Files.newInputStream (Paths.get (path))) {
                    props.load (in);
                }
            }
        }
    }

    static void initDatabase () throws SQLException, NoSuchAlgorithmException {
        String file;
        if (props.containsKey ("database.file")) {
            file = props.getProperty ("database.file");
        } else {
            file = "./bridge.db";
        }

        SQLite sqlite = SQLite.get (file);
        Schema.registerAllSchemas ();
        if (!sqlite.isTablePresent ("t_device")) {
            sqlite.createSchemas ();
            User user = new User ();
            user.setUserName ("root");
            user.setPassword (StringUtil.dump (AlgorithmUtil.md5 ("123456".getBytes ())).toLowerCase ());
            sqlite.save (user);
        }
        Context.db = sqlite;
    }
}