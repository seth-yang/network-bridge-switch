package org.dreamwork.network.bridge.util;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dreamwork.network.bridge.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Created by seth.yang on 2019/12/14
 */
public class Helper {
    private static final Logger logger = LoggerFactory.getLogger (Helper.class);
    public static NioSocketAcceptor bind (int port, IoHandler handler) throws IOException {
        NioSocketAcceptor acceptor = new NioSocketAcceptor ();
        acceptor.setReuseAddress (true);
        acceptor.setHandler (handler);
        acceptor.bind (new InetSocketAddress (port));

        logger.info ("nio service bound on {} using handler {}", port, handler);
        return acceptor;
    }

    public static ConnectionInfo connect (String host, int port, IoHandler handler) {
        NioSocketConnector connector = new NioSocketConnector ();
        connector.getSessionConfig ().setReuseAddress (true);
        connector.setHandler (handler);
        ConnectFuture future = connector.connect (new InetSocketAddress (host, port));
        future.awaitUninterruptibly ();
        IoSession session = future.getSession ();
        return new ConnectionInfo (connector, session);
    }

    public static String text (IoBuffer buffer) {
        int limit   = buffer.limit ();
        byte[] data = new byte[limit];
        buffer.get (data);
        return new String (data, StandardCharsets.UTF_8);
    }
}