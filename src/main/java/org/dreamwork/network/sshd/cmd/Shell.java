package org.dreamwork.network.sshd.cmd;

import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.channel.PuttyRequestHandler;
import org.apache.sshd.server.shell.TtyFilterInputStream;
import org.apache.sshd.server.shell.TtyFilterOutputStream;
import org.dreamwork.concurrent.Looper;
import org.dreamwork.network.Keys;
import org.dreamwork.network.bridge.io.Node;
import org.dreamwork.network.bridge.io.Pipe;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>open native shell.</p>
 * <p>for windows, open cmd.exe</p>
 * <p>for *nix, open /bin/bash</p>
 *
 * Created by seth.yang on 2019/11/4
 */
public class Shell extends Command {
    private ChannelSession channel;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private Environment env;

    public Shell (ChannelSession channel, Environment env) {
        super ("shell", "!", "execute native shell");
        this.channel = channel;
        this.env = env;

        in  = (InputStream) channel.getProperties ().get (Keys.KEY_IN);
        out = (OutputStream) channel.getProperties ().get (Keys.KEY_OUT);
        err = (OutputStream) channel.getProperties ().get (Keys.KEY_ERR);
    }

    /**
     * 执行命令
     *
     * @param console 当前控制台
     * @throws IOException io exception
     */
    @Override
    public void perform (Console console) throws IOException {
        List<String> cmd;
        if (OsUtils.isWin32 ()) {
            cmd = Collections.singletonList ("cmd.exe");
        } else {
            cmd = Arrays.asList ("/bin/bash", "-i", "-l");
        }
        Process p = new ProcessBuilder (cmd).start ();
        Map<PtyMode, ?> modes = resolveShellTtyOptions(env.getPtyModes(), channel.getSession ());
        TtyFilterInputStream pty_out = new TtyFilterInputStream (p.getInputStream (), modes);
        TtyFilterInputStream pty_err = new TtyFilterInputStream (p.getErrorStream (), modes);
        TtyFilterOutputStream pty_in = new TtyFilterOutputStream (p.getOutputStream (), pty_err, modes);

        Looper.invokeLater (() -> {
            Node.Input n_in = new Node.Input (in, out, err);
            Node.Output n_out = new Node.Output (pty_in, pty_out, pty_err);
            new Pipe (n_in, n_out).dump ();
        });
        try {
            int code = p.waitFor ();
            pty_in.close ();
            pty_err.close ();
            pty_out.close ();
            System.out.println ("----------------------------------------------------- code = " + code);
        } catch (Exception ex) {
            ex.printStackTrace ();
        }
        p.destroy ();
        console.println ();
        console.flush ();
    }

    /**
     * 根据输入的文本猜测可能合法的后续输入.
     * <ul>
     * <li>如果猜测无结果，返回 null</li>
     * <li>如果能够确定匹配后续输入，返回一条确切记录</li>
     * <li>如果能够猜测出多条可能的输入，返回一个列表</li>
     * </ul>
     *
     * @param text 输入的文本
     * @return 可能合法的后续输入.
     */
    @Override
    public List<String> guess (String text) {
        return null;
    }

    @Override
    public void showHelp (Console console) throws IOException {
        console.println ("A simple shell proxy");
    }

    private Map<PtyMode, Integer> resolveShellTtyOptions (Map<PtyMode, Integer> modes, Session session) {
        if (PuttyRequestHandler.isPuttyClient(session)) {
            return PuttyRequestHandler.resolveShellTtyOptions(modes);
        } else {
            return modes;
        }
    }
}