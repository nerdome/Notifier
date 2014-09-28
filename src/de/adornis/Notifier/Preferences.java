package de.adornis.Notifier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Preferences extends Activity {

	public static final int ALPHABETICALLY = 0;
	public static final int ONLINE_STATUS = 1;

	private static Context c = Notifier.getContext();

	private static SharedPreferences prefs;

	private static ApplicationUser appUser;
	private static ArrayList<TargetUser> users = new ArrayList<>();
	private static Set<String> ignoredRosterUsers;
	private static File usersFile;
	private static int listenerRunning = Listener.DISCONNECTED;

	private static boolean initialized = false;

	public Preferences() throws UserNotFoundException {
		if (!initialized) {
			try {
				initialize();
			} catch (UserNotFoundException e) {
				MainInterface.log("no app user yet, initiating firststart activity");
				Notifier.getContext().startActivity(new Intent(Notifier.getContext(), FirstStart.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				throw e;
			}
		}
	}

	// must be called before making a pref object
	public static void initialize() throws UserNotFoundException {
		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		usersFile = new File(c.getFilesDir(), "targetUsers");
		listenerRunning = prefs.getInt("listener_running", Listener.DISCONNECTED);
		ignoredRosterUsers = prefs.getStringSet("ignored_roster_users", new HashSet<String>());

		try {
			appUser = new ApplicationUser(prefs.getString("user", ""), prefs.getString("password", ""), prefs.getString("domain", ""));
		} catch (InvalidJIDException e) {
			throw new UserNotFoundException("application user");
		}

		try {
			if (!usersFile.createNewFile()) {
				try {
					ObjectInputStream ois = new ObjectInputStream(c.openFileInput("targetUsers"));
					Object in = ois.readObject();
					users = (ArrayList<TargetUser>) in;
					ois.close();
					for (TargetUser user : users) {
						user.updatePresence();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		initialized = true;
	}

	public static void close() {
		if (appUser != null) {
			prefs.edit().putString("user", appUser.getUsername()).putString("password", appUser.getPassword()).putString("domain", appUser.getDomain()).putInt("listener_running", Listener.DISCONNECTED).putStringSet("ignored_roster_users", ignoredRosterUsers).commit();
		}

		try {
			ObjectOutputStream oos = new ObjectOutputStream(c.openFileOutput("targetUsers", MODE_PRIVATE));
			oos.writeObject(users);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean sortUsers(int mode) {
		int i;
		int j;
		TargetUser newExtrema;
		int newExtremaNumber = -1;
		boolean reverse = !compare(users.get(0), users.get(users.size() - 1), mode, false);
		for (i = 0; i < users.size() - 1; i++) {
			newExtrema = null;
			newExtremaNumber = -1;
			for (j = i + 1; j < users.size(); j++) {
				if (compare(users.get(i), users.get(j), mode, reverse)) {
					newExtrema = users.get(j);
					newExtremaNumber = j;
				}
			}
			if (newExtremaNumber != -1) {
				users.set(newExtremaNumber, users.get(i));
				users.set(i, newExtrema);
			}
		}
		c.sendBroadcast(new Intent(Notifier.USER_EVENT));
		return reverse;
	}

	private static boolean compare(TargetUser a, TargetUser b, int mode, boolean reverse) {
		switch (mode) {
			case ALPHABETICALLY:
				Collator c = Collator.getInstance();
				c.setStrength(Collator.PRIMARY);
				return reverse ? (c.compare(a.getJID(), b.getJID()) > 0) : (c.compare(a.getJID(), b.getJID()) < 0);
			case ONLINE_STATUS:
				return reverse ? (a.getOnlineStatus() > b.getOnlineStatus()) : (a.getOnlineStatus() < b.getOnlineStatus());
			default:
				MainInterface.log("this shouldn't have happened while sorting");
				return false;
		}
	}

	public ApplicationUser getAppUser() {
		return appUser;
	}

	public static void setAppUser(ApplicationUser usr) {
		appUser = usr;
		c.sendBroadcast(new Intent(Notifier.CREDENTIALS));
	}

	public TargetUser findUser(String JID) {
		try {
			return users.get(getUserId(JID));
		} catch (UserNotFoundException e) {
			return null;
		}
	}

	public ArrayList<TargetUser> getUsers() {
		return users;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		close();
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			initialize();
		} catch (UserNotFoundException e) {
			MainInterface.log("no app user yet, initiating firststart activity");
			Notifier.getContext().startActivity(new Intent(Notifier.getContext(), FirstStart.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			finish();
		}
		c.sendBroadcast(new Intent(Notifier.CREDENTIALS));
	}

	public void addUser(String user) throws InvalidJIDException {
		if (user.contains("@") && user.contains(".")) {
			addUser(user, user.substring(0, user.indexOf("@")));
		} else {
			throw new InvalidJIDException(user);
		}
	}

	public void addUser(String user, String nick) throws InvalidJIDException {
		if (findUser(user) == null) {
			users.add(new TargetUser(user, nick));
			c.sendBroadcast(new Intent(Notifier.USER_EVENT));
		}
		unignoreRosterUser(user);
	}

	public void delUser(String JID) throws UserNotFoundException {
		users.remove(getUserId(JID));
	}

	public TargetUser getUser(String JID) throws UserNotFoundException {
		return users.get(getUserId(JID));
	}

	public int getUserId(String JID) throws UserNotFoundException {
		for (User current : users) {
			if (current.getJID().equals(JID)) {
				return users.indexOf(current);
			}
		}
		throw new UserNotFoundException(JID);
	}

	public boolean isRosterUserIgnored(String JID) {
		return ignoredRosterUsers.contains(JID) || findUser(JID) != null;
	}

	public void ignoreRosterUser(String JID) {
		ignoredRosterUsers.add(JID);
	}

	public void unignoreRosterUser(String JID) {
		ignoredRosterUsers.remove(JID);
	}

	public boolean isAutoStart() {
		return prefs.getBoolean("start_after_boot", false);
	}

	public String getAppAfterNotified() {
		return prefs.getString("activity_after_wake", "");
	}

	public void setConnected(int running) {
		listenerRunning = running;
		c.sendBroadcast(new Intent(Notifier.SERVICE));
	}

	public int isConnected() {
		return listenerRunning;
	}

	public void reset() {
		prefs.edit().clear().commit();
		appUser = null;
		users = new ArrayList<>();
		if (usersFile.delete()) {
			MainInterface.log("file has been deleted successfully!");
		} else {
			MainInterface.log("file has NOT been deleted successfully!");
		}
		c.sendBroadcast(new Intent(Notifier.USER_EVENT));

		Intent i = new Intent(c, FirstStart.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(i);

		finish();
	}
}
