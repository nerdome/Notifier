package de.adornis.Notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class StartupReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("receiver_online", false)) {
			context.startService(new Intent(context, Listener.class));
		}
	}
}
