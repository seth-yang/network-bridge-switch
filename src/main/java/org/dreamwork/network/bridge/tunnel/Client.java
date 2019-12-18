package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * Created by seth.yang on 2019/12/18
 */
public class Client {
    public final String name;
    public final boolean blocked;
    public final int port;

    public NioSocketAcceptor acceptor;

    Client (String name, boolean blocked, int port) {
        this.name = name;
        this.blocked = blocked;
        this.port = port;
    }
}