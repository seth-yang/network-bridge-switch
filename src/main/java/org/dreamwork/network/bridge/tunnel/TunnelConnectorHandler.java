package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.tunnel.data.Command;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by seth.yang on 2019/12/14
 */
public class TunnelConnectorHandler extends IoHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger (TunnelConnectorHandler.class);
    private Map<String, IoSession>  managed_tunnels;
    private Locks                   locks;

    public TunnelConnectorHandler (Map<String, IoSession> managed_tunnels) {
        this.managed_tunnels = managed_tunnels;
    }

    public void setLocks (Locks locks) {
        this.locks = locks;
    }

    @Override
    public void sessionOpened (IoSession session) {
        if (logger.isTraceEnabled ()) {
            String uuid = StringUtil.uuid ();
            session.setAttribute ("uuid", uuid);
            logger.trace ("a session[{}] connected, detail = {}", uuid, session);
        }
    }

    @Override
    public void messageReceived (IoSession session, Object message) {
        IoSession peer = (IoSession) session.getAttribute ("peer");
        String uuid = (String) session.getAttribute ("uuid");
        if (peer == null) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("the session[{}] first communicated, no peer associated, the message should contain the token", uuid);
            }
            IoBuffer buffer = (IoBuffer) message;
            if (buffer.limit () >= 7) {
                int cmd = buffer.get () & 0xff;
                if (logger.isTraceEnabled ()) {
                    logger.trace ("got command: {}", cmd);
                }

                if (cmd != Command.TOKEN) {
                    logger.warn ("in this round, command 0x04 is only valid command");
                    session.closeNow ();
                }

                byte[] token = new byte[6];
                buffer.get (token);
                String key = StringUtil.byte2hex (token, false);
                if (logger.isTraceEnabled ()) {
                    logger.trace ("the session[{}}] got token: {}", uuid, key);
                }
                managed_tunnels.put (key, session);
                locks.notify (key);
            } else if (logger.isTraceEnabled ()){
                logger.trace ("What The Fuck!!!");
            }
            if (logger.isTraceEnabled ()) {
                logger.trace ("peer != null complete, but why?");
            }
        } else {
            peer.write (message);
        }

        if (logger.isTraceEnabled ()) {
            session.setAttribute ("timestamp", System.currentTimeMillis ());
        }
    }

    @Override
    public void messageSent (IoSession session, Object message) {
        if (logger.isTraceEnabled ()) {
            session.setAttribute ("timestamp", System.currentTimeMillis ());
        }
    }

    @Override
    public void sessionClosed (IoSession session) {
        IoSession peer = (IoSession) session.getAttribute ("peer");
        String key = (String) session.getAttribute ("key");
        if (logger.isTraceEnabled ()) {
            logger.trace (">>>>>>>> the session closed. key = {}, remote = {}", key, session.getRemoteAddress ());
        }
        if (peer != null) {
            peer.removeAttribute ("peer");
        }
        session.removeAttribute ("peer");
        if (!StringUtil.isEmpty (key)) {
            locks.notify (key);
        }
    }

    @Override
    public void sessionIdle (IoSession session, IdleStatus status) {
        Long timestamp = (Long) session.getAttribute ("timestamp");
        String key = (String) session.getAttribute ("key");
        if (timestamp != null) {
            long delta = System.currentTimeMillis () - timestamp;
            logger.trace ("session[{}] idled, last communicated was {} ms ago, status = {}", key, delta, status);
        } else if (logger.isTraceEnabled ()) {
            logger.trace ("session[{}] idled, status = {}", key, status);
        }
    }
}