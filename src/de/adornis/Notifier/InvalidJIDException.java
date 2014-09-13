package de.adornis.Notifier;

public class InvalidJIDException extends Exception {
	private String JID = "";

	public InvalidJIDException(String JID) {
		this.JID = JID;
	}

	public String getJID() {
		return JID;
	}
}