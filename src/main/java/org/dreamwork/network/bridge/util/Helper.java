package org.dreamwork.network.bridge.util;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
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

    public static NioSocketAcceptor bind (int port, IoHandler handler, String[] names, IoFilter[] filters) throws IOException {
        NioSocketAcceptor acceptor = new NioSocketAcceptor ();
        acceptor.setReuseAddress (true);
        acceptor.setHandler (handler);
        if (names != null && filters != null) {
            if (names.length != filters.length) {
                logger.warn ("names are not match to filters");
                throw new IOException ("names are not match to filters");
            }

            for (int i = 0; i < names.length; i ++) {
                acceptor.getFilterChain ().addLast (names [i], filters [i]);
            }
        }
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

    public static ConnectionInfo connect (String host, int port, IoHandler handler, String[] names, IoFilter[] filters) {
        NioSocketConnector connector = new NioSocketConnector ();
        connector.addListener (listener);
        connector.getSessionConfig ().setReuseAddress (true);
        connector.setHandler (handler);
        if (names != null && filters != null && names.length == filters.length) {
            for (int i = 0; i < names.length; i ++) {
                connector.getFilterChain ().addLast (names [i], filters [i]);
            }
        }
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

    private static final IoServiceListener listener = new InternalIoServiceListener ();

    private static final class InternalIoServiceListener implements IoServiceListener {
        private final Logger logger = LoggerFactory.getLogger (InternalIoServiceListener.class);

        @Override
        public void serviceActivated (IoService service) {
            NioSocketConnector nsc = (NioSocketConnector) service;
            logger.trace ("{} active", nsc.getDefaultRemoteAddress ());
        }

        @Override
        public void serviceIdle (IoService service, IdleStatus idleStatus) {
            
        }

        @Override
        public void serviceDeactivated (IoService service) {
            NioSocketConnector nsc = (NioSocketConnector) service;
            logger.trace ("{} active", nsc.getDefaultRemoteAddress ());
        }

        @Override
        public void sessionCreated (IoSession session) {

        }

        @Override
        public void sessionClosed (IoSession session) {

        }

        @Override
        public void sessionDestroyed (IoSession session) {

        }
    }
}