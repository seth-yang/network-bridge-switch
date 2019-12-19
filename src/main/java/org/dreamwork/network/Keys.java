package org.dreamwork.network;

/**
 * Created by seth.yang on 2019/11/1
 */
public interface Keys {
    String PACKET_NAME              = "org.dreamwork.network.bridge.";

    String KEY_IN                   = PACKET_NAME + "KEY_IN";
    String KEY_OUT                  = PACKET_NAME + "KEY_OUT";
    String KEY_ERR                  = PACKET_NAME + "KEY_ERR";
    String KEY_PEER                 = PACKET_NAME + "KEY_PEER";
    String IS_PROXY_TYPE            = PACKET_NAME + "IS_PROXY_TYPE";
    String LAST_UPDATE_TIMESTAMP    = PACKET_NAME + "LAST_UPDATE_TIMESTAMP";

    String CFG_EXT_DIR              = "ext.conf.dir";
    String CFG_DB_FILE              = "database.file";
    String CFG_LOG_FILE             = "log.file";
    String CFG_LOG_LEVEL            = "log.level";
    String CFG_SSHD_PORT            = "service.sshd.port";
    String CFG_SSHD_CA_DIR          = "service.sshd.cert.file";
}
