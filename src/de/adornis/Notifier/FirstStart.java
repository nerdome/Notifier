package de.adornis.Notifier;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class FirstStart extends Activity {

	Preferences prefs;

	private EditText userEditText;
	private EditText passwordEditText;
	private Button verify;
	private ProgressBar progressBar;
	private TextView statusTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			prefs = new Preferences();
		} catch (Preferences.NotInitializedException e) {
			MainInterface.log("FATAL");
			e.printStackTrace();
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		userEditText = (EditText) findViewById(R.id.username);
		passwordEditText = (EditText) findViewById(R.id.password);
		verify = (Button) findViewById(R.id.verify);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		statusTextView = (TextView) findViewById(R.id.statusText);

		statusTextView.setText("");
		statusTextView.setVisibility(View.VISIBLE);
		verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				userEditText.setText(String.valueOf(userEditText.getText()).toLowerCase());
				progressBar.setVisibility(View.VISIBLE);

				String user = String.valueOf(userEditText.getText());
				String password = String.valueOf(passwordEditText.getText());

				Verificator ver = new Verificator();
				ver.execute(user, password);
			}
		});
	}


	public class Verificator extends AsyncTask<String, Void, Boolean> {

		String user;
		String password;

		@Override
		protected Boolean doInBackground(String... params) {
			user = params[0];
			password = params[1];
			XMPPTCPConnection conn = new XMPPTCPConnection(MainInterface.connectionConfiguration);
			try {
				conn.connect();
				conn.login(user.substring(0, user.indexOf("@")), password, "TESTING");
				conn.disconnect();
			} catch (XMPPException e) {
				e.printStackTrace();
				return false;
			} catch (SmackException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			if(success) {
				try {
					prefs.setAppUser(new ApplicationUser(user, password));
				} catch (InvalidJIDException e) {
					MainInterface.log("Invalid JID, shouldn't happen though because they have been verified");
				}
				prefs.close();
				startActivity(new Intent(FirstStart.this, MainInterface.class));
				finish();
			} else {
				prefs.setAppUser(null);
				statusTextView.setText("Please use your JID (user@domain), the login process was not successful");
				progressBar.setVisibility(View.INVISIBLE);
			}
		}
	}
}
