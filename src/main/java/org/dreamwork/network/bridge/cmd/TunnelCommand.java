package org.dreamwork.network.bridge.cmd;

import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

/**
 * Created by seth.yang on 2019/11/25
 */
public class TunnelCommand extends Command {
    private String action = "list", message;
    private int    index  = 0;

    public TunnelCommand () {
        super ("tunnel", null, "tunnel manager command");
    }

    @Override
    public void parse (String... options) {
        if (options.length > 0) {
            action = options[0];
        }

        if (options.length > 1) {
            String s_index = options[1];
            try {
                index = Integer.parseInt (s_index);
            } catch (Exception ex) {
                message = "invalid number format: " + s_index;
            }
        }

        if ("-h".equals (action) || "--help".equals (action)) {
            action = "help";
        } else if ("conn".equals (action)) {
            action = "connect";
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
                case "connect":
                    break;
                case "close":
                    break;
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
                "tunnel conn[ect] <index>  connect to a tunnel\r\n" +
                "tunnel close <index>      disconnect from a connected tunnel");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }

    private void resetOptions () {
        action  = "list";
        message = null;
        index   = 0;
    }

    private void list (Console console) {

    }
}