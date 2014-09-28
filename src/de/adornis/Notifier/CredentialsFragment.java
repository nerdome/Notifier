package de.adornis.Notifier;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

public class CredentialsFragment extends Fragment {

	private OnDoneListener onDoneListener;

	private CheckBox showPasswordCheckbox;
	private EditText userEditText;
	private EditText passwordEditText;
	private EditText domainEditText;
	private TextView statusText;
	private ProgressBar progress;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.credentials_dialog, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		userEditText = (EditText) getView().findViewById(R.id.username);
		passwordEditText = (EditText) getView().findViewById(R.id.password);
		domainEditText = (EditText) getView().findViewById(R.id.domain);
		showPasswordCheckbox = (CheckBox) getView().findViewById(R.id.visiblePassword);

		statusText = (TextView) getView().findViewById(R.id.statusText);
		progress = (ProgressBar) getView().findViewById(R.id.progressBar);

		showPasswordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
			}
		});

		passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND && onDoneListener != null) {
					onDoneListener.onDone(userEditText.getText().toString(), passwordEditText.getText().toString(), domainEditText.getText().toString());
					handled = true;
				}
				return handled;
			}
		});
	}

	public String getUserInput() {
		return userEditText.getText().toString();
	}

	public void setUserInput(String input) {
		userEditText.setText(input);
	}

	public String getPasswordInput() {
		return passwordEditText.getText().toString();
	}

	public void setPasswordInput(String input) {
		passwordEditText.setText(input);
	}

	public String getDomainInput() {
		return domainEditText.getText().toString();
	}

	public void setDomainInput(String input) {
		domainEditText.setText(input);
	}

	public void setStatusText(String statusText) {
		if (statusText.equals("")) {
			this.statusText.setVisibility(View.GONE);
		} else {
			this.statusText.setVisibility(View.VISIBLE);
		}
		this.statusText.setText(statusText);
	}

	public void setProgress(boolean shown) {
		this.progress.setVisibility(shown ? View.VISIBLE : View.GONE);
	}

	public void setOnDoneListener(OnDoneListener listener) {
		onDoneListener = listener;
	}

	interface OnDoneListener {
		void onDone(String user, String password, String domain);
	}
}
