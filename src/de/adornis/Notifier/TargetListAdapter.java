package de.adornis.Notifier;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class TargetListAdapter extends BaseAdapter {

	private Context c;
	private Preferences prefs;

	public TargetListAdapter(Context c) throws UserNotFoundException {
		this.c = c;
		prefs = new Preferences();
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
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO use convertView! and don't pass null...
		LayoutInflater inflater = LayoutInflater.from(c);

		convertView = inflater.inflate(R.layout.target_entry, null);

		((TextView) convertView.findViewById(R.id.nick)).setText(getItem(position).getNick());

		convertView.findViewById(R.id.invite).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Notifier.getContext().sendBroadcast(new Intent(Notifier.INVITATION).putExtra("JID", getItem(position).getJID()));
			}
		});

		TextView resourceView = (TextView) convertView.findViewById(R.id.resources);
		resourceView.setText("");
		ArrayList<String> resourceList = getItem(position).getResourceList();
		for (int i = 0; i < resourceList.size(); i++) {
			String current = resourceList.get(i);
			resourceView.setText(resourceView.getText() + current + (i != resourceList.size() - 1 ? " | " : ""));
		}

		switch (getItem(position).getOnlineStatus()) {
			case TargetUser.NOT_CHECKED:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 80, 80));
				convertView.setBackgroundColor(Color.rgb(70, 70, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : no information yet");
				convertView.findViewById(R.id.invite).setVisibility(View.VISIBLE);
				break;
			case TargetUser.ONLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 130, 80));
				convertView.setBackgroundColor(Color.rgb(70, 90, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : online");
				convertView.findViewById(R.id.invite).setVisibility(View.GONE);
				break;
			case TargetUser.HALF_ONLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(130, 130, 80));
				convertView.setBackgroundColor(Color.rgb(90, 90, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : only online on a different resource");
				convertView.findViewById(R.id.invite).setVisibility(View.VISIBLE);
				break;
			case TargetUser.OFFLINE:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(130, 80, 80));
				convertView.setBackgroundColor(Color.rgb(90, 70, 70));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : offline");
				convertView.findViewById(R.id.invite).setVisibility(View.VISIBLE);
				break;
			case TargetUser.NOT_IN_ROSTER:
				convertView.findViewById(R.id.colorCoding).setBackgroundColor(Color.rgb(80, 80, 130));
				convertView.setBackgroundColor(Color.rgb(70, 70, 90));
				((TextView) convertView.findViewById(R.id.JID)).setText(getItem(position).getJID() + " : not in roster");
				convertView.findViewById(R.id.invite).setVisibility(View.VISIBLE);
				break;
		}

		return convertView;
	}
}
