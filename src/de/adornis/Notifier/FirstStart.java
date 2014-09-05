package de.adornis.Notifier;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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

		verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				progressBar.setVisibility(View.VISIBLE);
				if(Listener.verify(String.valueOf(userEditText.getText()), String.valueOf(passwordEditText.getText()))) {
					statusTextView.setText("Sorry, please try again :(");
				} else {
					statusTextView.setText("You're in! :)");
					prefs.edit().putString("user", String.valueOf(userEditText.getText())).commit();
					prefs.edit().putString("password", String.valueOf(passwordEditText.getText())).commit();
					startActivity(new Intent(FirstStart.this, MainInterface.class));
				}
			}
		});
	}
}
