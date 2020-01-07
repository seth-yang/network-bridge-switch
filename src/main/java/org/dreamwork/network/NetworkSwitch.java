package org.dreamwork.network;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.server.SshServer;
import org.dreamwork.app.bootloader.ApplicationBootloader;
import org.dreamwork.app.bootloader.IBootable;
import org.dreamwork.cli.ArgumentParser;
import org.dreamwork.cli.CommandLineHelper;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.config.PropertyConfiguration;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.misc.Base64;
import org.dreamwork.network.bridge.NetBridge;
import org.dreamwork.network.cert.KeyTool;
import org.dreamwork.network.sshd.data.NAT;
import org.dreamwork.network.sshd.data.Schema;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.network.sshd.DatabaseAuthenticator;
import org.dreamwork.network.sshd.FileSystemHostKeyProvider;
import org.dreamwork.network.sshd.MainShellCommand;
import org.dreamwork.util.FileInfo;
import org.dreamwork.util.IOUtil;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.dreamwork.network.Keys.*;

/**
 * Created by seth.yang on 2019/10/28
 */
@IBootable (argumentDef = "network-bridge.json")
public class NetworkSwitch {
    private static final Object LOCKER = new byte[0];
    public static final IoSession[] CACHE = new IoSession[1];

    private Logger logger;

    public static void main (String[] args) throws Exception {
/*
        ClassLoader loader = NetworkSwitch.class.getClassLoader ();
        IConfiguration conf;
        {
            ArgumentParser parser;
            try (InputStream in = loader.getResourceAsStream ("network-bridge.json")) {
                if (in != null) {
                    byte[] buff = IOUtil.read (in);
                    String content = new String (buff, StandardCharsets.UTF_8);
                    parser = new ArgumentParser (content);
                } else {
                    throw new IllegalArgumentException ("can not find command schema!");
                }
            }

            parser.parse (args);
            String configFile = null;
            if (parser.isArgPresent ('c')) {
                configFile = parser.getValue ('c');
            }
            if (StringUtil.isEmpty (configFile)) {
                configFile = parser.getDefaultValue ('c');
            }
            if (StringUtil.isEmpty (configFile)) {
                configFile = "../conf/network-bridge.conf";
            }

            Properties props;
            if (Files.exists (Paths.get (configFile))) {
                props = CommandLineHelper.parseConfig (configFile);
            } else {
                System.err.println ("Cant find config file: " + configFile + ", using default settings.");
                props = new Properties ();
            }
            PropertyConfiguration pc = new PropertyConfiguration (props);
            merge (pc, parser);
            Context.conf = conf = pc;

            if (parser.isArgPresent ('v') || "trace".equalsIgnoreCase (pc.getString (CFG_LOG_LEVEL))) {
                pc.prettyShow ();
            }
        }

        String logFile  = conf.getString (CFG_LOG_FILE);
        String logLevel = conf.getString (CFG_LOG_LEVEL);
        Properties props = CommandLineHelper.initLogger (loader, logLevel, logFile, "org.dreamwork");
        PropertyConfigurator.configure (props);
*/
        ApplicationBootloader.run (NetworkSwitch.class, args);
//        new NetworkSwitch ().start (conf);
    }

    public NetworkSwitch () {
        logger = LoggerFactory.getLogger (NetworkSwitch.class);
    }

