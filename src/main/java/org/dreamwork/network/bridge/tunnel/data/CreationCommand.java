package org.dreamwork.network.bridge.tunnel.data;

import org.dreamwork.network.sshd.data.SystemConfig;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CreationCommand extends Command {
    public int port;
    public String name;
    public boolean blocked;

    public CreationCommand () {
        super (CREATION);
    }

    @Override
    public String toString () {
        return String.format ("CreationCommand [name=%s, port=%d, blocked=%s]", name, port, blocked);
    }

    @Override
    public byte[] toByteArray () {
        byte[] data = new byte[37];
        Arrays.fill (data, (byte) ' ');
        data [0] = CREATION;
        data [1] = (byte) (port >> 8);
        data [2] = (byte) (port & 0xff);

        if (name != null) {
            byte[] tmp = name.getBytes (StandardCharsets.UTF_8);
            System.arraycopy (tmp, 0, data, 3, tmp.length);
        }

        return data;
    }
}