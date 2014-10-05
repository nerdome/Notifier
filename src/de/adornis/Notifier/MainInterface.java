package de.adornis.Notifier;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import de.adornis.Notifier.preferences.TargetUser;
import de.adornis.Notifier.preferences.User;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;

import java.io.IOException;

public class MainInterface extends Activity {

	private BroadcastReceiver serviceStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (Preferences.isConnected()) {
				case Listener.CONNECTED:
					setupSwitch(true, true);
					break;
				case Listener.CONNECTING:
					setupSwitch(true, false);
					break;
				case Listener.DISCONNECTED:
					setupSwitch(false, true);
					break;
				case Listener.DISCONNECTING:
					setupSwitch(false, false);
					break;
			}
		}
	};
	private AutoUpdater updater;
	private Listener listener = null;
	private ServiceConnection listenerConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			listener = ((Listener.ListenerBinder) service).getService();
			if (Preferences.isAutoStart() && Preferences.isConnected() == Listener.DISCONNECTED) {
				listener.attemptConnect();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			listener = null;
		}
	};

	private BroadcastReceiver userEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					targetListUpdated();
				}
			});
		}
	};
	private BroadcastReceiver listProposeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			if (!Preferences.isRosterUserIgnored(intent.getStringExtra("JID"))) {
				// ignore first, it will be unignored but this way, we prevent further dialogs from popping up
				Preferences.ignoreRosterUser(intent.getStringExtra("JID"));
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final String JID = intent.getStringExtra("JID");
						AlertDialog.Builder builder = new AlertDialog.Builder(MainInterface.this);
						builder.setTitle(JID);
						builder.setMessage("Do you want to add this user from your roster to your target list?");
						builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Preferences.ignoreRosterUser(JID);
							}
						});
						builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									Preferences.addUser(JID);
								} catch (User.InvalidJIDException e) {
									MainInterface.log("this should never happen, JIDs from the roster should not be wrong");
								}
							}
						});
						builder.create().show();
					}
				});
			}
		}
	};
	private BroadcastReceiver rosterProposeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final String JID = intent.getStringExtra("JID");
					AlertDialog.Builder builder = new AlertDialog.Builder(MainInterface.this);
					builder.setTitle(JID);
					builder.setMessage("Do you want to add this user to your roster?");
					builder.setNegativeButton("No", null);
					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							listener.addToRoster(JID);
						}
					});
					builder.create().show();
				}
			});
		}
	};
	private BroadcastReceiver userChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					targetListUpdated();
				}
			});
		}
	};
	private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MainInterface.log("got stop command");
			targetListUpdated();
			finish();
		}
	};
	private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updater.update(null);
		}
	};
	private BroadcastReceiver invitationReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				listener.invite(intent.getStringExtra("JID"));
			} catch (Preferences.UserNotFoundException e) {
				Notifier.startWelcome();
			}
		}
	};
	private ListView targetListView;
	private Button notifyButton;
	private Button addTargetButton;
	private Switch receiverSwitch;
	private EditText targetEditText;
	private Button importRosterButton;
	private EditText messageEditText;

	public static ConnectionConfiguration getConfig(String domain, int port) {
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(domain, port);
		connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		return connectionConfiguration;
	}

	public static void log(String message) {
		if (message == null) {
			Log.e("ADORNIS", "empty message (probably error)");
		} else {
			Log.e("ADORNIS", message);
		}
	}

	private void setupSwitch(final boolean check, final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				receiverSwitch.setChecked(check);
				receiverSwitch.setEnabled(enabled);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			updater = new AutoUpdater();
			updater.check();
		} catch (IOException e) {
			MainInterface.log("failed to fetch version");
		}


		setContentView(R.layout.main_activity);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		}
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_bar, null);
		if (actionBar != null) {
			actionBar.setCustomView(actionBarView);
		}

		receiverSwitch = (Switch) actionBarView.findViewById(R.id.receiverSwitch);
		targetListView = (ListView) findViewById(R.id.targetList);
		notifyButton = (Button) findViewById(R.id.notify);
		addTargetButton = (Button) findViewById(R.id.addTarget);
		targetEditText = (EditText) findViewById(R.id.targetText);
		importRosterButton = (Button) findViewById(R.id.importRoster);
		messageEditText = (EditText) findViewById(R.id.message);

		try {
			targetListView.setAdapter(new TargetListAdapter(this));
		} catch (Preferences.UserNotFoundException e) {
			MainInterface.log("FATAL - exception while trying to initiate ListView Adapter");
			e.printStackTrace();
		}
		targetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				TargetListAdapter adapter = (TargetListAdapter) targetListView.getAdapter();
				View currentView = null;

				if (adapter.getCurrent() != null) {
					try {
						currentView = targetListView.getChildAt(Preferences.getUserId(adapter.getCurrent().getJID()));
					} catch (Preferences.UserNotFoundException e) {
						e.printStackTrace();
					}
				}

				if (currentView != null) {
					currentView.findViewById(R.id.details).setVisibility(View.GONE);
					currentView.findViewById(R.id.invite).setVisibility(View.GONE);
				}

				if (currentView == view) {
					View details = view.findViewById(R.id.details);
					View invite = view.findViewById(R.id.invite);
					details.setVisibility(details.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
					invite.setVisibility(invite.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
					if (details.getVisibility() == View.GONE) {
						adapter.setCurrent(null);
					}
				} else {
					view.findViewById(R.id.details).setVisibility(View.VISIBLE);
					view.findViewById(R.id.invite).setVisibility(View.VISIBLE);
					adapter.setCurrent(Preferences.findUser(((TextView) view.findViewById(R.id.JID)).getText().toString()));
				}

				targetListUpdated();
			}
		});
		targetListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
				AlertDialog.Builder db = new AlertDialog.Builder(MainInterface.this);
				db.setTitle("Confirm");
				db.setMessage("Do you really want to remove " + Preferences.getUsers().get(position).getJID() + " from your contact list?");
				db.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				db.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							Preferences.delUser(Preferences.getUsers().get(position).getJID());
						} catch (Preferences.UserNotFoundException e) {
							MainInterface.log("User " + e + " wasn't found even though this should never happen while removing");
						}

						targetListUpdated();

						dialog.dismiss();
					}
				});
				AlertDialog dialog = db.create();
				dialog.show();
				return true;
			}
		});

		notifyButton.setEnabled(false);
		notifyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TargetUser current = ((TargetListAdapter) targetListView.getAdapter()).getCurrent();
				MessageConfiguration msgc = new MessageConfiguration(current.getJID(), messageEditText.getText().toString());
				msgc.setDelay(3, (TextView) findViewById(R.id.countdown));
				if (current.getOnlineStatus() == TargetUser.ONLINE) {
					listener.processMessage(msgc);
				} else {
					final MessageConfiguration temp = msgc;
					(new AlertDialog.Builder(MainInterface.this).setTitle("Warning").setMessage("This user is not online with notifier. Do you want to text him on a different device or application?").setPositiveButton("Yes, please!", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							listener.processMessage(temp);
						}
					}).setNegativeButton("No, thanks!", null)).create().show();
				}
			}
		});

		addTargetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String thatNewGuy = targetEditText.getText().toString().toLowerCase();
				if (!thatNewGuy.equals("")) {
					try {
						Preferences.addUser(thatNewGuy.trim());
						if (!listener.getRoster().contains(thatNewGuy)) {
							sendBroadcast(new Intent(Notifier.USER_PROPOSE_ROSTER).putExtra("JID", thatNewGuy));
						}
					} catch (User.InvalidJIDException e) {
						(new AlertDialog.Builder(MainInterface.this)).setTitle("Error").setMessage(e.getJID() + " is not a valid JID (user@domain) or the user exists already").setPositiveButton("OK", null).create().show();
					}
				}
			}
		});

		importRosterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (RosterEntry current : listener.getRoster().getEntries()) {
					if (Preferences.findUser(current.getUser()) == null) {
						try {
							Preferences.addUser(current.getUser(), current.getName());
						} catch (User.InvalidJIDException e) {
							// users in the roster can't be wrong
						}
					}
				}
			}
		});

		receiverSwitch.setChecked(Preferences.isConnected() == Listener.CONNECTED);
		receiverSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (receiverSwitch.isChecked()) {
					listener.attemptConnect();
				} else {
					listener.disconnect();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Preferences.class));
				return true;
			case R.id.sort_alphabetically:
				if (Preferences.sortUsers(Preferences.ALPHABETICALLY)) {
					item.setTitle("sort z->a");
				} else {
					item.setTitle("sort a->z");
				}
				return true;
			case R.id.sort_status:
				Preferences.sortUsers(Preferences.ONLINE_STATUS);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void targetListUpdated() {
		((TargetListAdapter) targetListView.getAdapter()).notifyDataSetChanged();
		notifyButton.setEnabled(((TargetListAdapter) targetListView.getAdapter()).getCurrent() != null);
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(stopReceiver, new IntentFilter(Notifier.STOP));
		registerReceiver(updateReceiver, new IntentFilter(Notifier.UPDATE_AVAILABLE));
		registerReceiver(listProposeReceiver, new IntentFilter(Notifier.USER_PROPOSE_LIST));
		registerReceiver(rosterProposeReceiver, new IntentFilter(Notifier.USER_PROPOSE_ROSTER));
		registerReceiver(serviceStateReceiver, new IntentFilter(Notifier.SERVICE));
		registerReceiver(userChangeReceiver, new IntentFilter(Notifier.USER_CHANGE));
		registerReceiver(userEventReceiver, new IntentFilter(Notifier.USER_EVENT));
		registerReceiver(invitationReceiver, new IntentFilter(Notifier.INVITATION));

		if (Preferences.isConnected() == Listener.DISCONNECTED) {
			startService(new Intent(this, Listener.class));
		}
		bindService(new Intent(this, Listener.class), listenerConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(stopReceiver);
		unregisterReceiver(updateReceiver);
		unregisterReceiver(listProposeReceiver);
		unregisterReceiver(rosterProposeReceiver);
		unregisterReceiver(userChangeReceiver);
		unregisterReceiver(userEventReceiver);
		unregisterReceiver(serviceStateReceiver);
		unregisterReceiver(invitationReceiver);

		Preferences.close();
		targetListUpdated();
		unbindService(listenerConnection);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Preferences.close();
	}
}