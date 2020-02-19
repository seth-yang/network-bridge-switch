package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.tunnel.data.Command;
import org.dreamwork.network.bridge.tunnel.data.CreationCommand;
import org.dreamwork.network.bridge.tunnel.data.Reply;
import org.dreamwork.network.bridge.util.Helper;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private TunnelMonitor           monitor;

    private Map<String, Client> clients = new TreeMap<> ();

    public ManageHandler (Map<String, IoSession>  managed_tunnels, Locks locks) {
        this.managed_tunnels  = managed_tunnels;
        this.locks            = locks;
    }

    public void setTunnelMonitor (TunnelMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void messageReceived (IoSession session, Object message) throws Exception {
        Command cmd = (Command) message;
        switch (cmd.command) {
            case Command.CREATION:  // require create a tunnel
                logger.debug ("receive a creation command in session: {}", session);
                bind ((CreationCommand) message, session);
                // reply to client
                logger.debug ("write reply to session: {}", session);
                session.write (new Reply ());
                break;
            case Command.HEARTBEAT:  // heartbeat
                // here we got the client's heartbeat
                if (logger.isTraceEnabled ()) {
                    logger.trace ("got a heartbeat from client [{}], reply it back", session.getAttribute ("tunnel.name"));
                }
                session.write (message);
                break;
            case Command.CLOSE:  // closing
                session.closeNow ();
                break;
        }
    }

    @Override
    public void sessionClosed (IoSession session) {
        String name = (String) session.getAttribute ("tunnel.name");
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

            if (monitor != null) {
                monitor.removeManager (name);
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

    private void bind (CreationCommand creation, IoSession session) throws IOException {
        if (logger.isTraceEnabled ()) {
            logger.trace ("got a creation: {");
            logger.trace ("    name    = {}", creation.name);
            logger.trace ("    port    = {}", creation.port);
            logger.trace ("    blocked = {}", creation.blocked);
            logger.trace ("}");
        }

        Client client = new Client (creation.name, creation.blocked, creation.port);
        clients.put (creation.name, client);
        session.setAttribute ("tunnel.name", creation.name);
        session.setAttribute ("mapped.port", creation.port);
        managed_sessions.put (creation.port, session);

        FrontEndHandler feh = new FrontEndHandler (managed_sessions, creation.port, creation.blocked);
        feh.setTunnelName (creation.name);
        feh.setMonitor (monitor);
        feh.setManagedTunnels (managed_tunnels);
        feh.setLocks (locks);
        client.acceptor = Helper.bind (creation.port, feh);

        monitor.addManager (creation.name, session, client.acceptor);

        logger.info ("tunnel manager [{}] listened on {}", creation.name, creation.port);
    }
}