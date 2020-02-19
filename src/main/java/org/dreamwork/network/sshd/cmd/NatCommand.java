package org.dreamwork.network.sshd.cmd;

import org.dreamwork.cli.text.Alignment;
import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.db.IDatabase;
import org.dreamwork.network.bridge.NetBridge;
import org.dreamwork.network.sshd.data.NAT;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <ul>
 *     <li>nat [print]</li>
 *     <li>nat add -l &lt;local-port&gt; -h &lt;remote-host&gt; -t &lt;remote-port&gt;</li>
 *     <li>nat del -l &lt;local-port&gt;</li>
 *     <li>nat active -l &lt;local-port&gt;</li>
 *     <li>nat shutdown -l &lt;local-port&gt;</li>
 *     <li>nat -h|--help</li>
 * </ul>
 * <p>Created by seth.yang on 2019/11/13</p>
 */
public class NatCommand extends Command {
    private static final String[] HEADERS = { "Local Port", "Remote Address", "Auto Bind", "Bound" };
    private static final String[] VALID_COMMANDS = {
            "add", "del", "print", "enable", "disable", "bind", "unbind", "help"
    };

    private String action = "print";
    private String host;
    private int localPort = -1, remotePort = -1;
    private boolean auto_bind = false;
    private String message = null;
    private IDatabase database;

    private final Logger logger = LoggerFactory.getLogger (NatCommand.class);

    public NatCommand (IDatabase database) {
        super("nat", null, "build or print NAT rules");
        this.database = database;
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void parse (String... options) {
        if (options.length > 0) {
            action = options [0];

            if (action.startsWith ("-")) {
                action = "help";
            }
        }

        boolean found = false;
        for (String c : VALID_COMMANDS) {
            if (c.equals (action)) {
                found = true;
                break;
            }
        }

        if (!found) {
            message = "Invalid command: " + action;
            return;
        }

        if (options.length > 1) {
            for (int i = 1; i < options.length; i ++) {
                String p = options [i];
                switch (p) {
                    case "-l" :
                        localPort = parseInteger (options [++ i]);
                        break;
                    case "-h":
                        host = options [++ i].trim ();
                        break;
                    case "-t":
                        remotePort = parseInteger (options [++ i]);
                        break;
                    case "--auto-bind":
                        auto_bind = true;
                        break;
                    default:
                        if ("add".equals (action)) {
                            message = "Invalid option: " + p;
                            return;
                        } else {
                            localPort = parseInteger (p);
                            break;
                        }
                }
            }
        }
    }

    private int parseInteger (String value) {
        try {
            return Integer.parseInt (value.trim ());
        } catch (Exception ex) {
            message = "Invalid port number";
            return -1;
        }
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
            if (!StringUtil.isEmpty (message)) {
                console.errorln (message);
                return;
            }

            if ("help".equals (action)) {
                showHelp (console);
            } else if ("print".equals (action)) {
                printRules (console);
            } else if (checkLocalPort (console)) {
                switch (action) {
                    case "add":
                        add (console);
                        break;
                    case "del":
                        delete (console);
                        break;
                    case "enable":
                        enable (console);
                        break;
                    case "disable":
                        disable (console);
                        break;
                    case "bind":
                        bind (console);
                        break;
                    case "unbind":
                        unbind (console);
                        break;
                    default:
                        console.errorln ("unknown action: " + action);
                        break;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else {
                throw new IOException (ex);
            }
        } finally {
            resetOptions ();
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("build or print NAT table");
        console.println ("nat [options] <command> [command-options]");
        console.println ("for add a new NAT rule: ");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat add -l <local-port> -h <remote-host> -t <remote-port> [--auto-bind]");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for delete an existed NAT rule:");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat del [-l] <local-port>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for active an existed NAT rule:");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat enable [-l] <local-port>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for de-active an existed NAT rule:");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat disable [-l] <local-port>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for bind a rule: ");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat bind [-l] <local-port>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for unbind a rule: ");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat unbind [-l] <local-port>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("for help: ");
        console.setForegroundColor (TerminalIO.CYAN);
        console.println ("  nat -h | --help");
        console.setForegroundColor (TerminalIO.COLORINIT);
        console.println ();
    }

    @Override
    public List<String> guess (String text) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("text = {}", text);
        }

        if ("nat".equals (text)) {
            List<String> list = Arrays.asList (VALID_COMMANDS);
            list.sort (String::compareTo);
            return list;
        }

        String[] tmp = TextFormater.parse (text);
        if (tmp.length > 1) {
            List<String> list = new ArrayList<> ();
            String action = tmp [1];
            for (String cmd : VALID_COMMANDS) {
                if (cmd.startsWith (action)) {
                    list.add (cmd);
                }
            }

            if (list.isEmpty ()) {
                // action 未匹配到，不改变当前输入
                return null;
            }

            if (list.size () == 1) {
                // 只有唯一匹配，看看是否完全相等
                boolean fullMatches = false;
                for (String cmd : VALID_COMMANDS) {
                    if (cmd.equals (action)) {
                        fullMatches = true;
                        break;
                    }
                }

                if (!fullMatches) {
                    // 不是完全匹配，返回列表用于补齐 action
                    return list;
                }
            } else {
                return list;
            }
        }

        return null;
