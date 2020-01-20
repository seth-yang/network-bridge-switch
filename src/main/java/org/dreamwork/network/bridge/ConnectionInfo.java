package org.dreamwork.network.bridge;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

public final class ConnectionInfo {
    public final NioSocketConnector connector;
    public final IoSession session;

    public ConnectionInfo (NioSocketConnector connector, IoSession session) {
        this.connector = connector;
        this.session = session;
    }
}