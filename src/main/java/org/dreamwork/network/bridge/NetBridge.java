package org.dreamwork.network.bridge;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dreamwork.network.sshd.data.NAT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by seth.yang on 2019/11/12
 */
public class NetBridge {
    private static final Map<Integer, SocketAcceptor> cachedAcceptors = new TreeMap<> ();
    private static final Map<Integer, Proxy> cachedProxies = new HashMap<> ();

    private static final Logger logger = LoggerFactory.getLogger (NetBridge.class);

/*
    public static void main (String[] args) throws Exception {
        Configuration.load (null);
        Configuration.initDatabase ();
    }
*/

/*
    private static void test (String... args) throws IOException {
        int source = 8899, target = 22;
        String host = "127.0.0.1";

        if (args.length > 0) {
            for (int i = 0; i < args.length; i ++) {
                String p = args[i];
                String name = null, value = null;
                if (p.startsWith ("--")) {
                    p = p.substring (2);
                    int pos = p.indexOf ('=');
                    name = p.substring (0, pos);
                    value = p.substring (pos + 1);
                } else if (p.startsWith ("-")) {
                    name = p.substring (1);
                    value = args [++ i].trim ();
                }

                if (!StringUtil.isEmpty (name)) {
                    switch (name) {
                        case "s":
                        case "source-port":
                            try {
                                source = Integer.parseInt (value.trim ());
                            } catch (Exception ex) {
                                ex.printStackTrace ();
                            }
                            break;
                        case "t":
                        case "target-port":
                            try {
                                target = Integer.parseInt (value.trim ());
                            } catch (Exception ex) {
                                ex.printStackTrace ();
                            }
                            break;
                        case "h":
                        case "target-host":
                            host = value.trim ();
                            break;
                        default:

                    }
                }
            }
        }

        transform (source, host, target);

        {
            SocketAcceptor acceptor = new NioSocketAcceptor ();
            acceptor.setReuseAddress (true);
            acceptor.getSessionConfig ().setReuseAddress (true);
            acceptor.setHandler (new IoHandlerAdapter () {
                @Override
                public void messageReceived (IoSession session, Object message) {
                    IoBuffer buffer = (IoBuffer) message;
                    int length = buffer.limit ();
                    byte[] data = new byte[length];
                    buffer.get (data);
                    session.write (IoBuffer.wrap (data));
                }
            });
            acceptor.bind (new InetSocketAddress (7788));
        }
    }
*/

    private static void transform (int source, String host, int target) throws IOException {
        if (logger.isTraceEnabled ()) {
            logger.trace ("trying to bind NAT rule: {}:{}:{}", source, host, target);
        }
        SocketAcceptor acceptor = new NioSocketAcceptor ();
        acceptor.setReuseAddress (true);
        acceptor.getSessionConfig ().setReuseAddress (true);
        acceptor.setHandler (new Proxy (source, host, target));
        acceptor.bind (new InetSocketAddress (source));
        cachedAcceptors.put (source, acceptor);
    }

    public static void transform (NAT nat) throws IOException {
        transform (nat.getLocalPort (), nat.getRemoteHost (), nat.getRemotePort ());
    }

    public static boolean isBound (int port) {
        return cachedAcceptors.containsKey (port);
    }

    public static void shutdown (int port) {
        if (cachedAcceptors.containsKey (port)) {
            SocketAcceptor acceptor = cachedAcceptors.get (port);
            acceptor.unbind ();
            cachedAcceptors.remove (port);

            if (logger.isTraceEnabled ()) {
                logger.trace ("{}", cachedAcceptors);
            }
        }

        if (cachedProxies.containsKey (port)) {
            cachedProxies.get (port).shutdown ();
            cachedProxies.remove (port);

            if (logger.isTraceEnabled ()) {
                logger.trace ("{}", cachedProxies);
            }
        }
    }

    private static final class Proxy extends MyProxy {
        private int port;
        private int target;
        private String host;
        private boolean shutdown;

        private IoSession session;

        Proxy (int port, String host, int target) {
            this.port   = port;
            this.host   = host;
            this.target = target;
        }

        public void sessionCreated (IoSession session) {
            if (session.isReadSuspended() || session.isWriteSuspended()) {
                session.resumeRead();
                session.resumeWrite();
            }
            cachedProxies.put (port, this);
        }

        @Override
        public void sessionOpened (IoSession session) throws Exception {
            this.session = session;

            open ();
        }

        @Override
        public void exceptionCaught (IoSession session, Throwable cause) throws Exception {
            cause.printStackTrace ();

            if (!shutdown) {
                open ();
            }
        }

        @Override
        public void sessionClosed (IoSession session) throws Exception {
            if (!shutdown) {
                open ();
            }
        }

        @Override
        public void inputClosed (IoSession session) throws Exception {
            if (!shutdown) {
                open ();
            }
        }

        @Override
        public String toString () {
            return String.format ("{session=%s, local-port=%d, remote=%s:%d}", session, port, host, target);
        }

        private void shutdown () {
            shutdown = true;
            if (session != null) {
                session.closeNow ();

                if (logger.isTraceEnabled ()) {
                    logger.trace ("NAT {}:{}:{} shutdown.", port, host, target);
                }
            }
        }

        private void open () throws InterruptedException {
            if (session != null) {
                SocketConnector sc = new NioSocketConnector ();
                sc.setHandler (new MyProxy ());
                ConnectFuture cf = sc.connect (new InetSocketAddress (host, target));
                while (!cf.isConnected ()) {
                    Thread.sleep (10);
                }
                IoSession peer = cf.getSession ();
                session.setAttribute ("peer", peer);
                peer.setAttribute ("peer", session);

                if (logger.isTraceEnabled ()) {
                    logger.trace ("NAT {}:{}:{} bound.", port, host, target);
                }
            }
        }
    }

    private static class MyProxy extends IoHandlerAdapter {
        protected IoSession session;
        @Override
        public void messageReceived (IoSession session, Object message) {
            this.session = session;
            IoSession peer = (IoSession) session.getAttribute ("peer");

            if (peer != null) {
                IoBuffer buff = (IoBuffer) message;
                byte[] data = new byte[buff.limit ()];
                buff.get (data);
                peer.write (IoBuffer.wrap (data));
            }
        }
    }
}