package de.adornis.Notifier;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CredentialsFragment extends Fragment {

	private CheckBox showPasswordCheckbox;
	private EditText userEditText;
	private EditText passwordEditText;
	private EditText domainEditText;

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

		showPasswordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked) {
					passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				} else {
					passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
}
