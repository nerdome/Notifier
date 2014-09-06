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
			// TODO obviously this gets called before anything else can save the context. Weird
			Preferences.setContext(c);
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
			if (getItem(position).isOnline()) {
				convertView.setBackgroundColor(Color.rgb(100, 180, 100));
			} else {
				convertView.setBackgroundColor(Color.rgb(180, 100, 100));
			}
		} catch(NullPointerException e) {
			// online isn't defined yet, probably not on the roster
		}

		return convertView;
	}
}
