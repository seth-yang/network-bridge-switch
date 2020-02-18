package org.dreamwork.network.bridge.tunnel.data;

import org.dreamwork.util.StringUtil;

public class TokenCommand extends Command {
    public byte[] token;
    public TokenCommand () {
        super (TOKEN);
    }

    @Override
    public String toString () {
        return "TokenCommand [token=" + StringUtil.byte2hex (token, false) + ']';
    }

    @Override
    public byte[] toByteArray () {
        byte[] buff = new byte[7];
        buff[0] = HEARTBEAT;
        if (token != null) {
            System.arraycopy (token, 0, buff, 1, token.length);
        }
        return buff;
    }
}
