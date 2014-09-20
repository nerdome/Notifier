package de.adornis.Notifier;

import android.content.Context;
import android.preference.Preference;

public class UpdatePreference extends Preference {
	public UpdatePreference(Context context) {
		super(context);

		this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				(new AutoUpdater()).update();
				return true;
			}
		});
	}
}