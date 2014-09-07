package de.adornis.Notifier;


import java.io.Serializable;

class User implements Serializable {
	protected String JID;
	protected String username;
	protected String domain;

	public User() throws Exception {
		throw new Exception();
	}

	public User(String JID) throws Exception {
		setJID(JID);
	}

	public void setJID(String JID) throws Exception {
		this.JID = JID;
		if(JID.contains("@")) {
			username = JID.substring(0, JID.indexOf("@"));
			domain = JID.substring(JID.indexOf("@") + 1);
		} else {
			throw new Exception();
		}
	}

	public String getJID() {
		return JID;
	}
}