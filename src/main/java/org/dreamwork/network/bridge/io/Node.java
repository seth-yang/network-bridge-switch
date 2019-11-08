package org.dreamwork.network.bridge.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by seth.yang on 2019/11/5
 */
public class Node {
    public static final class Input {
        InputStream in;
        OutputStream out, err;

        Input () {}

        public Input (InputStream in, OutputStream out, OutputStream err) {
            this.in = in;
            this.out = out;
            this.err = err;
        }
    }

    public static final class Output {
        OutputStream in;
        InputStream out, err;

        Output () {}

        public Output (OutputStream in, InputStream out, InputStream err) {
            this.in = in;
            this.out = out;
            this.err = err;
        }
    }
}
