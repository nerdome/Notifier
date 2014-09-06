package de.adornis.Notifier;

import java.io.Serializable;

class TargetUser extends User implements Serializable {

	private String nick;
	private boolean online;

	public TargetUser(String JID, String nick) throws Exception {
		super(JID);
		setNick(nick);
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public String getNick() {
		return nick.equals("") ? username : nick;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}
}