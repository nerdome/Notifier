package de.adornis.Notifier;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TargetListAdapter extends BaseAdapter {

	private Context c;
	private Preferences prefs;

	public TargetListAdapter(Context c) {
		this.c = c;
		try {
			prefs = new Preferences();
		} catch (Preferences.NotInitializedException e) {
			MainInterface.log("FATAL");
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

		switch(getItem(position).getOnlineStatus()) {
			case TargetUser.NOT_CHECKED:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 80, 80));
				convertView.setBackgroundColor(Color.rgb(70, 70, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : no information yet");
				break;
			case TargetUser.ONLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 130, 80));
				convertView.setBackgroundColor(Color.rgb(70, 90, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : online");
				break;
			case TargetUser.HALF_ONLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(30, 70, 30));
				convertView.setBackgroundColor(Color.rgb(20, 50, 20));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : only online on a different resource");
				break;
			case TargetUser.OFFLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(130, 80, 80));
				convertView.setBackgroundColor(Color.rgb(90, 70, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : offline");
				break;
			case TargetUser.NOT_IN_ROSTER:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 80, 130));
				convertView.setBackgroundColor(Color.rgb(70, 70, 90));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : not in roster");
				break;
		}

		return convertView;
	}
}
