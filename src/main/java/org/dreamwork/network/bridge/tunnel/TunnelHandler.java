package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.util.StringUtil;
import org.dreamwork.util.Tools;

import java.util.Map;

/**
 * Created by seth.yang on 2019/12/14
 */
public class TunnelHandler extends IoHandlerAdapter {
    private Map<String, IoSession>  managed_tunnels;
    private Locks                   locks;

    private String key;

    public TunnelHandler (Map<String, IoSession> managed_tunnels) {
        this.managed_tunnels = managed_tunnels;
    }

    public void setLocks (Locks locks) {
        this.locks = locks;
    }

    @Override
    public void sessionCreated (IoSession session) {
        System.out.println ("a tunnel connected");
    }

    @Override
    public void messageReceived (IoSession session, Object message) {
        IoSession peer = (IoSession) session.getAttribute ("peer");
        if (peer == null) {
            IoBuffer buffer = (IoBuffer) message;
            if (buffer.limit () >= 6) {
                byte[] token = new byte[6];
                buffer.get (token);
                key = Tools.toHex (token).toLowerCase ();
                System.out.println ("[tunnel manager] got token: " + key);
                managed_tunnels.put (key, session);
                locks.notify (key);
            }
        } else {
            System.out.println ("[tunnel manager] write to peer");
            peer.write (message);
        }
    }

    @Override
    public void sessionClosed (IoSession session) {
        IoSession peer = (IoSession) session.getAttribute ("peer");
        System.out.println ("[tunnel manager] close session.");
        if (peer != null) {
            peer.removeAttribute ("peer");
        }
        session.removeAttribute ("peer");
        if (!StringUtil.isEmpty (key)) {
            locks.notify (key);
        }
    }
}
