package org.dreamwork.network.bridge;

/**
 * Created by seth.yang on 2019/11/1
 */
public interface Keys {
    String PACKET_NAME              = "org.dreamwork.network.bridge.";

    String KEY_IN                   = PACKET_NAME + "KEY_IN";
    String KEY_OUT                  = PACKET_NAME + "KEY_OUT";
    String KEY_ERR                  = PACKET_NAME + "KEY_ERR";
    String IS_PROXY_TYPE            = PACKET_NAME + "IS_PROXY_TYPE";
    String LAST_UPDATE_TIMESTAMP    = PACKET_NAME + "LAST_UPDATE_TIMESTAMP";
}
