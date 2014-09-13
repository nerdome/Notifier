package de.adornis.Notifier;

class ApplicationUser extends User {

	private final String password;

	public ApplicationUser(String JID, String password) throws InvalidJIDException {
		super(JID);
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

}