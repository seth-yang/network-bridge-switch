package org.dreamwork.network.bridge.tunnel;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

public interface ITunnelMonitor {
    final class ClientInfo {
        public int port;
        public String name;
        public SocketAddress remoteManagerAddress;
        public Map<String, TunnelInfo> tunnels;
    }

    final class TunnelInfo {
        public String a, z, t;
    }

    List<ClientInfo> getClientInfo ();
}
