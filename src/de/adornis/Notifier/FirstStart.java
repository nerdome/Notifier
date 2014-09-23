package de.adornis.Notifier;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FirstStart extends Activity {

	private Button verify;
	private ProgressBar progressBar;
	private TextView statusTextView;

	private CredentialsFragment credentials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		credentials = (CredentialsFragment) getFragmentManager().findFragmentById(R.id.credentials);
		verify = (Button) findViewById(R.id.verify);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		statusTextView = (TextView) findViewById(R.id.statusText);

		statusTextView.setText("");
		statusTextView.setVisibility(View.VISIBLE);
		verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				credentials.setUserInput(credentials.getUserInput().toLowerCase());
				progressBar.setVisibility(View.VISIBLE);

				if (credentials.getDomainInput().contains(".")) {
					Verificator ver = new Verificator(new Verificator.OnVerificatorListener() {
						@Override
						public void onResult(boolean success) {
							if (success) {
								try {
									Preferences.setAppUser(new ApplicationUser(credentials.getUserInput(), credentials.getPasswordInput(), credentials.getDomainInput()));
								} catch (InvalidJIDException e) {
									MainInterface.log("Invalid JID, shouldn't happen though because they have been verified");
								}
								Preferences.close();
								Notifier.getContext().startActivity(new Intent(Notifier.getContext(), MainInterface.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
								finish();
							} else {
								Preferences.setAppUser(null);
								statusTextView.setText("Please use your JID (user@domain), the login process was not successful");
								progressBar.setVisibility(View.INVISIBLE);
							}
						}
					});
					ver.execute(credentials.getUserInput(), credentials.getPasswordInput(), credentials.getDomainInput());
				} else {
					statusTextView.setText("You did not enter a proper domain");
					progressBar.setVisibility(View.INVISIBLE);
				}
			}
		});
	}
}
