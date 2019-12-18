package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dreamwork.network.bridge.util.Helper.bind;

/**
 * Created by seth.yang on 2019/12/18
 */
public class TunnelManager {
    private static ManageHandler handler;
    private static NioSocketAcceptor[] acceptors = new NioSocketAcceptor[2];

    public static void start (int manage_port, int tunnel_port) throws IOException {
        Map<String, IoSession> managed_tunnels  = new HashMap<> ();
        Locks locks                              = new Locks ();

        // manager port
        handler = new ManageHandler (managed_tunnels, locks);
        acceptors[0] = bind (manage_port, handler);

        // tunnel port
        TunnelHandler th = new TunnelHandler (managed_tunnels);
        th.setLocks (locks);
        acceptors[1] = bind (tunnel_port, th);
    }

    public static void stop () {
        if (acceptors[0] != null) {
            acceptors[0].unbind ();
        }

        if (acceptors[1] != null) {
            acceptors[1].unbind ();
        }
    }

    public static List<Client> getClients () {
        return handler == null ? Collections.emptyList () : handler.getClients ();
    }
}
