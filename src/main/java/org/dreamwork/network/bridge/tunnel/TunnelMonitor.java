package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.dreamwork.concurrent.Looper;
import org.dreamwork.config.IConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class TunnelMonitor implements Runnable, ITunnelMonitor {
    private final Logger logger = LoggerFactory.getLogger (TunnelMonitor.class);
    private final Object LOCKER = new byte[0];
    private final Map<String, Client> clients = new ConcurrentHashMap<> ();

    private boolean running;
    private IConfiguration conf;

    void startWatching () {
        Looper.invokeLater (this);
    }

    void stopWatching () {
        running = false;
        if (logger.isTraceEnabled ()) {
            logger.trace ("set the running to false, and notify the monitor thread to stop working");
        }
        synchronized (LOCKER) {
            LOCKER.notifyAll ();
        }
    }

    void setConfiguration (IConfiguration configuration) {
        this.conf = configuration;
    }

    void addManager (String name, IoSession session, IoService service) {
        synchronized (clients) {
            Client client = new Client (name, session);
            client.port = ((NioSocketAcceptor) service).getLocalAddress ().getPort ();
            client.service = service;
            clients.put (name, client);
        }
        synchronized (LOCKER) {
            LOCKER.notifyAll ();
        }
        if (logger.isTraceEnabled ()) {
            logger.trace ("manager[{}] has added.", name);
        }
    }

    void removeManager (String name) {
        synchronized (clients) {
            Client client = clients.get (name);
            if (client != null) {
                client.service.dispose ();
                if (!client.tunnels.isEmpty ()) for (Pair p : client.tunnels.values ()) {
                    p.local.closeNow ();
                    p.peer.closeNow ();
                }
                client.tunnels.clear ();
                client.manager.closeNow ();

                clients.remove (name);
            }
        }
        synchronized (LOCKER) {
            LOCKER.notifyAll ();
        }
        if (logger.isTraceEnabled ()) {
            logger.trace ("manager[{}] has removed.", name);
        }
    }

    void addTunnel (String name, String token, IoSession local, IoSession peer) {
        synchronized (clients) {
            Client client = clients.get (name);
            if (client != null) {
                client.addTunnel (token, local, peer);
            }
        }
        if (logger.isTraceEnabled ()) {
            logger.trace ("tunnel[{}.{}] has added.", name, token);
        }
    }

    void removeTunnel (String name, String token) {
        synchronized (clients) {
            Client client = clients.get (name);
            if (client != null) {
                client.tunnels.remove (token);
            }
        }
        if (logger.isTraceEnabled ()) {
            logger.trace ("tunnel[{}.{}] has removed.", name, token);
        }
    }

    @Override
    public void run () {
        if (logger.isTraceEnabled ()) {
            logger.trace ("starting the tunnel monitor...");
        }

        Map<String, Client> copies = new HashMap<> ();

        while (running) {
            synchronized (clients) {
                while (running && clients.isEmpty ()) {
                    if (logger.isTraceEnabled ()) {
                        logger.trace ("there's no clients in cache, have a rest.");
                    }
                    if (running) {
                        synchronized (LOCKER) {
                            try {
                                LOCKER.wait ();
                            } catch (InterruptedException e) {
                                e.printStackTrace ();
                            }
                        }
                    } else if (logger.isTraceEnabled ()) {
                        logger.trace ("i waked up, but the the monitor is not running, ignore this round.");
                    }
                }

                if (running) {
                    if (logger.isTraceEnabled ()) {
                        logger.trace ("here, some clients present, get to work. copy them into the work memory");
                    }
                    copies.putAll (clients);
                }
            }

            if (running) try {
                long timeout = conf.getLong ("tunnel.manage.timeout", 90000);
                for (Map.Entry<String, Client> e : copies.entrySet ()) {
                    String name = e.getKey ();
                    Client client = e.getValue ();
                    Long timestamp = (Long) client.manager.getAttribute ("last.hop");
                    if (timestamp == null) {
                        logger.warn ("the session[{}] never hops. close it", name);
                        client.manager.closeNow ();
                        client.service.dispose ();

                        clients.remove (name);
                        continue;
                    }

                    long delta = System.currentTimeMillis () - timestamp;
                    if (delta > timeout) {
                        logger.warn ("the manager[{}] wait for {} ms, it's greater than timeout {} ms. close it", name, delta, timeout);
                        client.manager.closeNow ();
                        client.service.dispose ();

                        clients.remove (name);
                    }
                }
                if (logger.isTraceEnabled ()) {
                    logger.trace ("this round complete, wait for next hop");
                }

                synchronized (LOCKER) {
                    LOCKER.wait (60000);    // check the heartbeat per minute
                }
            } catch (Exception ex) {
                logger.warn (ex.getMessage (), ex);
            } finally {
                copies.clear ();
            }
        }
    }

    @Override
    public List<ClientInfo> getClientInfo () {
        List<ClientInfo> list = new ArrayList<> ();
        for (Client client : clients.values ()) {
            ClientInfo info = new ClientInfo ();
            info.name = client.name;
            info.port = client.port;
            info.remoteManagerAddress = client.manager.getRemoteAddress ();

            if (client.tunnels != null && !client.tunnels.isEmpty ()) {
                info.tunnels = new HashMap<> ();
                for (Map.Entry<String, Pair> e : client.tunnels.entrySet ()) {
                    TunnelInfo ti = new TunnelInfo ();
                    Pair p = e.getValue ();
                    ti.t = e.getKey ();
                    ti.a = p.local.getRemoteAddress ().toString ();
                    ti.z = p.peer.getRemoteAddress ().toString ();
                    info.tunnels.put (ti.t, ti);
                }
            }

            list.add (info);
        }
        list.sort (Comparator.comparingInt (o -> o.port));
        return list;
    }

    private static final class Client {
        int port;
        String name;
        IoSession manager;
        IoService service;
        Map<String, Pair> tunnels = new ConcurrentHashMap<> ();

        private Client (String name, IoSession session) {
            this.name    = name;
            this.manager = session;
        }

        private void addTunnel (String key, IoSession local, IoSession peer) {
            tunnels.put (key, new Pair (local, peer));
        }
    }

    private static final class Pair {
        IoSession local, peer;

        Pair (IoSession local, IoSession peer) {
            this.local = local;
            this.peer = peer;
        }
    }
}