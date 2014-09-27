package de.adornis.Notifier;

import android.os.AsyncTask;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.io.IOException;

public class Verificator extends AsyncTask<String, Void, Boolean> {

	String user;
	String password;
	String domain;
	OnVerificatorListener listener;

	public Verificator(OnVerificatorListener listener) {
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(String... params) {
		user = params[0];
		password = params[1];
		domain = params[2];
		XMPPTCPConnection conn = new XMPPTCPConnection(MainInterface.getConfig(domain, 5222));
		try {
			conn.connect();
			conn.login(user, password, "TESTING");
			conn.disconnect();
			return true;
		} catch (XMPPException e) {
			MainInterface.log(e.getMessage());
			return false;
		} catch (SmackException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		listener.onResult(success, user, password, domain);
	}

	public interface OnVerificatorListener {
		public void onResult(boolean success, String user, String password, String domain);
	}
}