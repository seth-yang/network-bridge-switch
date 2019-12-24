package org.dreamwork.network.bridge.tunnel.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

/**
 * Created by seth.yang on 2019/12/19
 */
public class MainFrame extends JFrame {
    private JPanel root;
    private JList lstServers;
    private JTable tblMain;
    private JButton button1;

    private ResourceBundle bundle;

    public MainFrame (ResourceBundle bundle) {
        this.bundle = bundle;
        addWindowListener (new WindowAdapter () {
            @Override
            public void windowOpened (WindowEvent e) {
                UiConst.frame = MainFrame.this;
            }

            @Override
            public void windowClosed (WindowEvent e) {
                UiConst.frame = null;
            }
        });

        guiSetup ();

        if (UiConst.tray != null) {
            setDefaultCloseOperation (DISPOSE_ON_CLOSE);
        } else {
            setDefaultCloseOperation (EXIT_ON_CLOSE);
        }
    }

    private void guiSetup () {
        setTitle (bundle.getString ("main.title"));
        setIconImage (Helper.getImage ("images/64x64/route.png"));

        setContentPane (root);
    }
}