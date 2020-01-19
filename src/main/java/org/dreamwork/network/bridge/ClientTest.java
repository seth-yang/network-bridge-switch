package org.dreamwork.network.bridge;

import org.dreamwork.network.bridge.tunnel.ManagerClient;

/**
 * Created by seth.yang on 2019/12/18
 */
public class ClientTest {
    private static int manage_port = 50041, tunnel_port = 50042;
    public static void main (String[] args) {
        new ManagerClient ("test-manager", "120.26.97.130", manage_port, "192.168.2.80", 22) {
            @Override
            protected int getMappingPort () {
                return 50043;
            }
        }.setTunnelPort (tunnel_port).attach ();

    }
}
