package org.dreamwork.network.bridge.tunnel.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Created by seth.yang on 2019/12/19
 */
public class Helper {
    public static Image getImage (String uri) {
        URL in = Helper.class.getClassLoader ().getResource (uri);
        if (in == null) {
            throw new NullPointerException ();
        }

        ImageIcon icon = new ImageIcon (in);
        return icon.getImage ();
    }
}
