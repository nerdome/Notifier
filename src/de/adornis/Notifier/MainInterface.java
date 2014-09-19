package de.adornis.Notifier;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;

import java.io.IOException;

public class MainInterface extends Activity {

	private Preferences prefs;
	private AutoUpdater updater;

    private TargetUser currentTarget;

	public static ConnectionConfiguration getConfig(String domain, int port) {
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(domain, port);
		connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		return connectionConfiguration;
	}

	private Listener listener;
    private ServiceConnection listenerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
			listener = ((Listener) ((Listener.ListenerBinder) service).getService());
	        if(prefs.isAutoStart()) {
		        listener.attemptConnect();
	        }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

	private ListView targetListView;
	private Button notifyButton;
	private Button addTargetButton;
	private Switch receiverSwitch;
	private EditText targetEditText;
	private Button importRosterButton;
	private EditText messageEditText;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MainInterface.log("Starting MainInterface...");

		try {
			Preferences.initialize(this);
		} catch (UserNotFoundException e) {
			startActivity(new Intent(this, FirstStart.class));
			finish();
			return;
		}

		try {
			prefs = new Preferences();
		} catch (Preferences.NotInitializedException e) {
			MainInterface.log("FATAL");
			e.printStackTrace();
		}

		PreferenceListener.registerListener(new PreferenceListener() {

			@Override
			public void onCredentialsChanged() {

			}

			@Override
			public void onUserAdd(String... JID) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						targetListUpdated();
					}
				});
			}

			@Override
			public void onUserProposeList(final String JID) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(MainInterface.this);
						builder.setTitle(JID);
						builder.setMessage("Do you want to add this user from your roster to your target list?");
						builder.setNegativeButton("No", null);
						builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									prefs.addUser(JID);
								} catch (InvalidJIDException e) {
									MainInterface.log("this should never happen, JIDs from the roster should not be wrong");
								}
							}
						});
						builder.create().show();
					}
				});
			}

			@Override
			public void onUserProposeRoster(final String JID) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
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

			@Override
			public void onUserChanged(String... JID) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						targetListUpdated();
					}
				});
			}

			@Override
			public void onStopCommand() {
				MainInterface.log("got stop command");
				finish();
			}

			@Override
			public void onServiceStateChanged() {
				if(prefs != null) {
					switch (prefs.isConnected()) {
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
				} else {
					setupSwitch(false, false);
				}
			}

			@Override
			public void onUpdateAvailable() {
				updater.update(MainInterface.this);
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
		});

		try {
			updater = new AutoUpdater(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
			updater.check();
		} catch (PackageManager.NameNotFoundException e) {
			// lawl this doesn't exist... yes it does -.-
		} catch (IOException e) {
			MainInterface.log("failed to fetch version");
		}


        setContentView(R.layout.main_activity);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_bar, null);
		actionBar.setCustomView(actionBarView);

		receiverSwitch = (Switch) actionBarView.findViewById(R.id.receiverSwitch);
		targetListView = (ListView) findViewById(R.id.targetList);
		notifyButton = (Button) findViewById(R.id.notify);
		addTargetButton = (Button) findViewById(R.id.addTarget);
		targetEditText = (EditText) findViewById(R.id.targetText);
		importRosterButton = (Button) findViewById(R.id.importRoster);
		messageEditText = (EditText) findViewById(R.id.message);

        targetListView.setAdapter(new TargetListAdapter(this));
        targetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		        try {
			        if(currentTarget != null) {
				        targetListView.getChildAt(prefs.getUserId(currentTarget.getJID())).findViewById(R.id.JID).setVisibility(View.GONE);
			        }
		        } catch (UserNotFoundException e) {
			        MainInterface.log(e.getMessage());
		        }

		        currentTarget = (TargetUser) targetListView.getAdapter().getItem(position);
		        view.findViewById(R.id.JID).setVisibility(View.VISIBLE);
		        notifyButton.setEnabled(true);
	        }
        });
        targetListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
	        @Override
	        public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
		        AlertDialog.Builder db = new AlertDialog.Builder(MainInterface.this);
		        db.setTitle("Confirm");
		        db.setMessage("Do you really want to remove " + prefs.getUsers().get(position).getJID() + " from your contact list?");
		        db.setNegativeButton("Don't remove", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {
				        dialog.dismiss();
			        }
		        });
		        db.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {

				        try {
					        prefs.delUser(prefs.getUsers().get(position).getJID());
				        } catch (UserNotFoundException e) {
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
        targetListView.setSelection(1);

		notifyButton.setEnabled(false);
        notifyButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
		        MessageConfiguration msgc = new MessageConfiguration(currentTarget.getJID(), messageEditText.getText().toString());
		        if(currentTarget.getOnlineStatus() == TargetUser.ONLINE) {
			        listener.processMessage(msgc);
		        } else {
			        final MessageConfiguration temp = msgc;
			        (new AlertDialog.Builder(MainInterface.this)
					        .setTitle("Warning")
					        .setMessage("This user is not online with notifier. Do you want to text him on a different device or application?")
					        .setPositiveButton("Yes, please!", new DialogInterface.OnClickListener() {
						        @Override
						        public void onClick(DialogInterface dialog, int which) {
							        listener.processMessage(temp);
						        }
					        }).setNegativeButton("No, thanks!", null)
			        ).create().show();
		        }
	        }
        });

        addTargetButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
		        String thatNewGuy = targetEditText.getText().toString().toLowerCase();
		        if(!thatNewGuy.equals("")) {
			        try {
				        prefs.addUser(thatNewGuy.trim());
				        if(!listener.getRoster().contains(thatNewGuy)) {
					        PreferenceListener.notifyAll(PreferenceListener.USER_PROPOSE_ROSTER, thatNewGuy);
				        }
			        } catch (InvalidJIDException e) {
	                    (new AlertDialog.Builder(MainInterface.this)).setTitle("Error").setMessage("This is not a valid JID (user@domain) or the user exists already").setPositiveButton("OK", null).create().show();
			        }
		        }
	        }
        });

		importRosterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (RosterEntry current : listener.getRoster().getEntries()) {
					if(prefs.findUser(current.getUser()) == null) {
						try {
							prefs.addUser(current.getUser(), current.getName());
						} catch (InvalidJIDException e) {
							// users in the roster can't be wrong
						}
					}
				}
			}
		});

		receiverSwitch.setChecked(prefs.isConnected() == Listener.CONNECTED);
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
		switch(item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Preferences.class));
				return true;
			case R.id.sort_alphabetically:
				Preferences.sortUsers(Preferences.ALPHABETICALLY);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
	    if(prefs.isConnected() == Listener.DISCONNECTED) {
		    startService(new Intent(this, Listener.class));
	    }
	    bindService(new Intent(this, Listener.class), listenerConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
	    prefs.close();
        targetListUpdated();
	    unbindService(listenerConnection);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(prefs != null) {
			prefs.close();
		}
	}

	public static void log(String message) {
        if(message == null) {
            Log.e("ADORNIS", "empty message (probably error)");
        } else {
            Log.e("ADORNIS", message);
        }
    }
}