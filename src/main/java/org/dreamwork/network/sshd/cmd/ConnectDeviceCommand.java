package org.dreamwork.network.sshd.cmd;

import org.apache.sshd.server.channel.ChannelSession;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;

/**
 * Created by seth.yang on 2019/11/6
 */
public class ConnectDeviceCommand extends Command {
    private ChannelSession session;

    public ConnectDeviceCommand (ChannelSession session) {
        super ("connect", "conn", "Connect to a server");
        this.session = session;
    }

    @Override
    public void perform (Console console) throws IOException {

    }
}