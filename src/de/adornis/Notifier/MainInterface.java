package de.adornis.Notifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;

public class MainInterface extends Activity {

	private Preferences prefs;

    private User currentTarget;

	public final static String ROSTER = "de.adornis.Notifier.ROSTER";

    public static ConnectionConfiguration connectionConfiguration;
    static {
        connectionConfiguration = new ConnectionConfiguration("adornis.de", 5222);
        connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
    }

    private ServiceConnection senderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BroadcastReceiver connectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
	        receiverSwitch.setChecked(Listener.isRunning());
        }
    };

	private BroadcastReceiver rosterReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String user = intent.getStringExtra("user");
			int online = intent.getIntExtra("status", TargetUser.NOT_IN_ROSTER);

			if(!user.equals("")) {
				try {
					prefs.findUser(user).setOnline(online);
				} catch (Exception e) {
				}
			}
			targetListUpdated();
		}
	};

	private ListView targetListView;
	private Button notifyButton;
	private Button refreshButton;
	private Button addTargetButton;
	private Switch receiverSwitch;
	private EditText targetEditText;
	private Button importRosterButton;
	private EditText messageEditText;

	@Override
    public void onCreate(Bundle savedInstanceState) {

		// initialize the pref class for the rest of the application
		Preferences.initialize(this);

		try {
			prefs = new Preferences();
			if(prefs.getAppUser() == null) {
				startActivity(new Intent(this, FirstStart.class));
				finish();
			}
		} catch (Exception e) {
			// might as well stop, application basically doesn't work anymore
			finish();
		}

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

		targetListView = (ListView) findViewById(R.id.targetList);
		notifyButton = (Button) findViewById(R.id.notify);
		refreshButton = (Button) findViewById(R.id.refresh);
		addTargetButton = (Button) findViewById(R.id.addTarget);
		receiverSwitch = (Switch) findViewById(R.id.receiver);
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
		        } catch (Exception e) {
			        e.printStackTrace();
		        }
		        currentTarget = (User) targetListView.getAdapter().getItem(position);
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
				        } catch (Exception e) {
					        // no such user should never happen since it has been added and was checked
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
		        Intent i = new Intent(MainInterface.this, Sender.class);
		        i.putExtra("RECEIVER", currentTarget.getJID());
		        i.putExtra("MESSAGE", messageEditText.getText().toString());
		        i.putExtra("TYPE", Sender.DEFAULT);
		        startService(i);
	        }
        });

		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				unbindService(senderServiceConnection);
				stopService(new Intent(MainInterface.this, Sender.class));
				bindService(new Intent(MainInterface.this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
				targetListUpdated();
			}
		});

        addTargetButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
		        String thatNewGuy = targetEditText.getText().toString().toLowerCase();
		        try {
			        prefs.addUser(thatNewGuy.trim());
		        } catch (Exception e) {
			        (new AlertDialog.Builder(MainInterface.this)).setTitle("Error").setMessage("This is not a valid JID (user@domain)").setPositiveButton("OK", null).create().show();
		        }
		        targetEditText.setText("");
		        targetListUpdated();
	        }
        });

		importRosterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(Sender.roster != null) {
					for (RosterEntry current : Sender.roster.getEntries()) {
						try {
							prefs.findUser(current.getUser());
						} catch (Exception e) {
							// user wasn't found, try to add
							try {
								prefs.addUser(current.getUser(), current.getName());
							} catch (Exception e1) {
								// users in the roster can't be wrong
							}
						}
					}
				}
				unbindService(senderServiceConnection);
				stopService(new Intent(MainInterface.this, Sender.class));
				bindService(new Intent(MainInterface.this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
				targetListUpdated();
			}
		});

        receiverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	        @Override
	        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked && !Listener.isRunning()) {
			        startService(new Intent(MainInterface.this, Listener.class));
			        log("starting listener");
		        } else if (!isChecked && Listener.isRunning()) {
			        stopService(new Intent(MainInterface.this, Listener.class));
			        log("stopping listener");
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
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void targetListUpdated() {
		((TargetListAdapter) targetListView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
	    if(!Listener.isRunning()) {
		    startService(new Intent(this, Listener.class));
	    }
        bindService(new Intent(this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
        registerReceiver(connectedReceiver, new IntentFilter("LISTENER_CONNECTED"));
	    registerReceiver(rosterReceiver, new IntentFilter(ROSTER));
    }

    @Override
    protected void onPause() {
        super.onPause();
	    prefs.close();
        targetListUpdated();
        unbindService(senderServiceConnection);
        unregisterReceiver(connectedReceiver);
	    unregisterReceiver(rosterReceiver);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		prefs.close();
	}

	static void log(String message) {
        if(message == null) {
            Log.e("ADORNIS", "empty message (probably error)");
        } else {
            Log.e("ADORNIS", message);
        }
    }
}