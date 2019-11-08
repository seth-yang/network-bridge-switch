package org.dreamwork.network.bridge.cmd;

import org.apache.sshd.server.channel.ChannelSession;
import org.dreamwork.network.bridge.proxy.SshProxy;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;

/**
 * Created by seth.yang on 2019/11/5
 */
public class Ssh extends Command {
    private ChannelSession channel;

    public Ssh (ChannelSession channel) {
        super ("ssh", null, "a simple ssh client");
        this.channel = channel;
    }

    /**
     * 执行命令
     *
     * @param console 当前控制台
     * @throws IOException io exception
     */
    @Override
    public void perform (Console console) throws IOException {
        new SshProxy (console, channel).connect ();
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.setForegroundColor (TerminalIO.YELLOW);
        console.println ("A simple ssh client");
        console.setForegroundColor (TerminalIO.COLORINIT);
    }
}