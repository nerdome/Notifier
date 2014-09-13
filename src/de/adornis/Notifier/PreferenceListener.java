package de.adornis.Notifier;

abstract class PreferenceListener {

	public final static String CREDENTIALS = "de.adornis.Notifier.CREDENTIALS";
	public final static String USER_LIST = "de.adornis.Notifier.USER_LIST";
	public final static String STOP = "de.adornis.Notifier.STOP";

	abstract public void onPreferenceChanged(String type);
}