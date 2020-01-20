package org.dreamwork.network.sshd;

import org.apache.sshd.server.SshServer;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.SQLite;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.misc.Base64;
import org.dreamwork.network.Context;
import org.dreamwork.network.Keys;
import org.dreamwork.network.cert.KeyTool;
import org.dreamwork.network.sshd.cmd.PasswordCommand;
import org.dreamwork.network.sshd.cmd.SystemConfigCommand;
import org.dreamwork.network.sshd.cmd.UserCommand;
import org.dreamwork.network.sshd.data.Schema;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Arrays;

import static org.dreamwork.network.Keys.*;

public class Sshd {
    private static final Logger logger = LoggerFactory.getLogger (Sshd.class);
    private static final MainShellCommand shell = new MainShellCommand ();

    private IConfiguration conf;

    public Sshd () {
        shell.registerCommands (
                new PasswordCommand (),
                new UserCommand ()
        );
    }

    public void setConfiguration (IConfiguration conf) {
        this.conf = conf;
    }

    public SQLite initDatabase () throws NoSuchAlgorithmException, SQLException, IOException {
        if (logger.isTraceEnabled ()) {
            logger.trace ("initialing the database ...");
        }
        initDatabase (conf.getString (CFG_DB_FILE));
        if (logger.isTraceEnabled ()) {
            logger.trace ("the database initialed.");
        }

        shell.registerCommands (new SystemConfigCommand (Context.db));
        return Context.db;
    }

    public void bind () throws Exception {
        if (logger.isTraceEnabled ()) {
            logger.trace ("starting the sshd server ...");
        }

        int port = conf.getInt (CFG_SSHD_PORT, 9527);
        SshServer server = SshServer.setUpDefaultServer ();
        server.setHost ("0.0.0.0");
        server.setPort (port);
        server.setPasswordAuthenticator (new DatabaseAuthenticator ());
        server.setKeyPairProvider (new FileSystemHostKeyProvider (conf.getString (CFG_SSHD_CA_DIR)));
        server.setShellFactory (channel -> shell);
        server.start ();

        if (logger.isTraceEnabled ()) {
            logger.trace ("sshd server listen on {}:{}", server.getHost (), port);
        }
    }

    public SQLite getDatabase () {
        return Context.db;
    }

    public Sshd registerCommands (Command... commands) {
        shell.registerCommands (commands);
        return this;
    }

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
        if (logger.isDebugEnabled ()) {
            sqlite.setDebug (true);
        }
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
        PublicKey pub = pair.getPublic ();

        SystemConfig sc_private_key = new SystemConfig ();
        sc_private_key.setId (Keys.SYS_CONFIG.CFG_PRIMARY_KEY);
        sc_private_key.setValue (new String (Base64.encode (pri.getEncoded ())));
        sc_private_key.setEditable (false);

        SystemConfig sc_public_key  = new SystemConfig ();
        sc_public_key.setId (Keys.SYS_CONFIG.CFG_PUBLIC_KEY);
        sc_public_key.setValue (new String (Base64.encode (pub.getEncoded ())));
        sc_public_key.setEditable (false);

        sqlite.save (Arrays.asList (sc_private_key, sc_public_key));
    }
}
