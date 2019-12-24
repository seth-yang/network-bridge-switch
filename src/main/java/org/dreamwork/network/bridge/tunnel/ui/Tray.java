package org.dreamwork.network.bridge.tunnel.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Created by seth.yang on 2019/12/19
 */
public class Tray {
    private ResourceBundle bundle;

    private ActionListener al = e -> {
        if (UiConst.frame != null) {
            UiConst.frame.toFront ();
        } else {
            MainFrame frame = new MainFrame (bundle);
            frame.pack ();
            frame.setVisible (true);
        }
    };

    public Tray (ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public void attach () throws AWTException {
        PopupMenu popup = new PopupMenu ();

        MenuItem mi = new MenuItem (bundle.getString ("tray.mi.restore"));
        mi.addActionListener (al);
        popup.add (mi);
        popup.addSeparator ();

        mi = new MenuItem (bundle.getString ("global.label.exit"));
        mi.addActionListener (e-> System.exit (0));
        popup.add (mi);

        TrayIcon icon = new TrayIcon (Helper.getImage ("images/16x16/route.png"), bundle.getString ("main.title"));
        icon.addActionListener (al);
        icon.setPopupMenu (popup);
        SystemTray.getSystemTray ().add (icon);
    }
}