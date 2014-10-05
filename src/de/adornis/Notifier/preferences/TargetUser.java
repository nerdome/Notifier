package de.adornis.Notifier.preferences;

import android.content.Intent;
import de.adornis.Notifier.Notifier;
import org.jivesoftware.smack.packet.Presence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TargetUser extends User implements Serializable {

	public final static int ONLINE = 0;
	public final static int HALF_ONLINE = 1;
	public final static int OFFLINE = 2;
	public final static int NOT_IN_ROSTER = 3;
	public final static int NOT_CHECKED = 4;
	private int online = NOT_CHECKED;
	private String nick;
	private ArrayList<String> resources = new ArrayList<>();

	public TargetUser(String JID, String nick) throws InvalidJIDException {
		super(JID);
		setNick(nick);
	}

	public String getNick() {
		return nick.equals("") ? username : nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
		Notifier.getContext().sendBroadcast(new Intent(Notifier.USER_CHANGE).putExtra("JID", this.getJID()));
	}

	public int getOnlineStatus() {
		return online;
	}

	public void updatePresence() {
		online = NOT_CHECKED;
		resources.clear();
	}

	public void updatePresence(Presence presence) {
		String resource = presence.getFrom().substring(presence.getFrom().indexOf("/") + 1);

		if (presence.isAvailable() && !resources.contains(resource)) {
			resources.add(resource);
		} else if (!presence.isAvailable() && resources.contains(resource)) {
			resources.remove(resource);
		}

		updateOnline();
		Notifier.getContext().sendBroadcast(new Intent(Notifier.USER_CHANGE).putExtra("JID", this.getJID()));
	}

	public void updatePresence(List<Presence> presence) {
		updatePresence();
		for (Presence current : presence) {
			updatePresence(current);
		}
	}

	private void updateOnline() {
		if (resources.contains(Notifier.RESOURCE)) {
			online = ONLINE;
		} else if (!resources.isEmpty()) {
			online = HALF_ONLINE;
		} else {
			online = OFFLINE;
		}
	}

	public ArrayList<String> getResourceList() {
		return resources;
	}
}