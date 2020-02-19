package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.session.IoSession;

import java.util.Map;

public class HeartbeatMonitor implements Runnable {
    private Map<Integer, IoSession> managed_sessions;

    public HeartbeatMonitor (Map<Integer, IoSession> managed_sessions) {
        this.managed_sessions = managed_sessions;
    }

    @Override
    public void run () {

    }
}
