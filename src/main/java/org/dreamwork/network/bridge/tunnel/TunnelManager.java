package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.dreamwork.app.bootloader.ApplicationBootloader;

import java.io.IOException;
import java.util.*;

import static org.dreamwork.network.bridge.util.Helper.bind;

/**
 * Created by seth.yang on 2019/12/18
 */
public class TunnelManager {
    private static ManageHandler handler;
    private static NioSocketAcceptor[] acceptors = new NioSocketAcceptor[2];
    private static boolean running = false;
    private static TunnelMonitor monitor = new TunnelMonitor ();

    private static final Map<String, IoSession> managed_tunnels = new HashMap<> ();

    public static void start (int manage_port, int connector_port) throws IOException {
        Locks locks = new Locks ();
        monitor.setConfiguration (ApplicationBootloader.getRootConfiguration ());
        monitor.startWatching ();

        // manager port
        ProtocolCodecFilter filter  = new ProtocolCodecFilter (new ManageProtocolFactory ());
        handler = new ManageHandler (managed_tunnels, locks);
        handler.setTunnelMonitor (monitor);
        acceptors[0] = bind (manage_port, handler, new String[] {"protocol"}, new IoFilter[] { filter });

        // tunnel port
        TunnelConnectorHandler th = new TunnelConnectorHandler (managed_tunnels);
        th.setLocks (locks);
        th.setTunnelMonitor (monitor);
        acceptors[1] = bind (connector_port, th);

        running = true;
    }

    public static void stop () {
        if (acceptors[0] != null) {
            acceptors[0].unbind ();
        }

        if (acceptors[1] != null) {
            acceptors[1].unbind ();
        }

        if (monitor != null) {
            monitor.stopWatching ();
        }
        running = false;
    }

    public static List<Client> getClients () {
        return handler == null ? Collections.emptyList () : handler.getClients ();
    }

    public static boolean isRunning () {
        return running;
    }
}