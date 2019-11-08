package org.dreamwork.network.bridge.io;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by seth.yang on 2019/11/1
 */
public class IoSessionOutputStream extends OutputStream {
    private final IoSession session;

    private WriteFuture lastWriteFuture;

    public IoSessionOutputStream (IoSession session) {
        this.session = session;
    }

    @Override
    public void close () throws IOException {
        try {
            flush ();
        } finally {
            session.close (true).awaitUninterruptibly ();
        }
    }

    private void checkClosed () throws IOException {
        if (!session.isConnected ()) {
            throw new IOException ("The session has been closed.");
        }
    }

    private synchronized void write (IoBuffer buf) throws IOException {
        checkClosed ();
        lastWriteFuture = session.write (buf);
    }

    @Override
    public void write (byte[] b, int off, int len) throws IOException {
        write (IoBuffer.wrap (b.clone (), off, len));
    }

    @Override
    public void write (int b) throws IOException {
        IoBuffer buf = IoBuffer.allocate (1);
        buf.put ((byte) b);
        buf.flip ();
        write (buf);
    }

    @Override
    public synchronized void flush () throws IOException {
        if (lastWriteFuture == null) {
            return;
        }

        lastWriteFuture.awaitUninterruptibly ();
        if (!lastWriteFuture.isWritten ()) {
            throw new IOException (
                    "The bytes could not be written to the session");
        }
    }
}
