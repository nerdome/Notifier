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
		verify = (Button) findViewById(R.id.verify);

		credentials.setStatusText("");
		verify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				credentials.setUserInput(credentials.getUserInput().toLowerCase());
				credentials.setProgress(true);

				if (credentials.getDomainInput().contains(".")) {
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
								credentials.setStatusText("Please use your JID (user@domain), the login process was not successful");
								credentials.setProgress(false);
							}
						}
					});
					ver.execute(credentials.getUserInput(), credentials.getPasswordInput(), credentials.getDomainInput());
				} else {
					credentials.setStatusText("You did not enter a proper domain");
					credentials.setProgress(false);
				}
			}
		});
	}
}
