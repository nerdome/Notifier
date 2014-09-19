package de.adornis.Notifier;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ResetPreference extends DialogPreference {

	public ResetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogMessage("Do you REALLY want to reset all your data?");
		setPositiveButtonText("Yes");
		setNegativeButtonText("No");
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if(positiveResult) {
			try {
				(new Preferences()).reset();
			} catch (UserNotFoundException e) {
				MainInterface.log("FATAL - exception in ResetPreference in onDialogClosed()");
				e.printStackTrace();
			}
		}
	}
}