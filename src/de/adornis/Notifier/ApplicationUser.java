package de.adornis.Notifier;

class ApplicationUser extends User {

	private String password;

	public ApplicationUser(String JID, String password) throws Exception {
		super(JID);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

}