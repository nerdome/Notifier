package de.adornis.Notifier.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import de.adornis.Notifier.Preferences;

public class ResetPreference extends DialogPreference {

	public ResetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogMessage("Do you REALLY want to reset all your data?");
		setPositiveButtonText("Yes");
		setNegativeButtonText("No");
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			Preferences.reset();
		}
	}
}