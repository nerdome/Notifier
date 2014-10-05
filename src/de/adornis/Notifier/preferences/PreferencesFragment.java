package de.adornis.Notifier.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import de.adornis.Notifier.R;

public class PreferencesFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
	}
}
