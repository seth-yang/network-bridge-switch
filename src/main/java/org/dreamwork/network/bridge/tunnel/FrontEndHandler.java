package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.util.Tools;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Created by seth.yang on 2019/12/14
 */
public class FrontEndHandler extends IoHandlerAdapter {
    private Map<Integer, IoSession> managed_sessions;
    private Map<String, IoSession>  managed_tunnels;
    private Locks locks;

    private int category;
    private boolean blocked;

    private SecureRandom sr = new SecureRandom ();

    public FrontEndHandler (Map<Integer, IoSession> managed_sessions, int category, boolean blocked) {
        this.managed_sessions = managed_sessions;
        this.category         = category;
        this.blocked          = blocked;
    }

    public void setManagedTunnels (Map<String, IoSession> managed_tunnels) {
        this.managed_tunnels = managed_tunnels;
    }

    public void setLocks (Locks locks) {
        this.locks = locks;
    }

    @Override
    public void sessionOpened (IoSession session) throws Exception {
        IoSession peer = managed_sessions.get (category);
        if (peer != null) {
            final byte[] token = new byte[6];
            sr.nextBytes (token);
            String key = Tools.toHex (token).toLowerCase ();
            System.out.println ("generated token: " + key);
            locks.add (key, token);
            peer.write (IoBuffer.wrap (token));

            long now = System.currentTimeMillis ();
            locks.await (key, 10000);
            System.out.println ("first wait broken");
            if (System.currentTimeMillis () - now < 10000) {
                peer = managed_tunnels.get (key);
                session.setAttribute ("peer", peer);
                peer.setAttribute ("peer", session);
                System.out.println (">>> tunnel connected.");
                if (blocked) {
                    locks.await (key);
                }
                locks.release (key);
            } else {
                session.write (IoBuffer.wrap ("timeout".getBytes ()));
                session.closeNow ();
            }
        } else {
            session.write (IoBuffer.wrap ("no client active".getBytes ()));
            session.closeNow ();
        }
    }

    @Override
    public void exceptionCaught (IoSession session, Throwable cause) {
        cause.printStackTrace ();
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
        session.removeAttribute ("peer");
    }
}