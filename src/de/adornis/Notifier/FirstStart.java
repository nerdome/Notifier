package de.adornis.Notifier;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FirstStart extends Activity {

	private Button verify;

	private CredentialsFragment credentials;

	private Verificator ver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_start);

		credentials = (CredentialsFragment) getFragmentManager().findFragmentById(R.id.credentials);
		credentials.setOnDoneListener(new CredentialsFragment.OnDoneListener() {
			@Override
			public void onDone(String user, String password, String domain) {
				shoot();
			}
		});
		verify = (Button) findViewById(R.id.verify);

		credentials.setStatusText("");
		verify.setOnClickListener(new OnStartVerificationListener());
	}

	private void shoot() {
		String user = credentials.getUserInput();
		String password = credentials.getPasswordInput();
		String domain = credentials.getDomainInput();

		credentials.setStatusText("");

		credentials.setUserInput(credentials.getUserInput().toLowerCase());

		if (domain.contains(".")) {
			ver = new Verificator(new Verificator.OnVerificatorListener() {
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
					verify.setOnClickListener(new OnStartVerificationListener());
				}
			});
			verify.setOnClickListener(new OnCancelVerificationListener());
			ver.execute(user, password, domain);
		} else {
			credentials.setStatusText("You did not enter a proper domain");
			credentials.setProgress(false);
		}
	}

	private class OnStartVerificationListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			verify.setText("Abort!");
			credentials.setProgress(true);
			MainInterface.log("setting to abort");
			shoot();
		}
	}

	private class OnCancelVerificationListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (ver != null) {
				ver.cancel(true);
				if (ver.isCancelled()) {
					verify.setText("Let's go!");
					verify.setOnClickListener(new OnStartVerificationListener());
				} else {
					credentials.setStatusText("Aborting the verification failed!");
				}
				credentials.setProgress(false);
			}
		}
	}
}
