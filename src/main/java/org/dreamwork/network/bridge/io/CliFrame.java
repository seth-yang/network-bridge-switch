package org.dreamwork.network.bridge.io;

import org.dreamwork.cli.text.Alignment;
import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.telnet.ConnectionData;
import org.dreamwork.telnet.Console;
import org.dreamwork.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by seth.yang on 2019/11/29
 */
public class CliFrame extends Console {
    private String banner;

    public CliFrame (InputStream in, OutputStream out, ConnectionData data, boolean ssh) {
        super (in, out, data, ssh);
    }

    public CliFrame (InputStream in, OutputStream out, ConnectionData data) {
        super (in, out, data);
    }

    public CliFrame (InputStream in, OutputStream out, ConnectionData data, int buffSize) {
        super (in, out, data, buffSize);
    }

    public CliFrame (InputStream in, OutputStream out, ConnectionData data, boolean ssh, int buffSize) {
        super (in, out, data, ssh, buffSize);
    }

    @Override
    public void loop () throws IOException {
        clear ();

        showBanner ();
        showBody ();
        showFooter ();
    }

    private void showBanner () throws IOException {
        if (!StringUtil.isEmpty (banner)) {
            println (banner);
        }
    }

    private void showBody () throws IOException {
        showHeader ();
        showData ();
    }

    private void showFooter () {

    }

    private void showHeader () throws IOException {
        for (int i = 0; i < header.length; i ++) {
            FrameColumn fc = header[i];
            if (width[i] < fc.name.length ()) {
                width[i] = fc.name.length ();
            }
        }
        if (model != null && model.getRows () > 0) {
            for (int r = 0; r < model.getRows (); r ++) {
                for (int c = 0; c < header.length; c ++) {
                    Object o = model.valueAt (r, c);
                    if (o != null) {
                        String tmp = String.valueOf (o);
                        if (width [c] < tmp.length ()) {
                            width [c] = tmp.length ();
                        }
                    }
                }
            }
        }
        for (int i = 0; i < header.length - 1; i ++) {
            FrameColumn fc = header[i];
            write (TextFormater.fill (fc.name, ' ', width[i], fc.alignment));
            moveUp (2);
        }
        println (header [header.length - 1].name);
    }

    private void showData () throws IOException {
        if (model != null && model.getRows () > 0) {
            for (int r = 0; r < model.getRows (); r ++) {
                for (int c = 0; c < header.length - 1; c ++) {
                    Object o = model.valueAt (r, c);
                    if (o == null) {
                        moveLeft (width[c] + 2);
                    } else {
                        String tmp = String.valueOf (o);
                        write (TextFormater.fill (tmp, ' ', width[c], header[c].alignment));
                    }
                }
                Object o = model.valueAt (r, header.length - 1);
                if (o == null) {
                    println ();
                } else {
                    println (String.valueOf (o));
                }
            }
        }
    }

    private void showLineData () {}

    private int[] width;

    private FrameColumn[] header;
    private FrameModel model;

    public static final class FrameColumn {
        String name;
        char   acce;
        Alignment alignment;
    }

    public abstract static class FrameModel {
        abstract protected int getRows ();

        abstract protected<T> T valueAt (int row, int column);
    }
}