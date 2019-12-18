package org.dreamwork.network.bridge;

import org.apache.mina.core.session.IoSession;
import org.dreamwork.network.bridge.tunnel.*;

import java.util.HashMap;
import java.util.Map;

import static org.dreamwork.network.bridge.util.Helper.bind;

/**
 * Created by seth.yang on 2019/10/31
 */
public class Test {
    private static int manage_port = 6667, tunnel_port = 6668;

    public static void main (String[] args) throws Exception {
        Map<String, IoSession>  managed_tunnels  = new HashMap<> ();
        Locks locks                              = new Locks ();

        // manager port
        bind (manage_port, new ManageHandler (managed_tunnels, locks));

        // tunnel port
        TunnelHandler th = new TunnelHandler (managed_tunnels);
        th.setLocks (locks);
        bind (tunnel_port, th);
    }
}