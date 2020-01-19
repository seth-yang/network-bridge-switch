package org.dreamwork.network;

import org.dreamwork.app.bootloader.ApplicationBootloader;
import org.dreamwork.app.bootloader.IBootable;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.SQLite;
import org.dreamwork.network.bridge.NetBridge;
import org.dreamwork.network.sshd.Sshd;
import org.dreamwork.network.sshd.cmd.NatCommand;
import org.dreamwork.network.sshd.cmd.TunnelCommand;
import org.dreamwork.network.sshd.data.NAT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by seth.yang on 2019/10/28
 */
@IBootable (argumentDef = "network-bridge.json")
public class NetworkSwitch {
    private static final Object LOCKER = new byte[0];

    private Logger logger;

    public static void main (String[] args) throws InvocationTargetException {
        ApplicationBootloader.run (NetworkSwitch.class, args);
    }

    public NetworkSwitch () {
        logger = LoggerFactory.getLogger (NetworkSwitch.class);
    }

    public void start (IConfiguration conf) throws Exception {
        // start the ssh server
        Sshd sshd = new Sshd ();
        sshd.setConfiguration (conf);
        SQLite sqlite = sshd.initDatabase ();
        sshd.registerCommands (
                new TunnelCommand (),   // tunnel manage command
                new NatCommand ()       // nat manage command
        ).bind ();

        // bind the NATs
        {
            List<NAT> list = sqlite.get (NAT.class, "auto_bind = ?", true);
            if (list != null && !list.isEmpty ()) {
                for (NAT nat : list) {
                    NetBridge.transform (nat);
                }
            }
        }

        logger.info ("sshd server started.");
        synchronized (LOCKER) {
            LOCKER.wait ();
        }
    }
}