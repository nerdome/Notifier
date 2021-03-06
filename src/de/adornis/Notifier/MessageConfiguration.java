package de.adornis.Notifier;

import android.widget.TextView;
import de.adornis.Notifier.Preferences.UserNotFoundException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

public class MessageConfiguration {

	private String receiver = null;
	private String resource = Notifier.RESOURCE;
	private String message = null;
	private long delay = 0;
	private TextView countdownView;

	public MessageConfiguration(String receiver, String message) {
		this.receiver = receiver;
		this.message = message;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setDelay(int delay, TextView countdownView) {
		this.delay = delay * 1000;
		this.countdownView = countdownView;
	}

	public long getDelay() {
		return delay;
	}

	public void tickInActivity(final int number) {
		countdownView.post(new Runnable() {
			@Override
			public void run() {
				if (number != 0) {
					countdownView.setText(number + "");
				} else {
					countdownView.setText("");
				}
			}
		});
	}

	public Message getMessage() throws UserNotFoundException {
		Message msg = new Message();
		msg.setTo(receiver + "/" + resource);
		msg.setBody(Preferences.getAppUser().getJID() + " sent you this message via Notifier: " + message + "<br>This is an alarm notification :: NOTIFIER App for Android");
		JivePropertiesManager.addProperty(msg, "ALARM", message);
		return msg;
	}
}