package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import static org.dreamwork.network.bridge.util.Helper.connect;

/**
 * Created by seth.yang on 2019/12/18
 */
public class Tunnel {
    private IoSession session;
    public Tunnel (String src_host, int src_port, String dst_host, int dst_port) {
        session = connect (src_host, src_port, new IoHandlerAdapter () {
            @Override
            public void sessionCreated (IoSession session) {
                IoSession peer = connect (dst_host, dst_port, new IoHandlerAdapter () {
                    @Override
                    public void messageReceived (IoSession peer, Object message) {
                        session.write (message);
                    }

                    @Override
                    public void sessionClosed (IoSession session) {
                        IoSession peer = (IoSession) session.getAttribute ("peer");
                        if (peer != null) {
                            peer.closeNow ();
                        }
                    }
                });
                session.setAttribute ("peer", peer);
                peer.setAttribute ("peer", session);
            }

            @Override
            public void messageReceived (IoSession session, Object message) {
                IoSession peer = (IoSession) session.getAttribute ("peer");
                if (peer != null) {
                    peer.write (message);
                }
            }

            @Override
            public void sessionClosed (IoSession session) {
                IoSession peer = (IoSession) session.getAttribute ("peer");
                if (peer != null) {
                    peer.closeNow ();
                }
            }
        });
    }

    public IoSession getSession () {
        return session;
    }
}
