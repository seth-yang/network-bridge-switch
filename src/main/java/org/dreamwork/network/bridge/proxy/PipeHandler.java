package org.dreamwork.network.bridge.proxy;

import org.apache.mina.core.session.IoSession;

/**
 * Created by seth.yang on 2019/11/5
 */
public class PipeHandler extends ProxyIoHandler {
    @Override
    public void sessionOpened (IoSession session) throws Exception {
        super.sessionOpened (session);
    }

    @Override
    public void messageReceived (IoSession session, Object message) throws Exception {
        super.messageReceived (session, message);
    }
}
