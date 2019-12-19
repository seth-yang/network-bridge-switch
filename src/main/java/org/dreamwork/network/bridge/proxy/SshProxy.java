package org.dreamwork.network.bridge.proxy;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.PtyChannelConfiguration;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.apache.sshd.server.channel.ChannelSession;
import org.dreamwork.network.Keys;
import org.dreamwork.telnet.Console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by seth.yang on 2019/11/6
 */
public class SshProxy {
    private Console console;
    private ChannelSession channel;

    public SshProxy (Console console, ChannelSession channel) {
        this.console = console;
        this.channel = channel;
    }

    public void connect () throws IOException {
        try (SshClient client = SshClient.setUpDefaultClient ()) {
            client.start ();

            ConnectFuture future = client.connect ("root", "192.168.2.41", 22);
            if (future.await ()) {
                try (ClientSession session = future.getSession ()) {
                    session.addPasswordIdentity ("123456");
                    session.auth ().verify (30000);

                    InputStream in  = (InputStream) channel.getProperties ().get (Keys.KEY_IN);
                    OutputStream out = (OutputStream) channel.getProperties ().get (Keys.KEY_OUT);
                    OutputStream err = (OutputStream) channel.getProperties ().get (Keys.KEY_ERR);

                    PtyChannelConfiguration conf = new PtyChannelConfiguration ();
                    conf.setPtyType ("xterm");
                    conf.setPtyColumns (console.getColumns ());
                    conf.setPtyLines (console.getRows ());

                    try (ClientChannel channel = session.createShellChannel (conf, null)) {
                        channel.setIn (new NoCloseInputStream (in));
                        channel.setOut (new NoCloseOutputStream (out));
                        channel.setOut (new NoCloseOutputStream (err));
                        channel.open ();
                        List<ClientChannelEvent> events = Collections.singletonList (ClientChannelEvent.CLOSED);
                        channel.waitFor (events, 0);
                    }
                } finally {
                    client.stop ();
                }
            }
        }
    }
}
