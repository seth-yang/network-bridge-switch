package org.dreamwork.network.bridge.tunnel.data;

public abstract class Command {
    public static final int CREATION    = 0x01;
    public static final int HEARTBEAT   = 0x02;
    public static final int CLOSE       = 0x03;
    public static final int TOKEN       = 0x04;
    public static final int REPLY       = 0x05;

    public final int command;

    public Command (int command) {
        this.command = command;
    }

    abstract public byte[] toByteArray ();
}