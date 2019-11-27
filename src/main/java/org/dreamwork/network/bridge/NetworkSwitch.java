package org.dreamwork.network.bridge;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.server.SshServer;
import org.dreamwork.cli.ArgumentParser;
import org.dreamwork.network.bridge.data.NAT;
import org.dreamwork.network.bridge.sshd.DatabaseAuthenticator;
import org.dreamwork.network.bridge.sshd.FileSystemHostKeyProvider;
import org.dreamwork.network.bridge.sshd.MainShellCommand;
import org.dreamwork.util.IOUtil;
import org.dreamwork.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * Created by seth.yang on 2019/10/28
 */
public class NetworkSwitch {
    private static final Object LOCKER = new byte[0];
    public static final IoSession[] CACHE = new IoSession[1];

    public static void main (String[] args) throws Exception {
        ClassLoader loader = NetworkSwitch.class.getClassLoader ();
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
        initLogger (loader, parser);

        new NetworkSwitch ().start (parser);
    }

    private void start (ArgumentParser parser) throws Exception {
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

    private static void initLogger (ClassLoader loader, ArgumentParser parser) throws IOException {
        String logLevel, logFile;
        if (parser.isArgPresent ('v')) {
            logLevel = "TRACE";
        } else if (parser.isArgPresent ("log-level")) {
            logLevel = parser.getValue ("log-level");
        } else {
            logLevel = parser.getDefaultValue ("log-level");
        }

        logFile = parser.getValue ("log-file");
        if (StringUtil.isEmpty (logFile)) {
            logFile = parser.getDefaultValue ("log-file");
        }
        File file = new File (logFile);
        File parent = file.getParentFile ();
        if (!parent.exists () && !parent.mkdirs ()) {
            throw new IOException ("Can't create dir: " + parent.getCanonicalPath ());
        }

        try (InputStream in = loader.getResourceAsStream ("internal-log4j.properties")) {
            Properties props = new Properties ();
            props.load (in);

            System.out.println ("### setting log level to " + logLevel + " ###");
            if ("trace".equalsIgnoreCase (logLevel)) {
                props.setProperty ("log4j.rootLogger", "INFO, stdout, FILE");
                props.setProperty ("log4j.appender.FILE.File", logFile);
                props.setProperty ("log4j.appender.FILE.Threshold", logLevel);
                props.setProperty ("log4j.logger.org.dreamwork", "trace");
                props.setProperty ("log4j.logger.com.hothink", "trace");
            } else {
                props.setProperty ("log4j.rootLogger", logLevel + ", stdout, FILE");
                props.setProperty ("log4j.appender.FILE.File", logFile);
                props.setProperty ("log4j.appender.FILE.Threshold", logLevel);
            }

            PropertyConfigurator.configure (props);
        }
    }
}