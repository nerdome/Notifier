package de.adornis.Notifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.*;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class Preferences extends Activity {

	private static Context context;

	private static SharedPreferences prefs;

	private static ApplicationUser appUser;
	private static ArrayList<TargetUser> users = new ArrayList<>();

	// must be called before making a pref object
	public static void initialize(Context c) {
		Preferences.context = c.getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		File usersFile = new File(context.getFilesDir(), "targetUsers");

		try {
			appUser = new ApplicationUser(prefs.getString("user", ""), prefs.getString("password", ""));
		} catch (Exception e) {
			// application user has not been set up yet
		}

		try {
			if(!usersFile.createNewFile()) {
				try {
					ObjectInputStream ois = new ObjectInputStream(context.openFileInput("targetUsers"));
					Object in = ois.readObject();
					users = (ArrayList<TargetUser>) in;
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Preferences() throws Exception {
		if(context == null) {
			throw new Exception();
			// not initialized
		}
	}

	public ApplicationUser getAppUser() {
		return appUser;
	}

	public void setAppUser(ApplicationUser usr) {
		appUser = usr;
	}

	public void close() {
		MainInterface.log("saving...");

		prefs.edit().putBoolean("receiver_online", Listener.isRunning()).commit();

		if(appUser != null) {
			prefs.edit().putString("user", appUser.getJID()).commit();
			prefs.edit().putString("password", appUser.getPassword()).commit();
		}

		try {
			ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput("targetUsers", MODE_PRIVATE));
			oos.writeObject(users);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TargetUser findUser(String JID) throws Exception {
		return users.get(getUserId(JID));
	}

	public ArrayList<TargetUser> getUsers() {
		return users;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		initialize(context);
		stopService(new Intent(this, Listener.class));
		startService(new Intent(this, Listener.class));
	}

	public void addUser(String user) throws Exception {
		addUser(user, user);
	}

	public void addUser(String user, String nick) throws Exception {
		users.add(new TargetUser(user, nick));
	}

	public void delUser(String JID) throws Exception {
		users.remove(getUserId(JID));
	}

	public int getUserId(String JID) throws Exception {

		for(User current : users) {
			if(current.getJID().equals(JID)) {
				return users.indexOf(current);
			}
		}
		throw new Exception();
	}
}
