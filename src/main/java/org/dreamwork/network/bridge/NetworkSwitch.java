package org.dreamwork.network.bridge;

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.server.SshServer;
import org.dreamwork.network.bridge.data.NAT;
import org.dreamwork.network.bridge.sshd.DatabaseAuthenticator;
import org.dreamwork.network.bridge.sshd.FileSystemHostKeyProvider;
import org.dreamwork.network.bridge.sshd.MainShellCommand;

import java.util.List;

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
        Configuration.load (null);
        Configuration.initDatabase ();

        // start the ssh server
        SshServer server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (9527);
        server.setPasswordAuthenticator (new DatabaseAuthenticator ());
        server.setKeyPairProvider (new FileSystemHostKeyProvider ());
        server.setShellFactory (channel -> new MainShellCommand ());
        server.start ();

        // bind the NATs
        {
            List<NAT> list = Context.db.get (NAT.class, "auto_bind = ?", true);
            if (list != null && !list.isEmpty ()) {
                for (NAT nat : list) {
                    NetBridge.transform (nat);
                }
            }
        }

        synchronized (LOCKER) {
            LOCKER.wait ();
        }
    }
}