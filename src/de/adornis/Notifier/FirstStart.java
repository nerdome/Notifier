package de.adornis.Notifier;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.dns.HostAddress;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class FirstStart extends Activity {

	private EditText userEditText;
	private EditText passwordEditText;
	private Button verify;
	private ProgressBar progressBar;
	private TextView statusTextView;

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

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
				if(!String.valueOf(userEditText.getText()).contains("@")) {
					statusTextView.setText("Please use your JID (user@domain)");
					progressBar.setVisibility(View.INVISIBLE);
				} else {
					if (!verify(String.valueOf(userEditText.getText()), String.valueOf(passwordEditText.getText()))) {
						statusTextView.setText("Sorry, please try again :(");
						progressBar.setVisibility(View.INVISIBLE);
					} else {
						statusTextView.setText("You're in! :)");
						prefs.edit().putString("user", String.valueOf(userEditText.getText())).commit();
						prefs.edit().putString("password", String.valueOf(passwordEditText.getText())).commit();
						startActivity(new Intent(FirstStart.this, MainInterface.class));
						finish();
					}
				}
			}
		});
	}

	private boolean verify(String user, String password) {

		AsyncTask<String, Void, Boolean> verification = new AsyncTask<String, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(String... params) {
				String user = params[0];
				String password = params[1];
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
		};

		verification.execute(user, password);
		try {
			return verification.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		MainInterface.log("i shouldn't be here!");

		return false;
	}
}
