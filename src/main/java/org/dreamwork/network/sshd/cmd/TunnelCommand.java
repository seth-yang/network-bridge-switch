package org.dreamwork.network.sshd.cmd;

import org.dreamwork.cli.text.Alignment;
import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.config.IConfiguration;
import org.dreamwork.db.IDatabase;
import org.dreamwork.network.Context;
import org.dreamwork.network.bridge.tunnel.Client;
import org.dreamwork.network.bridge.tunnel.TunnelManager;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.dreamwork.network.sshd.data.TunnelClient;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;
import java.util.List;

import static org.dreamwork.network.Keys.SYS_CONFIG.*;
import static org.dreamwork.network.service.impls.SystemConfigServiceImpl.getPort;

/**
 * Created by seth.yang on 2019/11/25
 */
public class TunnelCommand extends Command {
    private String action = "list", message;
    private String name, value;
    private IDatabase database;

    public TunnelCommand (IDatabase database) {
        super ("tunnel", null, "tunnel manager command");
        this.database = database;
    }

    @Override
    public void parse (String... options) {
        if (options.length > 0) {
            action = options[0];
        }

        if ("-h".equals (action) || "--help".equals (action)) {
            action = "help";
        }

        if ("auth".equals (action)) {
            if (options.length > 1) {
                name = options [1];
            } else {
                message = "name is missing";
            }
        } else if ("auto-start".equals (action)) {
            if (options.length > 1) {
                value = options [1];
            } else {
                value = "show";
            }
        }
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void perform (Console console) throws IOException {
        if (!StringUtil.isEmpty (message)) {
            console.errorln (message);
            return;
        }

        try {
            switch (action) {
                case "help":
                    showHelp (console);
                    break;
                case "list":
                    list (console);
                    break;
                case "auth":
                    auth (console);
                    break;
                case "start":
                    if (!TunnelManager.isRunning ()) {
                        IConfiguration conf = Context.getConfiguration ("tunnel");
                        int manage_port = getPort (database, MANAGE_PORT, conf.getInt (MANAGE_PORT, 50041));
                        int connector_port = getPort (database, CONNECTOR_PORT, conf.getInt (CONNECTOR_PORT, 50042));
                        TunnelManager.start (manage_port, connector_port);
                        console.println ("tunnel manager start success.");
                    } else {
                        console.println ("tunnel manager already started.");
                    }
                    break;
                case "stop":
                    TunnelManager.stop ();
                    break;
/*
                case "allow":
                    break;
*/
                case "state":
                    if (TunnelManager.isRunning ()) {
                        console.setForegroundColor (TerminalIO.GREEN);
                        console.println ("    running");
                    } else {
                        console.setForegroundColor (TerminalIO.YELLOW);
                        console.println ("    not running");
                    }
                    break;

                case "auto-start":
                    {
                        SystemConfig item = database.getByPK (SystemConfig.class, CFG_TUNNEL_START);
                        boolean create = false;
                        switch (value) {
                            case "on":
                            case "true":
                                if (item != null) {
                                    item.setValue ("true");
                                } else {
                                    item = new SystemConfig ();
                                    item.setId (CFG_TUNNEL_START);
                                    item.setValue ("true");
                                    item.setEditable (true);
                                    create = true;
                                }
                                break;
                            case "off":
                            case "false":
                                if (item != null) {
                                    item.setValue ("false");
                                } else {
                                    item = new SystemConfig ();
                                    item.setId (CFG_TUNNEL_START);
                                    item.setValue ("false");
                                    item.setEditable (true);
                                    create = true;
                                }
                                break;
                            default:
                                console.errorln ("invalid value: " + value);
                                return;
                        }

                        if (create) {
                            database.save (item);
                        } else {
                            database.update (item);
                        }
                    }
                    break;
/*
                case "deny":
                    break;
*/
                default:
                    console.errorln ("invalid command: " + action + ", type tunnel --help for details");
                    break;
            }
        } finally {
            resetOptions ();
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println (
                "tunnel [command] [cmd-options]\r\n" +
                "\r\n" +
                "tunnel -h|--help          show this help list\r\n" +
                "tunnel [list]             show all active tunnels\r\n" +
                "tunnel auth <name>        authenticate a client and generate a unique key for it\r\n" +
                "tunnel start              start the tunnel service\r\n" +
                "tunnel stop               stop the tunnel service\r\n" +
                "tunnel state              shows the tunnel manage state"
/*
                "tunnel allow <port>       allow creating a tunnel on port\r\n" +
                "tunnel deny <port>        deny a port to create tunnel"
*/
        );
        console.setForegroundColor (TerminalIO.COLORINIT);
    }

    private void resetOptions () {
        action  = "list";
        message = null;
        name    = null;
        value   = null;
//        port    = 0;
    }

    private void list (Console console) throws IOException {
        String[] HEADER  = {"name", "Port", "Blocking"};
        int[] width = {HEADER[0].length (), HEADER[1].length ()};
        List<Client> clients = TunnelManager.getClients ();
        for (Client c : clients) {
            if (width[0] < c.name.length ()) {
                width[0] = c.name.length ();
            }
            String tmp = String.valueOf (c.port);
            if (width[1] < tmp.length ()) {
                width[1] = tmp.length ();
            }
        }

        console.write (TextFormater.fill (HEADER[0], ' ', width[0], Alignment.Left));
        console.write ("  ");
        console.write (TextFormater.fill (HEADER[1], ' ', width[1], Alignment.Right));
        console.write ("  ");
        console.println (HEADER[2]);

        for (Client c : clients) {
            console.write (TextFormater.fill (c.name, ' ', width[0], Alignment.Left));
            console.write ("  ");
            console.write (TextFormater.fill (String.valueOf (c.port), ' ', width[1], Alignment.Right));
            console.write ("  ");
            console.println (c.blocked ? "Y" : "");
        }
    }

    private void auth (Console console) throws IOException {
        String password;
        int count = 0;
        do {
            password = console.readPassword ();
            if (!StringUtil.isEmpty (password) && password.trim ().length () >= 6) {
                break;
            }
        } while (count ++ < 3);

        if (StringUtil.isEmpty (password)) {
            console.errorln ("no client will be authenticated without password");
            return;
        }

        TunnelClient tc = new TunnelClient ();
        tc.setName (name);
        tc.setPassword (password);

    }
}