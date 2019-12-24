package org.dreamwork.network.bridge.tunnel.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Created by seth.yang on 2019/12/19
 */
public class ClientGuiMain {
    public static void main (String[] args) throws Exception {
        UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
        ResourceBundle bundle = ResourceBundle.getBundle ("ui.strings");


        if (SystemTray.isSupported ()) {
            UiConst.tray = new Tray (bundle);
            UiConst.tray.attach ();
        } else {
            MainFrame frame = new MainFrame (bundle);
            frame.pack ();
            frame.setVisible (true);
        }
    }
}