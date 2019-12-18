package org.dreamwork.network.bridge.tunnel;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by seth.yang on 2019/12/18
 */
public class Locks {
    private final Map<String, Object> locks = new TreeMap<> ();

    void add (String key, Object token) {
        synchronized (locks) {
            locks.put (key, token);
        }
    }

    void await (String key) throws InterruptedException {
        if (locks.containsKey (key)) {
            synchronized (locks.get (key)) {
                locks.get (key).wait ();
            }
        } else {
            throw new InterruptedException ("can't lock on key: " + key);
        }
    }

    void await (String key, long timeout) throws InterruptedException {
        if (locks.containsKey (key)) {
            synchronized (locks.get (key)) {
                locks.get (key).wait (timeout);
            }
        } else {
            throw new InterruptedException ("can't lock on key: " + key);
        }
    }

    void notify (String key) {
        if (locks.containsKey (key)) {
            synchronized (locks.get (key)) {
                locks.get (key).notifyAll ();
            }
        }
    }

    void release (String key) {
        synchronized (locks) {
            locks.remove (key);
        }
    }
}
