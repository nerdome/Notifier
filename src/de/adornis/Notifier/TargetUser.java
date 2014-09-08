package de.adornis.Notifier;

import java.io.Serializable;

class TargetUser extends User implements Serializable {

	private String nick;
	private int online = NOT_IN_ROSTER;

	public final static int ONLINE = 0;
	public final static int OFFLINE = 1;
	public final static int NOT_IN_ROSTER = 2;

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

	public int isOnline() {
		return online;
	}

	public void setOnline(int online) {
		this.online = online;
	}
}