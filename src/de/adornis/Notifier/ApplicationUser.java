package de.adornis.Notifier;

class ApplicationUser extends User {

	private final String password;

	public ApplicationUser(String user, String password, String domain) throws InvalidJIDException {
		super(user, domain);
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	@Deprecated
	public String getJID() {
		return super.getJID();
	}
}