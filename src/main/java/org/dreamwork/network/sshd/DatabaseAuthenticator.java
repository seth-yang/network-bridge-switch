package org.dreamwork.network.sshd;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.dreamwork.misc.AlgorithmUtil;
import org.dreamwork.network.Context;
import org.dreamwork.network.sshd.data.User;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by seth.yang on 2019/11/6
 */
public class DatabaseAuthenticator implements PasswordAuthenticator {
    private final Logger logger = LoggerFactory.getLogger (DatabaseAuthenticator.class);
    @Override
    public boolean authenticate (String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
        User user = Context.db.getByPK (User.class, username);
        if (user != null) {
            try {
                String md5 = StringUtil.dump (AlgorithmUtil.md5 (password.getBytes ())).toLowerCase ();
                return md5.equals (user.getPassword ());
            } catch (Exception ex) {
                logger.warn (ex.getMessage (), ex);
            }
        }
        return false;
    }
}