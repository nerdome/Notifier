package de.adornis.Notifier;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TargetListAdapter extends BaseAdapter {

	Context c;
	Preferences prefs;

	public TargetListAdapter(Context c) {
		this.c = c;
		try {
			prefs = new Preferences();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getCount() {
		return prefs.getUsers().size();
	}

	@Override
	public TargetUser getItem(int position) {
		return prefs.getUsers().get(position);
	}

	@Override
	public long getItemId(int position) {
		return prefs.getUsers().get(position).getJID().hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO use convertView! and don't pass null...
		LayoutInflater inflater = LayoutInflater.from(c);

		convertView = inflater.inflate(R.layout.target_entry, null);

		((TextView) convertView.findViewById(R.id.nick)).setText(getItem(position).getNick());
		((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID());

		try {
			if(getItem(position).isOnline()) {
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 130, 80));
				convertView.setBackgroundColor(Color.rgb(70, 90, 70));
			} else {
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(130, 80, 80));
				convertView.setBackgroundColor(Color.rgb(90, 70, 70));
			}
		} catch(NullPointerException e) {
			// online is not set, probably not in roster
			convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(0, 0, 0));
			convertView.setBackgroundColor(Color.rgb(0, 0, 0));
		}

		return convertView;
	}
}