    public void start (IConfiguration conf) throws Exception {
        if (logger.isTraceEnabled ()) {
            logger.trace ("initialing the database ...");
        }
        initDatabase (conf.getString (CFG_DB_FILE));
        if (logger.isTraceEnabled ()) {
            logger.trace ("the database initialed.\r\n");
            logger.trace ("starting the sshd server ...");
        }

/*
        String ext = conf.getString (CFG_EXT_DIR);
        if (Files.exists (Paths.get (ext))) {
            Files.list (Paths.get (ext))
                    .filter (path -> path.toString ().endsWith (".conf"))
                    .forEach (path -> {
                        Properties props = new Properties ();
                        try (InputStream in = Files.newInputStream (path)) {
                            props.load (in);
                            PropertyConfiguration pc = new PropertyConfiguration (props);
                            String name = path.getFileName ().toString ();
                            name = FileInfo.getFileNameWithoutExtension (name);
                            Context.configs.put (name, pc);
                        } catch (IOException ex) {
                            ex.printStackTrace ();
                        }
                    });
        }
*/

        // start the ssh server
        int port = conf.getInt (CFG_SSHD_PORT, 9527);
        SshServer server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (port);
        server.setPasswordAuthenticator (new DatabaseAuthenticator ());
        server.setKeyPairProvider (new FileSystemHostKeyProvider (conf.getString (CFG_SSHD_CA_DIR)));
        server.setShellFactory (channel -> new MainShellCommand ());
        server.start ();

        if (logger.isTraceEnabled ()) {
            logger.trace ("sshd server listen on {}:{}", server.getHost (), port);
        }

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

/*
    private static void merge (PropertyConfiguration conf, ArgumentParser parser) {
        if (parser.isArgPresent ('e')) {
            String ext_dir = parser.getValue ('e');
            conf.setRawProperty (CFG_EXT_DIR, ext_dir);
        }
        if (!conf.contains (CFG_EXT_DIR)) {
            conf.setRawProperty (CFG_EXT_DIR, parser.getDefaultValue ('e'));
        }

        if (parser.isArgPresent ('d')) {
            String db_file = parser.getValue ('d');
            conf.setRawProperty (CFG_DB_FILE, db_file);
        }
        if (!conf.contains (CFG_DB_FILE)) {
            conf.setRawProperty (CFG_DB_FILE, parser.getDefaultValue ('d'));
        }

        if (parser.isArgPresent ("log-file")) {
            conf.setRawProperty (CFG_LOG_FILE, parser.getValue ("log-file"));
        }
        if (!conf.contains (CFG_LOG_FILE)) {
            conf.setRawProperty (CFG_LOG_FILE, parser.getDefaultValue ("log-file"));
        }

        if (parser.isArgPresent ('v')) {
            conf.setRawProperty (CFG_LOG_LEVEL, "trace");
        } else if (parser.isArgPresent ("log-level")) {
            conf.setRawProperty (CFG_LOG_LEVEL, parser.getValue ("log-level"));
        }
        if (!conf.contains (CFG_LOG_LEVEL)) {
            conf.setRawProperty (CFG_LOG_LEVEL, parser.getDefaultValue ("log-level"));
        }

        if (parser.isArgPresent ('p')) {
            conf.setRawProperty (CFG_SSHD_PORT, parser.getValue ('p'));
        }
        if (!conf.contains (CFG_SSHD_PORT)) {
            conf.setRawProperty (CFG_SSHD_PORT, parser.getDefaultValue ('p'));
        }

        if (parser.isArgPresent ("ca-dir")) {
            conf.setRawProperty (CFG_SSHD_CA_DIR, parser.getValue ("ca-dir"));
        }
        if (!conf.contains (CFG_SSHD_CA_DIR)) {
            conf.setRawProperty (CFG_SSHD_CA_DIR, parser.getDefaultValue ("ca-dir"));
        }
    }
*/

    private void initDatabase (String file) throws SQLException, NoSuchAlgorithmException, IOException {
        File db  = new File (file);
        File dir = db.getParentFile ();
        if (logger.isTraceEnabled ()) {
            logger.trace ("trying to create/get the database from: {}", db.getCanonicalPath ());
        }
        if (!dir.exists () && !dir.mkdirs ()) {
            logger.error ("can't create dir: {}", dir.getCanonicalPath ());
            System.exit (-1);
            return;
        }
        SQLite sqlite = SQLite.get (file);
        Schema.registerAllSchemas ();
        if (!sqlite.isTablePresent ("t_device")) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("creating tables ...");
            }
            sqlite.createSchemas ();

            if (logger.isTraceEnabled ()) {
                logger.trace ("creating root user ...");
            }
            User user = new User ();
            user.setUserName ("root");
            user.setPassword (StringUtil.dump (AlgorithmUtil.md5 ("123456".getBytes ())).toLowerCase ());
            sqlite.save (user);

            initRootCA (sqlite);
        }
        Context.db = sqlite;
    }

    private void initRootCA (SQLite sqlite) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("creating root CA");
        }
        KeyPair pair = KeyTool.createKeyPair ();
        PrivateKey pri = pair.getPrivate ();
        PublicKey  pub = pair.getPublic ();

        SystemConfig sc_private_key = new SystemConfig ();
        sc_private_key.setId (SYS_CONFIG.CFG_PRIMARY_KEY);
        sc_private_key.setValue (new String (Base64.encode (pri.getEncoded ())));

        SystemConfig sc_public_key  = new SystemConfig ();
        sc_public_key.setId (SYS_CONFIG.CFG_PUBLIC_KEY);
        sc_public_key.setValue (new String (Base64.encode (pub.getEncoded ())));

        sqlite.save (Arrays.asList (sc_private_key, sc_public_key));
    }
}