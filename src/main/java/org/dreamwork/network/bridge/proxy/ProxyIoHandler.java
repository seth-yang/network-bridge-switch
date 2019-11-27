package org.dreamwork.network.bridge.proxy;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.Keys;
import org.dreamwork.network.bridge.io.IoSessionInputStream;
import org.dreamwork.network.bridge.io.IoSessionOutputStream;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by seth.yang on 2019/11/1
 */
public class ProxyIoHandler extends IoHandlerAdapter {
    public void sessionOpened (IoSession session) throws Exception {
        // Create streams
        InputStream in = new IoSessionInputStream ();
        OutputStream out = new IoSessionOutputStream (session);
        session.setAttribute (Keys.KEY_IN, in);
        session.setAttribute (Keys.KEY_OUT, out);
        session.setAttribute (Keys.LAST_UPDATE_TIMESTAMP, System.currentTimeMillis ());
    }

    public void sessionCreated (IoSession session) {
        if (session.isReadSuspended() || session.isWriteSuspended()) {
            session.resumeRead();
            session.resumeWrite();
        }
    }

    public void messageSent(IoSession session, Object o) {
        session.setAttribute(Keys.LAST_UPDATE_TIMESTAMP, System.currentTimeMillis ());
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        session.setAttribute (Keys.LAST_UPDATE_TIMESTAMP, System.currentTimeMillis ());
        IoSession peer = (IoSession) session.getAttribute (Keys.KEY_PEER);
        if (peer != null) {
            peer.write (message);
        } else {
            IoSessionInputStream in = (IoSessionInputStream) session.getAttribute(Keys.KEY_IN);
            in.write((IoBuffer) message);
        }
    }
}