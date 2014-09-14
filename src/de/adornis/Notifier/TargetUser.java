package de.adornis.Notifier;

import org.jivesoftware.smack.packet.Presence;

import java.io.Serializable;
import java.util.ArrayList;

class TargetUser extends User implements Serializable {

	private String nick;
	private int online = NOT_CHECKED;

	public final static int ONLINE = 0;
	public final static int HALF_ONLINE = 1;
	public final static int OFFLINE = 2;
	public final static int NOT_IN_ROSTER = 3;
	public final static int NOT_CHECKED = 4;

	private ArrayList<String> resources = new ArrayList<>();

	public TargetUser(String JID, String nick) throws InvalidJIDException {
		super(JID);
		setNick(nick);
	}

	public void setNick(String nick) {
		this.nick = nick;
		Preferences.notifyChanged(PreferenceListener.USER_LIST);
	}

	public String getNick() {
		return nick.equals("") ? username : nick;
	}

	public int isOnline() {
		return online;
	}

	public void updatePresence(Presence presence) {
		if(presence != null) {
			String resource = presence.getFrom().substring(presence.getFrom().indexOf("/") + 1);

			if (presence.isAvailable() && !resources.contains(resource)) {
				resources.add(resource);
			} else if (!presence.isAvailable() && resources.contains(resource)) {
				resources.remove(resource);
			}

			updateOnline();
		} else {
			online = NOT_CHECKED;
		}
		Preferences.notifyChanged(PreferenceListener.USER_LIST);
	}

	private void updateOnline() {
		if(resources.contains("NOTIFIER_RECEIVER")) {
			online = ONLINE;
		} else if(!resources.isEmpty()) {
			online = HALF_ONLINE;
		} else {
			online = OFFLINE;
		}

	}

	public void incomingPing() {
		resources.add("NOTIFIER_RECEIVER");
		updateOnline();
		Preferences.notifyChanged(PreferenceListener.USER_LIST);
	}
}