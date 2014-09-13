package de.adornis.Notifier;


import java.io.Serializable;

class User implements Serializable {
	protected String JID;
	protected String username;
	protected String domain;

	// for the serializable
	public User() throws InvalidJIDException {
		throw new InvalidJIDException("");
	}

	public User(String JID) throws InvalidJIDException {
		setJID(JID);
	}

	public void setJID(String JID) throws InvalidJIDException {
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