package org.dreamwork.network.bridge.tunnel.data;

public class Reply extends Command {
    public byte code = 0;
    public Reply () {
        super (REPLY);
    }

    @Override
    public String toString () {
        return "Reply [code = " + code + "]";
    }

    @Override
    public byte[] toByteArray () {
        return new byte[] {REPLY, code};
    }
}
