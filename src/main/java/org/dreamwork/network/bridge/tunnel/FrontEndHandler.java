package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Created by seth.yang on 2019/12/14
 */
public class FrontEndHandler extends IoHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger (FrontEndHandler.class);
    private Map<Integer, IoSession> managedSessions;
    private Map<String, IoSession> managedTunnels;
    private Locks locks;

    private int category;
    private boolean blocked;

    private SecureRandom sr = new SecureRandom ();

    public FrontEndHandler (Map<Integer, IoSession> managedSessions, int category, boolean blocked) {
        this.managedSessions  = managedSessions;
        this.category         = category;
        this.blocked          = blocked;
    }

    public void setManagedTunnels (Map<String, IoSession> managedTunnels) {
        this.managedTunnels = managedTunnels;
    }

    public void setLocks (Locks locks) {
        this.locks = locks;
    }

    @Override
    public void sessionOpened (IoSession session) throws Exception {
        if (logger.isTraceEnabled ()) {
            logger.trace ("opening a front-end session: remote = {}, local-port = {}", session.getRemoteAddress (), category);
        }
        IoSession peer = managedSessions.get (category);
        if (peer != null) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("found the peer: {}", peer);
            }
            if (logger.isInfoEnabled ()) {
                logger.info ("a connection establishing: from {} to {}", session, peer);
            }
            final byte[] token = new byte[6];
            sr.nextBytes (token);
            String key = Tools.toHex (token).toLowerCase ();
            if (logger.isTraceEnabled ()) {
                logger.trace ("generated token: {}", key);
            }
            locks.add (key, token);
            if (logger.isTraceEnabled ()) {
                logger.trace ("a new lock {} acquired.", key);
            }
            try {
                if (logger.isTraceEnabled ()) {
                    logger.trace ("sending the token [{}] to client...", key);
                }
                peer.write (IoBuffer.wrap (token));
                if (logger.isTraceEnabled ()) {
                    logger.trace ("token [{}] wrote, waiting for client's response", key);
                    System.out.println ();
                    System.out.println ();
                    System.out.println ();
                }

                long now = System.currentTimeMillis ();
                locks.await (key, 10000);
                if (logger.isTraceEnabled ()) {
                    logger.trace ("first wait broken");
                }
                if (System.currentTimeMillis () - now < 10000) {
                    if (logger.isTraceEnabled ()) {
                        logger.trace ("here, i'm waked up! it means the client responds me, get to work!");
                    }
                    peer = managedTunnels.get (key);
                    session.setAttribute ("peer", peer);
                    peer.setAttribute ("peer", session);
                    if (logger.isTraceEnabled ()) {
                        logger.trace (">>> tunnel connected. local = {}, peer = {}", session, peer);
                    }
                    if (blocked) {
                        locks.await (key);
                    }
                } else {
                    session.write (IoBuffer.wrap ("timeout".getBytes ()));
                    session.closeNow ();
                }
            } finally {
                locks.release (key);
                if (logger.isTraceEnabled ()) {
                    logger.trace ("lock {} released.", key);
                }
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
        if (logger.isInfoEnabled ()) {
            logger.info ("front-end session closed. local = {}, peer = {}", session, peer);
        }
        if (peer != null) {
            peer.closeNow ();
        }
        session.removeAttribute ("peer");
    }
}