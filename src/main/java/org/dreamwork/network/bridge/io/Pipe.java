package org.dreamwork.network.bridge.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by seth.yang on 2019/11/5
 */
public class Pipe {
    private static final int DEFAULT_BUFF_SIZE = 8192;
    private static final Logger logger = LoggerFactory.getLogger (Pipe.class);

    private Node.Input in;
    private Node.Output out;
    private int buffSize;
    private Charset charset = StandardCharsets.UTF_8;

    public Pipe (Node.Input in, Node.Output out) {
        this (DEFAULT_BUFF_SIZE, in, out);
    }

    public Pipe (int buffSize, Node.Input in, Node.Output out) {
        this.in         = in;
        this.out        = out;
        this.buffSize   = buffSize;
    }

    public Pipe setBuffSize (int buffSize) {
        this.buffSize = buffSize;
        return this;
    }

    public Pipe setCharset (Charset charset) {
        this.charset = charset;
        return this;
    }

    public void dump () {
        for (byte[] buff = new byte[buffSize];;) {
            try {
                if (dump (in.in, out.in, buff)) {
                    continue;
                }
                if (dump (out.out, in.out, buff)) {
                    continue;
                }
                if (dump (out.err, in.err, buff)) {
                    continue;
                }
            } catch (IOException ex) {
                logger.warn (ex.getMessage (), ex);
                break;
            }

            try {
                Thread.sleep (10);
            } catch (InterruptedException ex) {
                logger.warn (ex.getMessage (), ex);
            }
        }
    }

    private boolean dump (InputStream in, OutputStream out, byte[] buff) throws IOException {
        int available = in.available ();
        if (available > 0) {
            int len = in.read (buff);
            if (len > 0) {
                out.write (buff, 0, len);
                out.flush ();
                return true;
            }

        }
        return false;
    }
}