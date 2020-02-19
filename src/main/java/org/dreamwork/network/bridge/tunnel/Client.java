package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seth.yang on 2019/12/18
 */
public class Client {
    private final Logger logger = LoggerFactory.getLogger (Client.class);

    public final String name;
    public final boolean blocked;
    public final int port;

    public NioSocketAcceptor acceptor;
    public IoSession managerSession;

    private Map<String, Tunnel> tunnels = new ConcurrentHashMap<> ();

    Client (String name, boolean blocked, int port) {
        this.name = name;
        this.blocked = blocked;
        this.port = port;
    }

    void addTunnel (IoSession session) {
        if (session == null) {
            logger.warn ("the session is null, ignore this option");
            return;
        }

        String key = (String) session.getAttribute ("key");
        if (StringUtil.isEmpty (key)) {
            logger.warn ("the session does not contain attribute: key, ignore this option");
            return;
        }

        Tunnel tunnel = new Tunnel ();
        tunnel.key = key;
        tunnel.session = session;
        tunnels.put (key, tunnel);
    }

    void removeTunnel (IoSession session) {
        if (session == null) {
            logger.warn ("the session is null, ignore this option");
            return;
        }

        String key = (String) session.getAttribute ("key");
        if (StringUtil.isEmpty (key)) {
            logger.warn ("the session does not contain attribute: key, ignore this option");
            return;
        }

        tunnels.remove (key);
    }

    private static final class Tunnel {
        public String key;
        public IoSession session;
    }
}