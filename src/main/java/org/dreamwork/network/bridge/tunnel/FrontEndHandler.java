package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.tunnel.data.TokenCommand;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Created by seth.yang on 2019/12/14
 */
public class FrontEndHandler extends IoHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger (FrontEndHandler.class);
    private Map<Integer, IoSession> managedSessions;
    private Map<String, IoSession> managedTunnels;
    private Locks locks;
    private String tunnelName;
    private TunnelMonitor monitor;

    private int category;
    private boolean blocked;

    private SecureRandom sr = new SecureRandom ();

    public FrontEndHandler (Map<Integer, IoSession> managedSessions, int category, boolean blocked) {
        this.managedSessions  = managedSessions;
        this.category         = category;
        this.blocked          = blocked;
    }

    public void setTunnelName (String tunnelName) {
        this.tunnelName = tunnelName;
    }

    public void setMonitor (TunnelMonitor monitor) {
        this.monitor = monitor;
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
            logger.trace ("opening a front-end session: {}", session);
        }

        IoSession clientManager = managedSessions.get (category);
        if (clientManager != null) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("found the client manager: {}", clientManager);
            }

            final byte[] token = new byte[6];
            sr.nextBytes (token);
            String key = StringUtil.byte2hex (token, false);
            if (logger.isTraceEnabled ()) {
                logger.trace ("generated token: {}", key);
            }
            locks.add (key, token);
            if (logger.isTraceEnabled ()) {
                logger.trace ("a new lock [{}] acquired.", key);
            }
            try {
                if (logger.isTraceEnabled ()) {
                    logger.trace ("sending the token [{}] to client...", key);
                }
                TokenCommand tc = new TokenCommand ();
                tc.token = token;
                clientManager.write (tc);
                if (logger.isTraceEnabled ()) {
                    logger.trace ("token [{}] wrote, waiting for client's response", key);
                    logger.trace ("waiting for 10 seconds to receive client manager create the proxy...");
                }
                long now = System.currentTimeMillis (), take;
                locks.await (key, 10000);
                take = System.currentTimeMillis () - now;
                if (logger.isTraceEnabled ()) {
                    logger.trace ("first wait broken, take {} ms", take);
                }
                if (take < 10000) {
                    if (logger.isTraceEnabled ()) {
                        logger.trace ("here, i'm waked up! it means the client responds me, get to work!");
                    }

                    IoSession tunnel = managedTunnels.get (key);
                    if (tunnel != null) {
                        session.setAttribute ("peer", tunnel);
                        tunnel.setAttribute ("peer", session);
                        tunnel.setAttribute ("key", key);
                        session.setAttribute ("key", key);
                        if (logger.isInfoEnabled ()) {
                            logger.info (">>> tunnel [{}] establishing. local = {}, peer = {}", key, session, tunnel);
                        }
                        monitor.addTunnel (tunnelName, key, session, tunnel);
                        if (blocked) {
                            if (logger.isTraceEnabled ()) {
                                logger.trace ("the client required me await until it disconnect");
                            }
                            locks.await (key);
                        }
                    } else {
                        // no key associated? I don't know why :(
                        logger.warn ("there's no key associated to this session...");
                        session.closeNow ();
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
        String key = (String) session.getAttribute ("key");
        logger.warn ("an error occurred in tunnel [{}]: ", key);
        logger.warn (cause.getMessage (), cause);
        cause.printStackTrace ();
    }

    @Override
    public void messageReceived (IoSession session, Object message) {
        IoSession peer = (IoSession) session.getAttribute ("peer");
        if (peer != null) {
            peer.write (message);
        }

        if (logger.isTraceEnabled ()) {
            session.setAttribute ("timestamp", System.currentTimeMillis ());
        }
    }

    @Override
    public void sessionClosed (IoSession session) {
        String key = (String) session.getAttribute ("key");
        if (logger.isTraceEnabled ()) {
            logger.trace ("tunnel [{}] going to close", key);
        }
        IoSession peer = (IoSession) session.getAttribute ("peer");
        if (logger.isInfoEnabled ()) {
            logger.info ("front-end session closed. local = {}, peer = {}", session, peer);
        }
        if (peer != null) {
            peer.closeNow ();
        }
        if (monitor != null) {
            monitor.removeTunnel (tunnelName, key);
        }
        session.removeAttribute ("peer");
        session.removeAttribute ("key");
    }

    @Override
    public void sessionIdle (IoSession session, IdleStatus status) {
        Long timestamp = (Long) session.getAttribute ("timestamp");
        String key = (String) session.getAttribute ("key");
        if (timestamp != null) {
            long delta = System.currentTimeMillis () - timestamp;
            logger.trace ("session[{}] idled, last communicated was {} ms ago, at {}, status = {}", key, delta, new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss").format (timestamp), status);
        } else if (logger.isTraceEnabled ()) {
            logger.trace ("session[{}] idled, status = {}", key, status);
        }
    }
}