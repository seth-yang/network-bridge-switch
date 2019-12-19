package org.dreamwork.network.sshd.cmd;

import org.dreamwork.network.Context;
import org.dreamwork.network.sshd.data.Device;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

/**
 * Created by seth.yang on 2019/11/6
 */
public class AddDeviceCommand extends Command {
    private String name, user, password, host;
    private int port = 22;

    private String error;

    public AddDeviceCommand () {
        super ("add-device", null, "Add a new device into repository");
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void parse (String... options) {
        if (options.length == 0) {
            return;
        }

        String p, k, v;
        for (int i = 0; i < options.length; i++) {
            p = options [i];
            if (p.startsWith ("--")) {
                p = p.substring (2);
                int pos = p.indexOf ('=');
                k = p.substring (0, pos);
                v = p.substring (pos + 1);
            } else {
                k = p.substring (1);
                v = options [++ i];
            }
            switch (k) {
                case "n":
                case "name":
                    name = v;
                    break;
                case "h":
                case "host":
                    host = v;
                    break;
                case "P":
                case "password":
                    password = v;
                    break;
                case "p":
                case "port":
                    try {
                        port = Integer.parseInt (v);
                    } catch (Exception ex) {
                        error = "invalid port: " + v;
                    }
                    break;
                case "u":
                case "user":
                    user = v;
                    break;
                default:
                    error = "unknown option: " + k;
                    return;
            }
        }
    }

    @Override
    public void perform (Console console) throws IOException {
        if (!StringUtil.isEmpty (error)) {
            console.errorln (error);
            error = null;
        }

        if (StringUtil.isEmpty (name) || StringUtil.isEmpty (user) || StringUtil.isEmpty (host)) {
            console.errorln ("Invalid parameter.");
            showHelp (console);
            return;
        }

        while (StringUtil.isEmpty (password)) {
            console.write ("please input password: ");
            password = console.readInput (true);
        }

        Device device = new Device ();
        device.setHost (host);
        device.setName (name);
        device.setPassword (password);
        device.setUser (user);
        device.setPort (port);
        Context.db.save (device);
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("add-device <options>");
        console.println ("where options are: ");
        console.println ("    -h    --host=<ip or hostname or domain name>");
        console.println ("    -n    --name=<device name>");
        console.println ("    -p    --port=<port>");
        console.println ("    -P    --password=<password>");
        console.println ("    -u    --user=<user name>");
        console.println ();
        console.println ("for example: ");
        console.println ("add-device -n test-server -h 192.168.1.1 -u root -p");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }
}
