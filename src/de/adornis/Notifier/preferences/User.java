package de.adornis.Notifier.preferences;


import java.io.Serializable;

public class User implements Serializable {
	protected final String JID;
	protected final String username;
	protected final String domain;

	// for the serializable
	public User() throws InvalidJIDException {
		throw new InvalidJIDException("");
	}

	public User(String JID) throws InvalidJIDException {
		this.JID = JID;
		if (JID.contains("@") && JID.contains(".")) {
			username = JID.substring(0, JID.indexOf("@"));
			domain = JID.substring(JID.indexOf("@") + 1);
		} else {
			throw new InvalidJIDException(JID);
		}
	}

	public User(String user, String domain) throws InvalidJIDException {
		if (domain.contains(".")) {
			this.username = user;
			this.domain = domain;
			this.JID = user + "@" + domain;
		} else {
			throw new InvalidJIDException(user + "@" + domain);
		}
	}

	public String getJID() {
		return JID;
	}

	public String getUsername() {
		return username;
	}

	public String getDomain() {
		return domain;
	}

	public static class InvalidJIDException extends Exception {
		private String JID = "";

		public InvalidJIDException(String JID) {
			this.JID = JID;
		}

		public String getJID() {
			return JID;
		}
	}
}