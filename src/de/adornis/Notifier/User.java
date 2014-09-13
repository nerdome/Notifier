package de.adornis.Notifier;


import java.io.Serializable;

class User implements Serializable {
	protected final String JID;
	protected final String username;
	protected final String domain;

	// for the serializable
	public User() throws InvalidJIDException {
		throw new InvalidJIDException("");
	}

	public User(String JID) throws InvalidJIDException {
		this.JID = JID;
		if(JID.contains("@")) {
			username = JID.substring(0, JID.indexOf("@"));
			domain = JID.substring(JID.indexOf("@") + 1);
		} else {
			throw new InvalidJIDException(JID);
		}
	}

	public String getJID() {
		return JID;
	}
}