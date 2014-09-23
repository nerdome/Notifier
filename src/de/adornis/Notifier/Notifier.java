package de.adornis.Notifier;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

public class Notifier extends Application {

	public final static String CREDENTIALS = "de.adornis.Notifier.CREDENTIALS";
	public final static String USER_CHANGE = "de.adornis.Notifier.USER_CHANGE";
	public final static String USER_EVENT = "de.adornis.Notifier.USER_EVENT";
	public final static String STOP = "de.adornis.Notifier.STOP";
	public final static String SERVICE = "de.adornis.Notifier.SERVICE";
	public final static String USER_PROPOSE_LIST = "de.adornis.Notifier.USER_PROPOSE_LIST";
	public final static String USER_PROPOSE_ROSTER = "de.adornis.Notifier.USER_PROPOSE_ROSTER";
	public final static String UPDATE_AVAILABLE = "de.adornis.Notifier.UPDATE_AVAILABLE";
	public final static String INVITATION = "de.adornis.Notifier.INVITATION";

	private static Notifier self;
	private static int version;

	public static int getVersion() {
		return version;
	}

	public static Context getContext() {
		return self.getApplicationContext();
	}

	@Override
	public void onCreate() {
		self = this;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			// lawl this doesn't exist... yes it does -.-
		}
	}
}
