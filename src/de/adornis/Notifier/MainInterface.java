package de.adornis.Notifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MainInterface extends Activity {

    SharedPreferences prefs;
    private String currentTarget = "";
    private ArrayList<String> targets;

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
	        receiverSwitch.setChecked(Listener.running);
        }
    };

	private BroadcastReceiver rosterReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String user = "";
			boolean online = false;
			if(intent.getStringExtra("ONLINE") != null) {
				user = intent.getStringExtra("ONLINE");
				online = true;
			} else if(intent.getStringExtra("OFFLINE") != null) {
				user = intent.getStringExtra("OFFLINE");
				online = false;
			}
			for(int i = 0; i < targets.size(); i++) {
				if(targets.get(i).equals(user)) {
					((ListView) findViewById(R.id.targetList)).getChildAt(i).setBackgroundColor(online ? Color.rgb(180,100,100) : Color.rgb(100,180,100));
				}
			}
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        targets = getTargets();

		if(prefs.getString("user", "???").equals("???")) {
			startActivity(new Intent(this, FirstStart.class));
			finish();
		}

		targetListView = (ListView) findViewById(R.id.targetList);
		notifyButton = (Button) findViewById(R.id.notify);
		refreshButton = (Button) findViewById(R.id.refresh);
		addTargetButton = (Button) findViewById(R.id.addTarget);
		receiverSwitch = (Switch) findViewById(R.id.receiver);
		targetEditText = (EditText) findViewById(R.id.targetText);
		importRosterButton = (Button) findViewById(R.id.importRoster);
		messageEditText = (EditText) findViewById(R.id.message);

		targetListView.setBackgroundColor(Color.rgb(100, 100, 180));
        targetListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, targets));
        targetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		        currentTarget = targets.get(position);
		        notifyButton.setEnabled(true);
	        }
        });
        targetListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
	        @Override
	        public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
		        AlertDialog.Builder db = new AlertDialog.Builder(MainInterface.this);
		        db.setTitle("Confirm");
		        db.setMessage("Do you really want to remove " + ((TextView) view).getText().toString() + " from your contact list?");
		        db.setNegativeButton("Don't remove", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {
				        dialog.dismiss();
			        }
		        });
		        db.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {

				        String toRemove = ((TextView) view).getText().toString();
				        targets.remove(toRemove);

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
		        i.putExtra("RECEIVER", currentTarget);
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
		        if(thatNewGuy.contains("@") && thatNewGuy.contains(".")) {
			        targets.add(thatNewGuy);
		        } else {
			        // this works, passing null as listener will make it do nothing on click which is exactly what we want for this OK button
			        (new AlertDialog.Builder(MainInterface.this)).setTitle("Error").setMessage("This is not a valid JID (user@domain)").setPositiveButton("OK", null).create().show();
		        }
		        targetEditText.setText("");
		        targetListUpdated();
	        }
        });

		importRosterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for(RosterEntry current : Sender.roster.getEntries()) {
					if(!targets.contains(current.getUser().toLowerCase())) {
						targets.add(current.getUser());
					}
				}
				targetListUpdated();
			}
		});

        receiverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	        @Override
	        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked && !Listener.running) {
			        startService(new Intent(MainInterface.this, Listener.class));
			        log("starting listener");
			        Listener.running = true;
			        prefs.edit().putBoolean("receiver_online", true).commit();
		        } else if (isChecked && Listener.running) {
		        } else {
			        stopService(new Intent(MainInterface.this, Listener.class));
			        log("stopping listener");
			        Listener.running = false;
			        prefs.edit().putBoolean("receiver_online", false).commit();
		        }
	        }
        });
		if(Listener.running) {
			receiverSwitch.setChecked(true);
		}
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
				startActivity(new Intent(this, PreferenceEditor.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void targetListUpdated() {
        if(targetListView.getAdapter() instanceof ArrayAdapter) {
            ArrayAdapter adapter = (ArrayAdapter) targetListView.getAdapter();
            adapter.notifyDataSetChanged();
        } else {
            MainInterface.log("Something went horribly wrong while updating the Adapter which wasn't an ArrayAdapter as expected");
        }

        Iterator<String> input = targets.iterator();
        Set<String> output = new HashSet<>();
        while(input.hasNext()) {
	        output.add(input.next());
        }
        prefs.edit().putStringSet("accounts", output).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
	    if(prefs.getBoolean("receiver_online", false) && !Listener.running) {
		    startService(new Intent(this, Listener.class));
	    }
        bindService(new Intent(this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
        registerReceiver(connectedReceiver, new IntentFilter("LISTENER_CONNECTED"));
	    registerReceiver(rosterReceiver, new IntentFilter("ROSTER"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        targetListUpdated();
	    prefs.edit().putBoolean("receiver_online", Listener.running).commit();
        unbindService(senderServiceConnection);
        unregisterReceiver(connectedReceiver);
	    unregisterReceiver(rosterReceiver);
    }

    static void log(String message) {
        if(message == null) {
            Log.e("ADORNIS", "empty message (probably error)");
        } else {
            Log.e("ADORNIS", message);
        }
    }

    private ArrayList<String> getTargets() {
        Set<String> defaultTargets = new HashSet<>();
        defaultTargets.add(prefs.getString("user", "this should never happen"));
        Iterator<String> input = prefs.getStringSet("accounts", defaultTargets).iterator();
        ArrayList<String> output = new ArrayList<>();
        while(input.hasNext()) {
            output.add(input.next());
        }
        return output;
    }
}