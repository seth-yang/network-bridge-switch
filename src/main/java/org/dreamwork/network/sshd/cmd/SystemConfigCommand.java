package org.dreamwork.network.sshd.cmd;

import org.dreamwork.cli.text.Alignment;
import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.db.IDatabase;
import org.dreamwork.network.sshd.data.SystemConfig;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemConfigCommand extends Command {
    private static final String[] VALID_COMMANDS = {"print", "set", "unset", "help"};

    private final Pattern P = Pattern.compile ("^(.*?)((\\s*=\\s*)|\\s*)(.*?)$");
    private final Logger logger = LoggerFactory.getLogger (SystemConfigCommand.class);
    private String action = "print";
    private String message = null;
    private String id, value;

    private IDatabase sqlite;

    public SystemConfigCommand (IDatabase sqlite) {
        super ("sys-config", null, "system config manage");
        this.sqlite = sqlite;
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void parse (String... options) {
        for (String opt : options) {
            if ("-h".equals (opt) || "--help".equals (opt)) {
                action = "help";
                return;
            }
        }

        if (options.length > 0) {
            action = options [0];
        }

        switch (action) {
            case "print" :
                if (options.length > 1) {
                    id = options [1];
                }
                break;
            case "set":
                if (options.length != 3) {
                    message = "invalid parameter, type `help` for command details";
                    return;
                }
                id = options [1];
                value = options [2];
                break;
            case "unset":
                if (options.length != 2) {
                    message = "parameter [id] is missing";
                    return;
                }
                id = options [1];
                break;
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
                case "print":
                    print (console);
                    break;
                case "set": {
                    SystemConfig config = sqlite.getByPK (SystemConfig.class, id);
                    if (config == null) {
                        config = new SystemConfig ();
                        config.setId (id);
                        config.setValue (value);
                        config.setEditable (true);
                        sqlite.save (config);
                    } else {
                        config.setValue (value);
                        sqlite.update (config);
                    }
                    break;
                }
                case "unset": {
                    SystemConfig config = sqlite.getByPK (SystemConfig.class, id);
                    if (config == null) {
                        console.errorln ("the config [" + id + "] not exists.");
                        return;
                    }

                    console.setForegroundColor (TerminalIO.MAGENTA);
                    if (console.readBoolean ("Are you sure to remove this config", false)) {
                        sqlite.delete (config);
                        console.println ("config [" + id + "] removed.");
                    }
                    break;
                }
                case "help":
                    showHelp (console);
                    break;
            }
        } finally {
            reset ();
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        colorLine (console, TerminalIO.YELLOW, "sys-config [command] [parameters]");
        colorLine (console, TerminalIO.YELLOW, "for show all configs: ");
        colorLine (console, TerminalIO.CYAN,   "  sys-config [print] [id]");
        colorLine (console, TerminalIO.CYAN,   "    if the parameter id is not present, shows all system config");
        colorLine (console, TerminalIO.YELLOW, "for set a system config:");
        colorLine (console, TerminalIO.CYAN,   "  sys-config set <id> <value>");
        colorLine (console, TerminalIO.YELLOW, "for remove a system config:");
        colorLine (console, TerminalIO.CYAN,   "  sys-config unset <id>");
    }

    private void reset () {
        action  = "print";
        id      = null;
        value   = null;
        message = null;
    }

    private void colorLine (Console console, int color, String text) throws IOException {
        console.setForegroundColor (color);
        console.println (text);
        console.setForegroundColor (TerminalIO.COLORINIT);
    }

    private void print (Console console) throws IOException {
        String sql = "SELECT MAX(LENGTH(id)) FROM t_sys_conf WHERE editable = 1";
        int maxIdLength = sqlite.getSingleField (Integer.class, sql);

        sql = "SELECT * FROM t_sys_conf WHERE editable = 1";
        if (!StringUtil.isEmpty (id)) {
            sql += " AND id = ?";
        }
        sql += " ORDER BY id ASC";
        int columns = console.getColumns (), second = columns - maxIdLength - 2;
        List<SystemConfig> list;
        if (StringUtil.isEmpty (id)) {
            list = sqlite.list (SystemConfig.class, sql);
        } else {
            list = sqlite.list (SystemConfig.class, sql, id);
        }

        if (list == null || list.isEmpty ()) {
            console.setForegroundColor (TerminalIO.CYAN);
            console.println ("no config");
            return;
        }
        console.write (TextFormater.fill ("ID", ' ', maxIdLength, Alignment.Right));
        console.write ("  ");
        console.println ("VALUE");

        // 汉字：[0x4e00,0x9fa5]（或十进制[19968,40869]）
        for (SystemConfig sc : list) {
            console.write (TextFormater.fill (sc.getId (), ' ', maxIdLength, Alignment.Right));
            console.write ("  ");
            String value = sc.getValue ();
            char[] buff  = value.toCharArray ();
            int length   = 0;
            for (int i = 0; i < buff.length; i ++) {
                char ch = buff [i];
                if (ch >= 0x4e00 && ch <= 0x9fa5) {
                    length += 2;
                } else {
                    length ++;
                }
                if (length > second) {
                    i --;
                    length = 0;
                    console.println ();
                    console.moveRight (maxIdLength + 2);
                } else {
                    if (ch >= 0x4e00 && ch <= 0x9fa5) {
                        // the chinese character
                        console.write (String.valueOf (ch));
                    } else {
                        // ascii
                        console.write (ch);
                    }

                    if (ch == '\n') {
                        length = 0;
                        console.moveRight (maxIdLength + 2);
                    }
                }
            }
            console.println ();
        }
    }
}
