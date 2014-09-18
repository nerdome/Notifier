package de.adornis.Notifier;

import java.util.ArrayList;

abstract class PreferenceListener {

	public final static String CREDENTIALS = "de.adornis.Notifier.CREDENTIALS";
	public final static String USER_CHANGE = "de.adornis.Notifier.USER_CHANGE";
	public final static String USER_ADD_OR_REMOVE = "de.adornis.Notifier.USER_ADD_OR_REMOVE";
	public final static String STOP = "de.adornis.Notifier.STOP";
	public final static String SERVICE = "de.adornis.Notifier.SERVICE";
	public final static String USER_PROPOSE_LIST = "de.adornis.Notifier.USER_PROPOSE_LIST";
	public final static String USER_PROPOSE_ROSTER = "de.adornis.Notifier.USER_PROPOSE_ROSTER";
	public final static String UPDATE_AVAILABLE = "de.adornis.Notifier.UPDATE_AVAILABLE";

	private static ArrayList<PreferenceListener> pls = new ArrayList<>();

	public static void registerListener(PreferenceListener pl) {
		pls.add(pl);
	}

	public static void notifyAll(String type, String... JID) {
		for(PreferenceListener current : pls) {
			switch (type) {
				case USER_CHANGE:
					current.onUserChanged(JID);
					break;
				case USER_ADD_OR_REMOVE:
					current.onUserAdd(JID);
					break;
				case USER_PROPOSE_LIST:
					current.onUserProposeList(JID[0]);
					break;
				case USER_PROPOSE_ROSTER:
					current.onUserProposeRoster(JID[0]);
					break;
				case CREDENTIALS:
					current.onCredentialsChanged();
					break;
				case STOP:
					current.onStopCommand();
					break;
				case SERVICE:
					current.onServiceStateChanged();
					break;
				case UPDATE_AVAILABLE:
					current.onUpdateAvailable();
					break;
			}
		}
	}

	abstract public void onUserAdd(String... JID);

	abstract public void onUserProposeList(String JID);

	abstract public void onUserProposeRoster(String JID);

	abstract public void onCredentialsChanged();

	abstract public void onUserChanged(String... JID);

	abstract public void onStopCommand();

	abstract public void onServiceStateChanged();

	abstract public void onUpdateAvailable();
}