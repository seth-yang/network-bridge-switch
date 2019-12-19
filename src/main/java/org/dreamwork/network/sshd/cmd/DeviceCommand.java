package org.dreamwork.network.sshd.cmd;

import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.config.KeyValuePair;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

import static org.dreamwork.network.sshd.cmd.CommandUtil.readPassword;

/**
 * Created by seth.yang on 2019/11/14
 */
public class DeviceCommand extends Command {
    private String action = "print", message, name, host, user, password;
    private int port = -1;
    private boolean askForPassword;

    public DeviceCommand () {
        super ("device", "dev", "device management command.");
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void parse (String... options) {
        if (options.length > 0) {
            action = options [0];
            if ("-h".equals (action) || "--help".equals (action) || "-?".equals (action)) {
                action = "help";
                return;
            }

            if ("print".equals (action)) {
                return;
            }

            if (options.length > 1) {
                for (int i = 1; i < options.length; i ++) {
                    String option = options [i];
                    if (option.startsWith ("--")) {
                        if (option.contains ("=")) {
                            KeyValuePair<String> p = TextFormater.parseValue (String.class, option);
                            if (p != null) {
                                switch (p.getName ()) {
                                    case "name":
                                        name = p.getValue ();
                                        break;
                                    case "host":
                                        host = p.getValue ();
                                        break;
                                    case "user":
                                        user = p.getValue ();
                                        break;
                                    case "port":
                                        String s_port = p.getValue ();
                                        try {
                                            port = Integer.parseInt (s_port);
                                        } catch (Exception ex) {
                                            message = "Invalid port number: " + s_port;
                                            return;
                                        }
                                        break;
                                    default:
                                        message = "Invalid option: " + option;
                                        return;
                                }
                            }
                        } else if ("--password".equals (option)) {
                            askForPassword = true;
                        }
                    } else if (option.startsWith ("-")) {
                        switch (option) {
                            case "-n":
                                name = options [++ i];
                                break;
                            case "-p":
                                String s_port = options[++ i];
                                try {
                                    port = Integer.parseInt (s_port);
                                } catch (Exception ex) {
                                    message = "Invalid port number: " + s_port;
                                    return;
                                }
                                break;
                            case "-P":
                                askForPassword = true;
                                break;
                            case "-h":
                                host = options[++ i];
                                break;
                            case "-u":
                                user = options [++ i];
                                break;
                            default:
                                message = "Invalid option: " + option;
                                return;
                        }
                    } else if (!"add".equals (action)) {
                        name = option;
                    } else {
                        message = "Invalid option: " + option;
                        return;
                    }
                }
/*
                for (String option : options) {
                    switch (option) {
                        case "add":
                            break;
                        case "delete":
                        case "del":
                            break;
                        case "connect":
                        case "conn":
                            break;
                        default:
                            break;
                    }
                }
*/
            }
        }
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
            if (!StringUtil.isEmpty (message)) {
                console.errorln (message);
                return;
            }

            switch (action) {
                case "help":
                    showHelp (console);
                    break;
                case "add":
                    add (console);
                    break;
                case "del":
                case "delete":
                    console.println ("name = " + name);
                    break;
                case "conn":
                case "connect":
                    console.println ("name = " + name);
                    break;
                case "print":
                    console.println ("action = print");
                    break;
            }
        } finally {
            resetOptions ();
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("usage: device [options] [command] [command-options]\r\n" +
                "\r\n" +
                "options:\r\n" +
                "  -h    --help    show help list\r\n" +
                "\r\n" +
                "command:\r\n" +
                "  add       add a new device\r\n" +
                "  valid options:\r\n" +
                "    -n    --name        the device name will be shown\r\n" +
                "    -h    --host        the device host address\r\n" +
                "    -p    --port        the device ssh port, default to 22\r\n" +
                "    -u    --user        the user who will be post\r\n" +
                "    -P    --password    the passowrd of ssh user\r\n" +
                "  \r\n" +
                "  del       remove an exists device\r\n" +
                "  valid optoins:\r\n" +
                "    -n    --name        the device name\r\n" +
                "\r\n" +
                "  conn      connect to a device\r\n" +
                "  valid options:\r\n" +
                "    -n    --name        the device name\r\n" +
                "\r\n" +
                "  print     print the list of all devices\r\n" +
                "\r\n" +
                "for example:\r\n" +
                "  device add -n test-server --host=127.0.0.1 -u test_user -P\r\n" +
                "  device conn [-n] test-server\r\n" +
                "  dev del [-n] test-server\r\n" +
                "  device [print]");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }

    private void resetOptions () {
        action          = "print";
        message         = null;
        user            = null;
        host            = null;
        password        = null;
        askForPassword  = false;
        port            = -1;
        name            = null;
    }

    private void add (Console console) throws IOException {
        if (StringUtil.isEmpty (name)) {
            console.errorln ("name is missing");
            return;
        }

        if (port < 0) {
            console.errorln ("port is missing");
            return;
        }

        if (port > 65535) {
            console.errorln ("invalid port number: " + port);
            return;
        }

        if (StringUtil.isEmpty (host)) {
            console.errorln ("host is missing");
            return;
        }

        if (StringUtil.isEmpty (user)) {
            user = console.getEnv ("USER");
        }

        if (StringUtil.isEmpty (password)) {
            if (askForPassword) {
                password = readPassword (console);
            }
        }

        if (StringUtil.isEmpty (password)) {
            console.errorln ("password is missing");
            return;
        }

        console.println ("add -n " + name + " " + user + "." + password +"@" + host + ":" + port);
    }
}