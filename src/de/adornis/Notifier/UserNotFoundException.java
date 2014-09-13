package de.adornis.Notifier;

public class UserNotFoundException extends Exception {

	private String user = "";

	public UserNotFoundException(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public String getMessage() {
		return "User " + user + " wasn't found.";
	}
}
