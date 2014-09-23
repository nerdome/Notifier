package de.adornis.Notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CredentialsPreference extends Preference {

	SharedPreferences sp;
	EditText userEditText;
	EditText passwordEditText;
	EditText domainEditText;
	CheckBox visiblePasswordCheckBox;
	private Context c;

	public CredentialsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		Log.e("contructor CredentialsPreferences", "beginning after super(context,attrs);");

		c = context;
		sp = PreferenceManager.getDefaultSharedPreferences(c);

		setFragment(CredentialsFragment.class.getName());
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		MainInterface.log("on create view");
		View v = LayoutInflater.from(Notifier.getContext()).inflate(R.layout.credentials_dialog, null);

		userEditText = (EditText) v.findViewById(R.id.username);
		passwordEditText = (EditText) v.findViewById(R.id.password);
		domainEditText = (EditText) v.findViewById(R.id.domain);
		visiblePasswordCheckBox = (CheckBox) v.findViewById(R.id.visiblePassword);

		//own code

		userEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Log.e("onFocusChange", "bluub");
				sp.edit().putString("user", userEditText.getText().toString()).apply();
			}
		});

//		v.setId(android.R.id.widget_frame);

		String tmpUserEditText = userEditText.getText() == null ? userEditText.getText().toString() : "defaultValueUserFromField";
		Log.e("userEditText from Field beginning of onCreateView after Inflator", tmpUserEditText);
		Log.e("userEditText from SP beginning of onCreateView after Inflator", sp.getString("user", "defaultValueUserFromSP"));
		userEditText.setText(userEditText.getText());

		String user;
		String password;
		String domain;


		user = sp.getString("user", "");
		password = sp.getString("password", "");
		domain = sp.getString("domain", "");


		userEditText.setText(sp.getString("user", ""));
		passwordEditText.setText(sp.getString("password", ""));
		domainEditText.setText(sp.getString("domain", ""));
		//end own code

		super.onCreateView(parent);

		return v;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		MainInterface.log("onGetDefaultValue");
		return new String[]{"", "", ""};
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		MainInterface.log("onSetInitialValue");
		String user;
		String password;
		String domain;

		if (restorePersistedValue) {
			user = sp.getString("user", "");
			password = sp.getString("password", "");
			domain = sp.getString("domain", "");
		} else {
			user = ((String[]) defaultValue)[0];
			password = ((String[]) defaultValue)[1];
			domain = ((String[]) defaultValue)[2];
		}

		userEditText.setText(sp.getString("user", ""));
		passwordEditText.setText(sp.getString("password", ""));
		domainEditText.setText(sp.getString("domain", ""));
		visiblePasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked) {
					passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				} else {
					passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				}
			}
		});

		sp.edit().putString("user", user).putString("password", password).putString("domain", domain).commit();
	}

	//	public void verify() {
	//
	//		Verificator v = new Verificator(new Verificator.OnVerificatorListener() {
	//			@Override
	//			public void onResult(boolean success) {
	//				try {
	//					Preferences.setAppUser(new ApplicationUser(user, password, domain));
	//				} catch (InvalidJIDException e) {
	//					MainInterface.log("Setting app user in CredentialsPreference didn't work which should never happen because the credentials have been verified");
	//				}
	//			}
	//		});
	//		v.execute(user, password, domain);
	//	}
}