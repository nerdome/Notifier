package de.adornis.Notifier;

public class InvalidCredentialsException extends InvalidJIDException {

	private String password = "";

	public InvalidCredentialsException(String user, String password) {
		super(user);
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
}
