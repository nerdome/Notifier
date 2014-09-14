package de.adornis.Notifier;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;

public class MainInterface extends Activity {

	private Preferences prefs;

    private TargetUser currentTarget;

	private Sender.SenderServiceBinder senderService;

	public static ConnectionConfiguration getConfig(String domain, int port) {
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(domain, port);
        connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
	    return connectionConfiguration;
    }

    private ServiceConnection senderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
			senderService = (Sender.SenderServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BroadcastReceiver connectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
	        receiverSwitch.setChecked(prefs.isListenerRunning());
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

				        unbindService(senderServiceConnection);
				        stopService(new Intent(MainInterface.this, Sender.class));
				        bindService(new Intent(MainInterface.this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);

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
		        if(currentTarget.isOnline() == TargetUser.ONLINE) {
			        Intent i = new Intent(MainInterface.this, Sender.class);
			        i.putExtra("RECEIVER", currentTarget.getJID());
			        i.putExtra("MESSAGE", messageEditText.getText().toString());
			        i.putExtra("TYPE", Sender.DEFAULT);
			        startService(i);
		        } else {
			        (new AlertDialog.Builder(MainInterface.this)
					        .setTitle("Warning")
					        .setMessage("This user is not online with notifier. Do you want to text him on a different device or application?")
					        .setPositiveButton("Yes, please!", new DialogInterface.OnClickListener() {
						        @Override
						        public void onClick(DialogInterface dialog, int which) {
							        Intent i = new Intent(MainInterface.this, Sender.class);
							        i.putExtra("RECEIVER", currentTarget.getJID());
							        i.putExtra("MESSAGE", messageEditText.getText().toString());
							        i.putExtra("TYPE", Sender.DEFAULT);
							        startService(i);
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
			        } catch (InvalidJIDException e) {
	                    (new AlertDialog.Builder(MainInterface.this)).setTitle("Error").setMessage("This is not a valid JID (user@domain) or the user exists already").setPositiveButton("OK", null).create().show();
			        }
		        }
		        stopService(new Intent(MainInterface.this, Listener.class));
		        startService(new Intent(MainInterface.this, Listener.class));
	        }
        });

		importRosterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (RosterEntry current : senderService.getRoster().getEntries()) {
					if(prefs.findUser(current.getUser()) == null) {
						try {
							prefs.addUser(current.getUser(), current.getName());
						} catch (InvalidJIDException e) {
							// users in the roster can't be wrong
						}
					}
				}
				stopService(new Intent(MainInterface.this, Listener.class));
				startService(new Intent(MainInterface.this, Listener.class));
			}
		});

		receiverSwitch.setChecked(prefs.isListenerRunning());
		receiverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	        @Override
	        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked && !prefs.isListenerRunning()) {
			        startService(new Intent(MainInterface.this, Listener.class));
			        log("starting listener");
		        } else if (!isChecked && prefs.isListenerRunning()) {
			        stopService(new Intent(MainInterface.this, Listener.class));
			        log("stopping listener");
		        }
	        }
        });

		prefs.registerPreferenceListener(new PreferenceListener() {
			@Override
			public void onPreferenceChanged(String type) {
				switch(type) {
					case PreferenceListener.CREDENTIALS:
						stopService(new Intent(MainInterface.this, Listener.class));
						startService(new Intent(MainInterface.this, Listener.class));
						break;
					case PreferenceListener.USER_LIST:
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								targetListUpdated();
							}
						});
						break;
					case PreferenceListener.STOP:
						MainInterface.log("got stop command");
						finish();
						break;
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
		if(currentTarget != null) {
			try {
				targetListView.getChildAt(prefs.getUserId(currentTarget.getJID())).findViewById(R.id.nick).setVisibility(View.GONE);
			} catch (UserNotFoundException e) {
				// won't happen
				MainInterface.log("User " + e.getUser() + " wasn't found here which should never have happened");
			}
		}
    }

    @Override
    protected void onResume() {
        super.onResume();
	    if(!prefs.isListenerRunning() && prefs.isAutoStart()) {
		    startService(new Intent(this, Listener.class));
	    }
        bindService(new Intent(this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
        registerReceiver(connectedReceiver, new IntentFilter("LISTENER_CONNECTED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
	    prefs.close();
        targetListUpdated();
        unbindService(senderServiceConnection);
        unregisterReceiver(connectedReceiver);
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