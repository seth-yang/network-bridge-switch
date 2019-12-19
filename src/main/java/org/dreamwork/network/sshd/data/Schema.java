package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.DeviceSchema;
import org.dreamwork.network.sshd.data.schema.NATSchema;
import org.dreamwork.network.sshd.data.schema.UserSchema;
import org.dreamwork.persistence.DatabaseSchema;

public class Schema {
	public static void registerAllSchemas () {
		DatabaseSchema.register(UserSchema.class);
		DatabaseSchema.register(DeviceSchema.class);
		DatabaseSchema.register (NATSchema.class);
	}
}
