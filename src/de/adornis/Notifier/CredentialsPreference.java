package de.adornis.Notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

public class CredentialsPreference extends Preference {

	private ViewGroup thisView;

	private SharedPreferences sp;
	private ViewGroup detailView;
	private Button save;
	private EditText user;
	private EditText password;
	private EditText domain;
	private CheckBox visiblePassword;
	private TextView status;
	private ProgressBar progress;

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
		save.setVisibility(View.GONE);
		((ViewGroup) superView).addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0));

		return superView;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		this.thisView = (ViewGroup) view;

		status = (TextView) thisView.findViewById(R.id.statusText);
		progress = (ProgressBar) thisView.findViewById(R.id.progressBar);

		user = (EditText) detailView.findViewById(R.id.username);
		password = (EditText) detailView.findViewById(R.id.password);
		domain = (EditText) detailView.findViewById(R.id.domain);
		visiblePassword = (CheckBox) detailView.findViewById(R.id.visiblePassword);

		user.setText(sp.getString("user", ""));
		password.setText(sp.getString("password", ""));
		domain.setText(sp.getString("domain", ""));

		thisView.getChildAt(0).setOnClickListener(new EditOnClickListener());
		thisView.getChildAt(1).setOnClickListener(new EditOnClickListener());

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

		status.setVisibility(View.GONE);
		progress.setVisibility(View.GONE);

		user.requestFocus();
	}

	private void persist() {
		String userValue = user.getText().toString();
		String passwordValue = password.getText().toString();
		String domainValue = domain.getText().toString();

		progress.setVisibility(View.VISIBLE);

		Verificator v = new Verificator(new Verificator.OnVerificatorListener() {
			@Override
			public void onResult(boolean success, String user, String password, String domain) {
				if (success) {
					try {
						Preferences.setAppUser(new ApplicationUser(user, password, domain));
					} catch (InvalidJIDException e) {
						MainInterface.log("Setting app user in CredentialsPreference didn't work which should never happen because the credentials have been verified");
					}

					sp.edit().putString("user", userValue).putString("password", passwordValue).putString("domain", domainValue).apply();

					detailView.setVisibility(View.GONE);
					save.setVisibility(View.GONE);
					progress.setVisibility(View.GONE);
					status.setVisibility(View.GONE);
					thisView.getChildAt(0).setVisibility(View.VISIBLE);
					thisView.getChildAt(1).setVisibility(View.VISIBLE);
					thisView.getChildAt(0).setOnClickListener(new EditOnClickListener());
					thisView.getChildAt(1).setOnClickListener(new EditOnClickListener());
				} else {
					progress.setVisibility(View.GONE);
					status.setText("Wrong credentials, please try again!");
					status.setVisibility(View.VISIBLE);
				}
			}
		});
		v.execute(userValue, passwordValue, domainValue);
	}

	private class EditOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			detailView.setVisibility(detailView.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
			save.setVisibility(save.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
			thisView.getChildAt(0).setVisibility(View.GONE);
			thisView.getChildAt(1).setVisibility(View.GONE);
			thisView.getChildAt(0).setOnClickListener(null);
			thisView.getChildAt(1).setOnClickListener(null);
		}
	}
}