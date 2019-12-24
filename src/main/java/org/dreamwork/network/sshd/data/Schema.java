package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.*;
import org.dreamwork.persistence.DatabaseSchema;

public class Schema {
	public static void registerAllSchemas () {
		DatabaseSchema.register (UserSchema.class);
		DatabaseSchema.register (DeviceSchema.class);
		DatabaseSchema.register (NATSchema.class);
		DatabaseSchema.register (SystemConfigSchema.class);
		DatabaseSchema.register (TunnelClientSchema.class);
	}
}
