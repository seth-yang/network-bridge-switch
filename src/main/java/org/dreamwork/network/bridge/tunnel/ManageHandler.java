package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.util.Helper;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by seth.yang on 2019/12/14
 */
public class ManageHandler extends IoHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger (ManageHandler.class);

    private Map<Integer, IoSession> managed_sessions = new TreeMap<> ();
    private Map<String, IoSession>  managed_tunnels;
    private Locks                   locks;

    private Map<String, Client> clients = new TreeMap<> ();

    public ManageHandler (Map<String, IoSession>  managed_tunnels, Locks locks) {
        this.managed_tunnels  = managed_tunnels;
        this.locks            = locks;
    }

    @Override
    public void messageReceived (IoSession session, Object message) throws Exception {
        IoBuffer buffer = (IoBuffer) message;
        int length = buffer.limit ();
        byte[] data = new byte[length];
        buffer.get (data);
        ByteArrayInputStream bais = new ByteArrayInputStream (data);
        DataInputStream dis = new DataInputStream (bais);
        int port = dis.readInt ();
        boolean blocked = dis.readBoolean ();
        String name = dis.readUTF ();

        if (logger.isTraceEnabled ()) {
            logger.trace ("receive a message: {name = {}, port = {}, blocked = {}}", name, port, blocked);
        }

        Client client = new Client (name, blocked, port);
        clients.put (name, client);
        session.setAttribute ("key", name);

        managed_sessions.put (port, session);

        FrontEndHandler feh = new FrontEndHandler (managed_sessions, port, blocked);
        feh.setManagedTunnels (managed_tunnels);
        feh.setLocks (locks);
        client.acceptor = Helper.bind (port, feh);
    }

    @Override
    public void sessionClosed (IoSession session) {
        String name = (String) session.getAttribute ("key");
        if (!StringUtil.isEmpty (name)) {
            if (clients.containsKey (name)) {
                Client client = clients.get (name);
                IoSession peer = managed_sessions.get (client.port);
                peer.closeNow ();
                managed_sessions.remove (client.port);
                clients.remove (client.name);

                client.acceptor.unbind ();
                logger.info ("tunnel {} unbind from port {}", client.name, client.port);
            }
        }
    }

    @Override
    public void exceptionCaught (IoSession session, Throwable cause) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("an error occurred in session: {}", session);
        }
        logger.warn (cause.getMessage (), cause);
        session.closeNow ();
    }

    public List<Client> getClients () {
        return new ArrayList<> (clients.values ());
    }

}