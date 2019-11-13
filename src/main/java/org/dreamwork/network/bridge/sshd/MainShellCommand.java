package org.dreamwork.network.bridge.sshd;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.dreamwork.concurrent.Looper;
import org.dreamwork.network.bridge.Keys;
import org.dreamwork.network.bridge.NetworkSwitch;
import org.dreamwork.network.bridge.cmd.*;
import org.dreamwork.network.bridge.io.IoSessionInputStream;
import org.dreamwork.network.bridge.io.IoSessionOutputStream;
import org.dreamwork.network.bridge.io.Node;
import org.dreamwork.network.bridge.io.Pipe;
import org.dreamwork.telnet.ConnectionData;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.SimpleCommandParser;
import org.dreamwork.telnet.TerminalIO;
import org.dreamwork.telnet.command.CommandParser;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

/**
 * Created by seth.yang on 2019/10/29
 */
public class MainShellCommand implements Command {
    private InputStream in;
    private OutputStream out;
    private OutputStream err;

    private Console console;

    private final Logger logger = LoggerFactory.getLogger (MainShellCommand.class);

    @Override
    public void setInputStream (InputStream in) {
        this.in = in;
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting input stream into : {}", in);
        }
    }

    @Override
    public void setOutputStream (OutputStream out) {
        this.out = out;
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting output stream into: {}", out);
        }
    }

    @Override
    public void setErrorStream (OutputStream err) {
        this.err = err;
        if (logger.isTraceEnabled ()) {
            logger.trace ("setting err stream into : {}", err);
        }
    }

    /**
     * Set the callback that the shell has to call when it is closed.
     *
     * @param callback The {@link ExitCallback} to call when shell is closed
     */
    @Override
    public void setExitCallback (ExitCallback callback) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("exit callback set: {}", callback);
        }
    }

    private void onWindowChanged (Map<String, String> map, ConnectionData cd) {
        String term     = map.get (Environment.ENV_TERM);
        String s_column = map.get (Environment.ENV_COLUMNS);
        String s_row    = map.get (Environment.ENV_LINES);

        if (logger.isTraceEnabled ()) {
            logger.trace ("width = {}, height = {}, term = {}", s_column, s_row, term);
        }

        if (!StringUtil.isEmpty (term)) {
            cd.setNegotiatedTerminalType (term);
        }
        if (!StringUtil.isEmpty (s_column) && !StringUtil.isEmpty (s_row)) {
            int column = Integer.parseInt (s_column);
            int row    = Integer.parseInt (s_row);
            cd.setTerminalGeometry (column, row);
        }
    }

    @Override
    public void start (ChannelSession channel, Environment env) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("starting the command ...");
        }

        if (in != null && out != null) {
            channel.getProperties ().put (Keys.KEY_IN, in);
            channel.getProperties ().put (Keys.KEY_OUT, out);
            channel.getProperties ().put (Keys.KEY_ERR, err);

            ConnectionData cd = new ConnectionData ();
            {
                Map<String, String> map = env.getEnv ();
                onWindowChanged (map, cd);
                env.addSignalListener ((channel1, signal) -> {
                    if (signal == Signal.WINCH) {
                        onWindowChanged (map, cd);
                    }
                });
            }
            channel.getProperties ().put ("connect.data", cd);
            console = new Console (in, out, cd, true);
            CommandParser parser = new SimpleCommandParser (true);
            parser.registerCommand (
                    new Shell (channel, env),
                    new Ssh (channel),
                    new AddDeviceCommand (),
                    new ShowDeviceCommand (),
                    new NatCommand (),
                    new org.dreamwork.telnet.command.Command ("test-command", "tc", "this is a test command") {
                        @Override
                        public void perform (Console console) throws IOException {
                            if ("aaa".equals (content)) {
                                console.errorln ("error message");
                            } else {
                                console.println ("your type: " + content);
                            }
                            content = null;
                        }

                        @Override
                        public void showHelp (Console console) throws IOException {
                            console.setForegroundColor (TerminalIO.YELLOW);
                            console.println ("this is a test help");
                            console.setForegroundColor (TerminalIO.COLORINIT);
                        }
                    }
            );
            parser.registerCommand (new org.dreamwork.telnet.command.Command ("conn", null, "ssh connector") {
                @Override
                public void perform (Console console) {
                    if (NetworkSwitch.CACHE [0] != null) {
                        IoSession session = NetworkSwitch.CACHE [0];
                        IoBuffer buff = IoBuffer.wrap ("connect".getBytes ());
                        session.write (buff);

                        InputStream s_in = new IoSessionInputStream ();
                        OutputStream s_out = new IoSessionOutputStream (session);

                        Node.Input n_in = new Node.Input (in, out, err);
                        Node.Output n_out = new Node.Output (s_out, s_in, s_in);
                        new Pipe (n_in, n_out).dump ();
                    }
                }
            });
            console.setCommandParser (parser);
            for (Map.Entry<String, String> e : env.getEnv ().entrySet ()) {
                console.setEnv (e.getKey (), e.getValue ());
            }
            Looper.invokeLater (() -> {
                try {
                    console.loop ();
                    channel.close ();
                    channel.getSession ().close ();
                } catch (IOException e) {
                    e.printStackTrace ();
                }
            });
        }
    }

    @Override
    public void destroy (ChannelSession channel) throws Exception {

    }
}