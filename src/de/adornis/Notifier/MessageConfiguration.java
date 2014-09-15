package de.adornis.Notifier;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

public class MessageConfiguration {

	private String receiver = null;
	private String resource = "NOTIFIER_RECEIVER";
	private String message = null;
	private long delay = 0;

	public MessageConfiguration(String receiver, String message) {
		this.receiver = receiver;
		this.message = message;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getDelay() {
		return delay;
	}

	public Message getMessage(Preferences prefs) {
		Message msg = new Message();
		msg.setTo(receiver + "/" + resource);
		msg.setBody(prefs.getAppUser().getJID() + " sent you this message via Notifier: " + message + "<br>This is an alarm notification :: NOTIFIER App for Android");
		JivePropertiesManager.addProperty(msg, "ALARM", message);
		return msg;
	}
}