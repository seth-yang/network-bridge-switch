package org.dreamwork.network.bridge.data;

import org.dreamwork.network.bridge.data.schema.DeviceSchema;
import org.dreamwork.persistence.*;

@ISchema (DeviceSchema.class)
public class Device {
	@ISchemaField (id = true, name = "id", autoincrement = true)
	private int id;

	@ISchemaField (name = "name")
	private String name;

	@ISchemaField (name = "host")
	private String host;

	@ISchemaField (name = "port")
	private int port;

	@ISchemaField (name = "user")
	private String user;

	@ISchemaField (name = "password")
	private String password;

	public int getId () {
		return id;
	}

	public void setId (int id) {
		this.id = id;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public String getHost () {
		return host;
	}

	public void setHost (String host) {
		this.host = host;
	}

	public int getPort () {
		return port;
	}

	public void setPort (int port) {
		this.port = port;
	}

	public String getUser () {
		return user;
	}

	public void setUser (String user) {
		this.user = user;
	}

	public String getPassword () {
		return password;
	}

	public void setPassword (String password) {
		this.password = password;
	}

	@Override
	public boolean equals (java.lang.Object o) {
		if (this == o) return true;
		if (o == null || getClass () != o.getClass ()) return false;
		Device that = (Device) o;
		return id == that.id;
	}

	@Override
	public int hashCode () {
		return String.valueOf (id).hashCode ();
	}
}