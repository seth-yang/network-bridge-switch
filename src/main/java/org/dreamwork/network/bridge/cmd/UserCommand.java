package org.dreamwork.network.bridge.cmd;

import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.bridge.Context;
import org.dreamwork.network.bridge.data.User;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

import static org.dreamwork.network.bridge.cmd.CommandUtil.option;
import static org.dreamwork.network.bridge.cmd.CommandUtil.readPassword;

/**
 * Created by seth.yang on 2019/11/14
 */
public class UserCommand extends Command {
    private String action, userName, message;

    public UserCommand () {
        super ("user", null, "user management");
    }

    @Override
    public void parse (String... options) {
        if (options.length == 1 && ("--help".equals (options [0]) || "-h".equals (options [0]))) {
            action = "help";
        } else if (options.length != 2) {
            message = "invalid arguments";
        } else {
            action = options[0];
            userName = options[1];
        }
    }

    @Override
    public boolean isOptionSupported () {
        return true;
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
            String current = console.getEnv ("USER");
            if (!"root".equals (current)) {
                console.errorln ("You are not authorized to execute this command.");
                return;
            }

            if (!StringUtil.isEmpty (message)) {
                console.errorln (message);
                return;
            }

            if ("help".equals (action)) {
                showHelp (console);
                return;
            }

            if (StringUtil.isEmpty (action) || StringUtil.isEmpty (userName)) {
                console.errorln ("invalid arguments");
                return;
            }

            User user = Context.db.getByPK (User.class, userName);
            switch (action) {
                case "add":
                    if (user != null) {
                        console.errorln ("user " + userName + " already exists.");
                        return;
                    }

                    String password = readPassword (console);
                    if (StringUtil.isEmpty (password)) {
                        return;
                    }

                    user = new User ();
                    user.setUserName (userName);
                    user.setPassword (StringUtil.dump (AlgorithmUtil.md5 (password.getBytes ())).toLowerCase ());
                    Context.db.save (user);

                    console.setForegroundColor (TerminalIO.GREEN);
                    console.println ("user add success.");
                    console.setForegroundColor (TerminalIO.COLORINIT);
                    break;
                case "del":
                case "delete":
                    if (user != null) {
                        Boolean answer = option ("Are you sure to delete the user", false, console);
                        if (answer != null && answer) {
                            Context.db.delete (user);
                            console.setForegroundColor (TerminalIO.GREEN);
                            console.println ("user delete success.");
                            console.setForegroundColor (TerminalIO.COLORINIT);
                        }
                    } else {
                        console.errorln ("user " + userName + " not found.");
                    }
                    break;
                default:
                    console.errorln ("invalid command: " + action);
                    break;
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();
        } finally {
            action   = null;
            userName = null;
            message  = null;
        }
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("user add|del[ete] <username>");
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("user -h | --help    show this list");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }
}