package org.dreamwork.network.bridge;

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.server.SshServer;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.bridge.data.Schema;
import org.dreamwork.network.bridge.data.User;
import org.dreamwork.network.bridge.sshd.DatabaseAuthenticator;
import org.dreamwork.network.bridge.sshd.FileSystemHostKeyProvider;
import org.dreamwork.network.bridge.sshd.MainShellCommand;
import org.dreamwork.util.StringUtil;

/**
 * Created by seth.yang on 2019/10/28
 */
public class NetworkSwitch {
    private static final Object LOCKER = new byte[0];
    public static final IoSession[] CACHE = new IoSession[1];

    public static void main (String[] args) throws Exception {
        new NetworkSwitch ().start ();
    }

    private void start () throws Exception {
        {
            SQLite sqlite = SQLite.get ("./bridge.db");
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

        SshServer server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (9527);
        server.setPasswordAuthenticator (new DatabaseAuthenticator ());
        server.setKeyPairProvider (new FileSystemHostKeyProvider ());
        server.setShellFactory (channel -> new MainShellCommand ());
        server.start ();

        synchronized (LOCKER) {
            LOCKER.wait ();
        }
    }
}