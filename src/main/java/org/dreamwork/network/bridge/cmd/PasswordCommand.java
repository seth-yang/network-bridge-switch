package org.dreamwork.network.bridge.cmd;

import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.bridge.Context;
import org.dreamwork.network.bridge.data.User;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;
import org.dreamwork.util.StringUtil;

import java.io.IOException;

import static org.dreamwork.network.bridge.cmd.CommandUtil.readPassword;

/**
 * Created by seth.yang on 2019/11/14
 */
public class PasswordCommand extends Command {
    private String userName;

    public PasswordCommand () {
        super ("passwd", null, "change current or spec user's password");
    }

    @Override
    public void setContent (String content) {
        userName = content;
    }

    @Override
    public void perform (Console console) throws IOException {
        try {
            String current = console.getEnv ("USER");
            if (userName == null) {
                userName = current;
            } else if (!"root".equals (current) && !current.equals (userName)) {
                console.errorln ("You are not authorized to execute this command.");
                return;
            }

            User currentUser = Context.db.getByPK (User.class, current);
            console.write ("Please input current password: ");
            String current_password = console.readInput (true);

            String md5 = StringUtil.dump (AlgorithmUtil.md5 (current_password.getBytes ())).toLowerCase ();
            if (!md5.equals (currentUser.getPassword ())) {
                console.errorln ("current password mismatched.");
                return;
            }

            String password = readPassword ("Please input new password", console);
            if (StringUtil.isEmpty (password)) {
                return;
            }

            User user = currentUser;
            if (!current.equals (userName)) {
                user = Context.db.getByPK (User.class, userName);
            }

            if (user != null) {
                md5 = StringUtil.dump (AlgorithmUtil.md5 (password.getBytes ())).toLowerCase ();
                user.setPassword (md5);
                Context.db.update (user);
                console.setForegroundColor (TerminalIO.GREEN);
                console.println ("password change success.");
                console.setForegroundColor (TerminalIO.COLORINIT);
            } else {
                console.errorln (userName + " not exists.");
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();
        } finally {
            userName = null;
        }
    }
}
