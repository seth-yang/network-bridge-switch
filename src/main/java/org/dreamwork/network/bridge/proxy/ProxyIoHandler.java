package org.dreamwork.network.bridge.proxy;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.Keys;
import org.dreamwork.network.bridge.io.IoSessionInputStream;
import org.dreamwork.network.bridge.io.IoSessionOutputStream;

import java.io.IOException;
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
//        session.setAttribute (Keys.IS_PROXY_TYPE, Boolean.FALSE);
        session.setAttribute (Keys.LAST_UPDATE_TIMESTAMP, System.currentTimeMillis ());
    }

    public void sessionCreated (IoSession session) throws Exception {
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
        Boolean isProxyType = (Boolean) session.getAttribute (Keys.IS_PROXY_TYPE);
//        Boolean isProxySsh = (Boolean) session.getAttribute("IS_PROXY_SSH");

        if (isProxyType == null || !isProxyType) {
            IoSessionInputStream in = (IoSessionInputStream) session.getAttribute(Keys.KEY_IN);
            in.write((IoBuffer) message);
        } else {
            infiltrate(session, message);
        }
/*

        // proxy跳转ssh方式登录设备
        if (isProxySsh != null && isProxySsh) {
            if (session.getAttribute("") != null) {
                IoSession proxy = (IoSession) session.getAttribute("");
                proxy.write(message);
            }
        }
*/
    }

    protected void infiltrate(IoSession session, Object message) throws IOException {
        if (session.getAttribute("peer") != null) {
            IoBuffer rb = (IoBuffer) message;
            IoBuffer wb = IoBuffer.allocate(rb.remaining ());
            rb.mark ();
            wb.put (rb);
            wb.flip ();
            ((IoSession) session.getAttribute("peer")).write (wb);
            rb.reset ();
        }
    }
}
