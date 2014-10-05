package de.adornis.Notifier.preferences;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.adornis.Notifier.MainInterface;

import java.util.List;

public class AppChooserPreference extends DialogPreference {

	public AppChooserPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AppChooserPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setPositiveButtonText("");
		setNegativeButtonText("");
	}

	@Override
	protected View onCreateDialogView() {
		final PackageManager pm = getContext().getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		ListView lv = new ListView(getContext());
		lv.setAdapter(new AppChooserListAdapter(packages, pm));

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				persistString(((ApplicationInfo) lv.getAdapter().getItem(position)).packageName);
				getDialog().cancel();
			}
		});

		return lv;
	}

	class AppChooserListAdapter extends BaseAdapter {

		private List<ApplicationInfo> apps;
		private PackageManager pm;

		public AppChooserListAdapter(List<ApplicationInfo> apps, PackageManager pm) {
			this.apps = apps;
			this.pm = pm;
		}

		@Override
		public int getCount() {
			return apps.size();
		}

		@Override
		public Object getItem(int position) {
			return apps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return apps.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ApplicationInfo current = apps.get(position);

			LinearLayout layout = new LinearLayout(getContext());
			layout.setOrientation(LinearLayout.HORIZONTAL);

			ImageView icon = new ImageView(getContext());
			try {
				BitmapDrawable d = (BitmapDrawable) current.loadIcon(pm);
				Bitmap b = d.getBitmap();
				b = Bitmap.createScaledBitmap(b, 36, 36, true);
				icon.setImageBitmap(b);
				icon.setPadding(22, 8, 8, 8);
			} catch (Resources.NotFoundException e) {
				MainInterface.log(current.name + "'s logo couldn't be found");
			}
			layout.addView(icon, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

			LinearLayout textLayout = new LinearLayout(getContext());
			textLayout.setOrientation(LinearLayout.VERTICAL);
			textLayout.setGravity(Gravity.CENTER_VERTICAL);

			TextView label = new TextView(getContext(), null, android.R.attr.textAppearanceLarge);
			label.setText(current.loadLabel(pm));
			textLayout.addView(label, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

			TextView packageName = new TextView(getContext(), null, android.R.attr.textAppearanceMedium);
			packageName.setText(current.packageName);
			packageName.setTextColor(Color.LTGRAY);
			textLayout.addView(packageName, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

			layout.addView(textLayout, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);

			return layout;
		}
	}
}