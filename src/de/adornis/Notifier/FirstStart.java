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

public class FirstStart extends Activity {

	private EditText userEditText;
	private EditText domainEditText;
	private EditText passwordEditText;
	private Button verify;
	private ProgressBar progressBar;
	private TextView statusTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MainInterface.log("asdf");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		userEditText = (EditText) findViewById(R.id.username);
		domainEditText = (EditText) findViewById(R.id.domain);
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
				String domain = String.valueOf(domainEditText.getText());
				String password = String.valueOf(passwordEditText.getText());

				if(user.contains("@") && user.contains(".")) {
					Verificator ver = new Verificator();
					ver.execute(user, password, domain);
				} else {
					statusTextView.setText("You did not enter a proper JID");
					progressBar.setVisibility(View.INVISIBLE);
				}
			}
		});
	}


	public class Verificator extends AsyncTask<String, Void, Boolean> {

		String user;
		String password;
		String domain;

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
			if(success) {
				try {
					Preferences.setAppUser(new ApplicationUser(user, password, domain));
				} catch (InvalidJIDException e) {
					MainInterface.log("Invalid JID, shouldn't happen though because they have been verified");
				}
				Preferences.close();
				startActivity(new Intent(FirstStart.this, MainInterface.class));
				finish();
			} else {
				Preferences.setAppUser(null);
				statusTextView.setText("Please use your JID (user@domain), the login process was not successful");
				progressBar.setVisibility(View.INVISIBLE);
			}
		}
	}
}
