package org.dreamwork.network.bridge.tunnel.data;

import org.dreamwork.util.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Heartbeat extends Command {
    public String name;

    public Heartbeat () {
        super (HEARTBEAT);
    }

    @Override
    public String toString () {
        return "HeartbeatCommand [name=" + name + ']';
    }

    @Override
    public byte[] toByteArray () {
        if (StringUtil.isEmpty (name)) {
            return new byte[] {HEARTBEAT};
        }

        byte[] tmp  = name.getBytes (StandardCharsets.UTF_8);
        byte[] data = new byte[33];
        Arrays.fill (data, (byte) ' ');
        data[0] = HEARTBEAT;
        System.arraycopy (tmp, 0, data, 1, tmp.length);
        return data;
    }
}