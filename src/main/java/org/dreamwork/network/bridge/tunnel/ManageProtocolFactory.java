package org.dreamwork.network.bridge.tunnel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;
import org.dreamwork.network.bridge.tunnel.data.*;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ManageProtocolFactory implements ProtocolCodecFactory {
    private ProtocolEncoder encoder = new ProtocolEncoderAdapter () {
        private Logger logger = LoggerFactory.getLogger (getClass ());

        @Override
        public void encode (IoSession session, Object message, ProtocolEncoderOutput out) {
            if (message != null) {
                Command cmd = (Command) message;

                if (logger.isTraceEnabled ()) {
                    logger.trace ("trying to encoding data: \r\n{}", StringUtil.format (cmd.toByteArray ()));
                }

                IoBuffer buffer;
                switch (cmd.command) {
                    case Command.CREATION:
                        CreationCommand creation = (CreationCommand) message;
                        byte[] src = creation.name.getBytes (StandardCharsets.UTF_8);
                        int length = Math.min (src.length, 32);
                        // 1 byte command + 2 bytes port + 1 byte block + 32 bytes name
                        buffer = IoBuffer.allocate (36);
                        buffer.put ((byte) cmd.command);
                        buffer.putShort ((short) creation.port);
                        buffer.put ((byte) (creation.blocked ? 1 : 0));
                        buffer.put (src, 0, length);
                        buffer.fill ((byte) ' ', 32 - length);

                        buffer.flip ();
                        out.write (buffer);
                        break;

                    case Command.HEARTBEAT:  // heartbeat
                        Heartbeat heartbeat = (Heartbeat) message;
                        byte[] tmp = heartbeat.toByteArray ();
                        buffer = IoBuffer.allocate (tmp.length);
                        buffer.put (tmp);

                        buffer.flip ();
                        out.write (buffer);
                        break;

                    case Command.CLOSE:  // require closing the tunnel
                        break;

                    case Command.TOKEN:  // token transport
                        TokenCommand token = (TokenCommand) message;
                        buffer = IoBuffer.allocate (7);
                        buffer.put ((byte) token.command);
                        buffer.put (token.token);

                        buffer.flip ();
                        out.write (buffer);
                        break;

                    case Command.REPLY:     // the reply
                        buffer = IoBuffer.allocate (2);
                        buffer.put ((byte) Command.REPLY);
                        buffer.put ((byte) 0);

                        buffer.flip ();
                        out.write (buffer);
                        break;
                }
            }
        }
    };

    private ProtocolDecoder decoder = new CumulativeProtocolDecoder () {
        private Logger logger = LoggerFactory.getLogger (getClass ());

        @Override
        protected boolean doDecode (IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
            Integer command = (Integer) session.getAttribute ("cmd");

            if (command == null && in.remaining () >= 1) {
                command = in.get () & 0xff;
            }

            if (logger.isTraceEnabled ()) {
                logger.debug ("==============================================");
                logger.trace ("the command = {}", command);
                logger.debug ("==============================================");
            }

            if (command != null) {
                switch (command) {
                    case Command.CREATION:  // require a new tunnel
                        // the mapped port                      2
                        // block the tunnel or not              1
                        // the tunnel name                     32
                        if (in.remaining () >= 35) {
                            CreationCommand creation = new CreationCommand ();
                            creation.port = in.getUnsignedShort ();
                            creation.blocked = in.get () != 0;
                            byte[] buff = new byte[32];
                            in.get (buff);
                            creation.name = new String (buff).trim ();

                            session.removeAttribute ("cmd");
                            out.write (creation);

                            if (logger.isTraceEnabled ()) {
                                logger.trace ("decoded command: {}", creation);
                            }

                            return true;
                        }
                        break;

                    case Command.HEARTBEAT:  // heartbeat
                        if (in.remaining () >= 32) {
                            Heartbeat heartbeat = new Heartbeat ();
                            byte[] tmp = new byte[32];
                            in.get (tmp);
                            heartbeat.name = new String (tmp, StandardCharsets.UTF_8).trim ();

                            session.removeAttribute ("cmd");
                            out.write (heartbeat);

                            if (logger.isTraceEnabled ()) {
                                logger.trace ("decoded command: {}", heartbeat);
                            }

                            return true;
                        }
                        break;

                    case Command.CLOSE:  // require close a tunnel
                        if (logger.isTraceEnabled ()) {
                            logger.trace ("we got a close command");
                        }
                        CloseCommand cc = new CloseCommand ();
                        cc.name = (String) session.getAttribute ("name");
                        session.removeAttribute ("cmd");
                        out.write (cc);
                        return true;

                    case Command.TOKEN:  // token transport
                        if (in.remaining () >= 6) {
                            TokenCommand token = new TokenCommand ();
                            token.token = new byte[6];
                            in.get (token.token);

                            session.removeAttribute ("cmd");
                            out.write (token);

                            if (logger.isTraceEnabled ()) {
                                logger.trace ("decoded command: {}", token);
                            }
                            return true;
                        }

                        break;

                    case Command.REPLY:
                        Reply reply = new Reply ();
                        session.removeAttribute ("cmd");
                        out.write (reply);

                        if (logger.isTraceEnabled ()) {
                            logger.trace ("decoded command: {}", reply);
                        }

                        return true;
                }
            }

            if (logger.isTraceEnabled ()) {
                logger.trace ("decode not complete");
            }
            return false;
        }
    };

    @Override
    public ProtocolEncoder getEncoder (IoSession session) {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder (IoSession session) {
        return decoder;
    }
}