/*
        String[] tmp = text.split ("\\s+");
        int index = 0;
        if ("nat".equals (tmp [0])) {
            index ++;
        }

        List<String> result = new ArrayList<> ();
        while (index < tmp.length) {
            String word = tmp [index ++];
            if (logger.isTraceEnabled ()) {
                logger.trace ("tmp [{}] = {}", index - 1, word);
            }

        }
        return result;
*/
    }

    private void resetOptions () {
        action      = "print";
        host        = null;
        localPort   = -1;
        remotePort  = -1;
        auto_bind   = false;
        message     = null;
    }

    private boolean checkLocalPort (Console console) throws IOException {
        if (localPort < 0) {
            console.errorln ("The local-port is missing!");
            return false;
        }
        if (localPort > 65535) {
            console.errorln ("Invalid port number");
            return false;
        }
        return true;
    }

    private void add (Console console) throws IOException {
        if (StringUtil.isEmpty (host)) {
            console.errorln ("remote address is missing");
            return;
        }

        if (remotePort < 0) {
            console.errorln ("remote port is missing");
            return;
        }

        if (remotePort > 65535) {
            console.errorln ("invalid remote port");
            return;
        }

        NAT nat = new NAT ();
        nat.setAutoBind (auto_bind);
        nat.setLocalPort (localPort);
        nat.setRemoteHost (host);
        nat.setRemotePort (remotePort);
        database.save (nat);
    }

    private void enable (Console console) throws IOException {
        changeAutoBind (console, true);
    }

    private void disable (Console console) throws IOException {
        changeAutoBind (console, false);
    }

    private void bind (Console console) throws IOException {
        if (NetBridge.isBound (localPort)) {
            console.errorln ("The rule: " + localPort + " already bound!");
        } else {
            NAT nat = get (console);
            if (nat != null) {
                NetBridge.transform (nat);
            }
        }
    }

    private void unbind (Console console) throws IOException {
        if (NetBridge.isBound (localPort)) {
            NetBridge.shutdown (localPort);
        } else {
            console.errorln ("The rule: " + localPort + " is not bound!");
        }
    }

    private void changeAutoBind (Console console, boolean autoBind) throws IOException {
        NAT nat= get (console);
        if (nat != null) {
            nat.setAutoBind (autoBind);
            database.update (nat);
        }
    }

    private void delete (Console console) throws IOException {
        NetBridge.shutdown (localPort);
        NAT nat = get (console);
        if (nat != null) {
            database.delete (nat);
        }
    }

    private NAT get (Console console) throws IOException {
        NAT nat = database.getSingle (NAT.class, "SELECT * FROM t_nat WHERE src_port = ?", localPort);
        if (nat == null) {
            console.errorln ("The NAT rule bound local port: " + localPort + " not found!");
        }
        return nat;
    }

    private void printRules (Console console) throws IOException {
        List<NAT> list = database.get (NAT.class);
        int[] w = new int[HEADERS.length];
        for (int i = 0; i < HEADERS.length; i ++) {
            w [i] = HEADERS[i].length ();
        }

        List<Wrapper> wrappers = new ArrayList<> (list.size ());
        for (NAT nat : list) {
            Wrapper wrapper = new Wrapper ();

            wrapper.port        = String.valueOf (nat.getLocalPort ());
            wrapper.remote      = nat.getRemoteHost () + ":" + nat.getRemotePort ();
            wrapper.auto_bind   = nat.isAutoBind () ? "Y" : "";
            wrapper.bound       = NetBridge.isBound (nat.getLocalPort ()) ? "Y" : "";

            if (w[1] < wrapper.remote.length ()) {
                w[1] = wrapper.remote.length ();
            }
            wrappers.add (wrapper);
        }

        int W = 0;
        console.write (TextFormater.fill (HEADERS [0], ' ', w[0], Alignment.Right)); W += w[0];
        console.write ("    ");                                                      W += 4;
        console.write (TextFormater.fill (HEADERS [1], ' ', w[1], Alignment.Left));  W += w[1];
        console.write ("    ");                                                      W += 4;
        console.write (TextFormater.fill (HEADERS [2], ' ', w[2], Alignment.Right)); W += w[2];
        console.write ("    ");                                                      W += 4;
        console.println (TextFormater.fill (HEADERS [3], ' ', w[3], Alignment.Right)); W += w[3];
        console.println (TextFormater.fill ("-", '-', W, Alignment.Left));
        for (Wrapper wrapper : wrappers) {
            console.write (TextFormater.fill (wrapper.port, ' ', w[0], Alignment.Right));
            console.write ("    ");
            console.write (TextFormater.fill (wrapper.remote, ' ', w[1], Alignment.Left));
            console.write ("    ");
            console.write (TextFormater.fill (wrapper.auto_bind, ' ', w[2], Alignment.Right));
            console.write ("    ");
            console.println (TextFormater.fill (wrapper.bound, ' ', w[3], Alignment.Right));
        }
    }

    private static final class Wrapper {
        String port, remote, auto_bind, bound;
    }
}