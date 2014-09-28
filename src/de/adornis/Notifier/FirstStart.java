package de.adornis.Notifier;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FirstStart extends Activity {

	private Button verify;

	private CredentialsFragment credentials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		credentials = (CredentialsFragment) getFragmentManager().findFragmentById(R.id.credentials);
		credentials.setOnDoneListener(new CredentialsFragment.OnDoneListener() {
			@Override
			public void onDone(String user, String password, String domain) {
				shoot(user, password, domain);
			}
		});
		verify = (Button) findViewById(R.id.verify);

		credentials.setStatusText("");
		verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shoot(credentials.getUserInput(), credentials.getPasswordInput(), credentials.getDomainInput());
			}
		});
	}

	private void shoot(String user, String password, String domain) {
		credentials.setStatusText("");

		credentials.setUserInput(credentials.getUserInput().toLowerCase());
		credentials.setProgress(true);

		if (domain.contains(".")) {
			Verificator ver = new Verificator(new Verificator.OnVerificatorListener() {
				@Override
				public void onResult(boolean success, String user, String password, String domain) {
					if (success) {
						try {
							Preferences.setAppUser(new ApplicationUser(user, password, domain));
						} catch (InvalidJIDException e) {
							MainInterface.log("Invalid JID, shouldn't happen though because they have been verified");
						}
						Preferences.close();
						Notifier.getContext().startActivity(new Intent(Notifier.getContext(), MainInterface.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						finish();
					} else {
						Preferences.setAppUser(null);
						credentials.setStatusText("Not successful! Either your credentials are wrong or the connection timed out!");
						credentials.setProgress(false);
					}
				}
			});
			ver.execute(user, password, domain);
		} else {
			credentials.setStatusText("You did not enter a proper domain");
			credentials.setProgress(false);
		}
	}
}
