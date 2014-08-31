package de.adornis.Notifier;

import android.app.Activity;
import android.os.Bundle;

public class PreferenceEditor extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
}
