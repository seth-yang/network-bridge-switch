package org.dreamwork.network;

import org.dreamwork.app.bootloader.ApplicationBootloader;
import org.dreamwork.app.bootloader.IBootable;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.IDatabase;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.Base64;
import org.dreamwork.network.bridge.NetBridge;
import org.dreamwork.network.cert.KeyTool;
import org.dreamwork.network.sshd.Sshd;
import org.dreamwork.network.sshd.cmd.NatCommand;
import org.dreamwork.network.sshd.cmd.SystemConfigCommand;
import org.dreamwork.network.sshd.cmd.TunnelCommand;
import org.dreamwork.network.sshd.data.NAT;
import org.dreamwork.network.sshd.data.Schema;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * Created by seth.yang on 2019/10/28
 */
@IBootable (argumentDef = "network-bridge.json")
public class NetworkSwitch {
    private static final String CFG_DB_FILE = "database.file";
    private static final Object LOCKER = new byte[0];

    private Logger logger;
//    private IConfiguration conf;

    public static void main (String[] args) throws InvocationTargetException {
        ApplicationBootloader.run (NetworkSwitch.class, args);
    }

    public NetworkSwitch () {
        logger = LoggerFactory.getLogger (NetworkSwitch.class);
    }

    public void start (IConfiguration conf) throws Exception {
//        this.conf = conf;
        IDatabase database = createDatabase (conf.getString (CFG_DB_FILE));

        // register all schema
        Schema.registerAllSchemas ();

        // check weather the required schema presented in the database or not
        if (!database.isTablePresent (SystemConfig.class)) {
            // tables are not created
            database.createSchemas ();

            // generate the ca key-pair
            initRootCA (database);
        }

        // create a sshd server
        Sshd sshd = new Sshd (conf);
        sshd.init (database);
        sshd.registerCommands (
                new SystemConfigCommand (database),
                new TunnelCommand (),   // tunnel manage command
                new NatCommand ()       // nat manage command
        ).bind ();

        // bind the NATs
        {
            List<NAT> list = database.get (NAT.class, "auto_bind = ?", true);
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

    private IDatabase createDatabase (String file) throws IOException {
        File db  = new File (file);
        File dir = db.getParentFile ();
        if (logger.isTraceEnabled ()) {
            logger.trace ("trying to create/get the database from: {}", db.getCanonicalPath ());
        }
        if (!dir.exists () && !dir.mkdirs ()) {
            logger.error ("can't create dir: {}", dir.getCanonicalPath ());
            throw new IOException ("can't create dir: " + dir.getCanonicalPath ());
        }
        return SQLite.get (db.getCanonicalPath ());
    }

    private void initRootCA (IDatabase database) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("creating root CA");
        }
        KeyPair pair = KeyTool.createKeyPair ();
        PrivateKey pri = pair.getPrivate ();
        PublicKey pub = pair.getPublic ();

        SystemConfig sc_private_key = new SystemConfig ();
        sc_private_key.setId (Keys.SYS_CONFIG.CFG_PRIMARY_KEY);
        sc_private_key.setValue (new String (Base64.encode (pri.getEncoded ())));
        sc_private_key.setEditable (false);

        SystemConfig sc_public_key  = new SystemConfig ();
        sc_public_key.setId (Keys.SYS_CONFIG.CFG_PUBLIC_KEY);
        sc_public_key.setValue (new String (Base64.encode (pub.getEncoded ())));
        sc_public_key.setEditable (false);

        database.save (Arrays.asList (sc_private_key, sc_public_key));
    }
}