package de.adornis.Notifier.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import de.adornis.Notifier.*;

public class CredentialsPreference extends Preference {

	private SharedPreferences sp;

	private ViewGroup detailView;
	private boolean detail;
	private Button save;

	private EditText user;
	private String userText;
	private EditText password;
	private String passwordText;
	private EditText domain;
	private String domainText;

	private CheckBox visiblePassword;
	private TextView status;
	private ProgressBar progress;

	private View title;
	private View summary;

	public CredentialsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		sp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View superView = super.onCreateView(parent);

		detailView = (ViewGroup) LayoutInflater.from(Notifier.getContext()).inflate(R.layout.credentials_dialog, null);
		((ViewGroup) superView).addView(detailView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

		save = new Button(getContext());
		save.setText("Save");
		((ViewGroup) superView).addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0));

		status = (TextView) superView.findViewById(R.id.statusText);
		progress = (ProgressBar) superView.findViewById(R.id.progressBar);
		user = (EditText) detailView.findViewById(R.id.username);
		password = (EditText) detailView.findViewById(R.id.password);
		domain = (EditText) detailView.findViewById(R.id.domain);
		visiblePassword = (CheckBox) detailView.findViewById(R.id.visiblePassword);

		setUserText(sp.getString("user", ""));
		setPasswordText(sp.getString("password", ""));
		setDomainText(sp.getString("domain", ""));

		title = ((ViewGroup) superView).getChildAt(0);
		summary = ((ViewGroup) superView).getChildAt(1);

		setDetail(false);

		return superView;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		setDetail(false);

		detailView.setVisibility(View.GONE);

		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				persist();
			}
		});

		visiblePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				password.setInputType(InputType.TYPE_CLASS_TEXT | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
			}
		});

		password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					persist();
					handled = true;
				}
				return handled;
			}
		});

		status.requestFocus();
	}

	private void persist() {

		progress.setVisibility(View.VISIBLE);

		Verificator v = new Verificator(new Verificator.OnVerificatorListener() {
			@Override
			public void onResult(boolean success, String user, String password, String domain) {
				if (success) {
					try {
						Preferences.setAppUser(new ApplicationUser(user, password, domain));
					} catch (User.InvalidJIDException e) {
						MainInterface.log("Setting app user in CredentialsPreference didn't work which should never happen because the credentials have been verified");
					}

					updateValues();
					sp.edit().putString("user", userText).putString("password", passwordText).putString("domain", domainText).apply();
					setDetail(false);
				} else {
					progress.setVisibility(View.GONE);
					status.setText("Wrong credentials, please try again!");
					status.setVisibility(View.VISIBLE);
				}
			}
		});
		v.execute(userText, passwordText, domainText);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		MainInterface.log("onsaveinstancestate");
		Parcelable superState = super.onSaveInstanceState();

		SavedState thisState = new SavedState(superState);
		updateValues();
		thisState.values = new String[]{userText, passwordText, domainText, detailView.getVisibility() == View.GONE ? "nodetail" : "detail"};
		return thisState;
	}

	private void updateValues() {
		userText = user.getText().toString();
		passwordText = password.getText().toString();
		domainText = domain.getText().toString();
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		MainInterface.log("onrestoreinstancestate");
		if (state == null || !state.getClass().equals(SavedState.class)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState thisState = (SavedState) state;
		MainInterface.log(thisState.values[0]);
		super.onRestoreInstanceState(thisState.getSuperState());
		MainInterface.log(thisState.values[0]);

		setUserText(thisState.values[0]);
		setPasswordText(thisState.values[1]);
		setDomainText(thisState.values[2]);
		setDetail(thisState.values[3].equals("detail"));
	}

	public void setUserText(String userText) {
		this.userText = userText;
		if (user != null) user.setText(userText);
	}

	public void setPasswordText(String passwordText) {
		this.passwordText = passwordText;
		if (password != null) password.setText(passwordText);
	}

	public void setDomainText(String domainText) {
		this.domainText = domainText;
		if (domain != null) domain.setText(domainText);
	}

	public void setDetail(boolean detail) {
		this.detail = detail;

		if (detailView != null) detailView.setVisibility(detail ? View.VISIBLE : View.GONE);
		if (save != null) save.setVisibility(detail ? View.VISIBLE : View.GONE);
		if (progress != null) progress.setVisibility(detail ? View.GONE : View.VISIBLE);
		if (status != null) status.setVisibility(detail ? View.GONE : View.VISIBLE);
		if (title != null) title.setVisibility(detail ? View.GONE : View.VISIBLE);
		if (summary != null) summary.setVisibility(detail ? View.GONE : View.VISIBLE);
		if (title != null) title.setOnClickListener(detail ? null : new EditOnClickListener());
		if (summary != null) summary.setOnClickListener(detail ? null : new EditOnClickListener());
	}

	private static class SavedState extends BaseSavedState {

		public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel source) {
				MainInterface.log("createFromParcel");
				return new SavedState(source);
			}

			{
				MainInterface.log("new CREATOR");
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}


		};
		String[] values = new String[4];

		public SavedState(Parcelable superState) {
			super(superState);
			MainInterface.log("SavedState parcelable");
		}

		public SavedState(Parcel source) {
			super(source);
			MainInterface.log("SavedState parcel");
			source.readStringArray(values);
			MainInterface.log(values[0]);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			MainInterface.log("write to parcel" + values[0]);
			dest.writeStringArray(values);
		}
	}

	private class EditOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			((InputMethodManager) Notifier.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(user, InputMethodManager.SHOW_FORCED);
			user.requestFocus();

			setDetail(true);
		}
	}
}