package org.dreamwork.network.bridge.tunnel.data;

import org.dreamwork.util.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CloseCommand extends Command {
    public String name;

    public CloseCommand () {
        super (Command.CLOSE);
    }

    @Override
    public String toString () {
        return "CloseCommand [name=" + name + ']';
    }

    @Override
    public byte[] toByteArray () {
        if (StringUtil.isEmpty (name)) {
            return new byte[] {HEARTBEAT};
        }

        byte[] tmp  = name.getBytes (StandardCharsets.UTF_8);
        byte[] data = new byte[33];
        data[0] = CLOSE;
        Arrays.fill (data, (byte) ' ');
        System.arraycopy (tmp, 0, data, 1, tmp.length);
        return data;
    }
}