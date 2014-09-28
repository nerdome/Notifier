package de.adornis.Notifier;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class UpdatePreference extends Preference {

	private Context context;

	public UpdatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		((LinearLayout) view).setOrientation(LinearLayout.VERTICAL);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
				((ViewGroup) view).addView(progress, 3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1F));
				(new AutoUpdater()).update(progress);

				view.findViewById(android.R.id.title).setEnabled(false);
				view.findViewById(android.R.id.summary).setEnabled(false);
				view.setOnClickListener(null);
			}
		});
	}
}
