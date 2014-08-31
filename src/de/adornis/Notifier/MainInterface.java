package de.adornis.Notifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;

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

    private BroadcastReceiver notConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((Switch) findViewById(R.id.receiver)).setChecked(false);
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
				if(targets.get(i).equals("ONLINE : " + user) || targets.get(i).equals("OFFLINE : " + user) || targets.get(i).equals(user)) {
					if(targets.get(i).contains(" : ")) {
						targets.set(i, (online ? "ONLINE : " : "OFFLINE : ") + targets.get(i).substring(targets.get(i).indexOf(" : ") + 3));
					} else {
						targets.set(i, (online ? "ONLINE : " : "OFFLINE : ") + targets.get(i));
					}
				}
			}
		}
	};

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        targets = getTargets();

        findViewById(R.id.notify).setEnabled(false);

        ListView lv = (ListView) findViewById(R.id.targetList);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, targets));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentTarget = targets.get(position);
	            if(currentTarget.contains(" : ")) {
		            currentTarget = currentTarget.substring(currentTarget.indexOf(" : ") + 3);
	            }
                findViewById(R.id.notify).setEnabled(true);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String toRemove = ((TextView) view).getText().toString();
                targets.remove(toRemove);
                targetListUpdated();
                return true;
            }
        });
        lv.setSelection(1);

        findViewById(R.id.notify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainInterface.this, Sender.class);
                i.putExtra("RECEIVER", currentTarget);
                i.putExtra("MESSAGE", ((EditText) findViewById(R.id.message)).getText().toString());
                startService(i);
            }
        });

		findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				unbindService(senderServiceConnection);
				stopService(new Intent(MainInterface.this, Sender.class));
				bindService(new Intent(MainInterface.this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
				targetListUpdated();
			}
		});

        findViewById(R.id.addTarget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targets.add(((EditText) findViewById(R.id.targetText)).getText().toString());
                ((EditText) findViewById(R.id.targetText)).setText("");
                targetListUpdated();
            }
        });

		if(Listener.running) {
			((Switch) findViewById(R.id.receiver)).setChecked(true);
		}
        ((Switch) findViewById(R.id.receiver)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !Listener.running) {
	                startService(new Intent(MainInterface.this, Listener.class));
	                log("starting listener");
	                Listener.running = true;
                } else if(isChecked && Listener.running) {
                } else {
                    stopService(new Intent(MainInterface.this, Listener.class));
	                log("stopping listener");
	                Listener.running = false;
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
				startActivity(new Intent(this, PreferenceEditor.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void targetListUpdated() {
        ListView lv = (ListView) findViewById(R.id.targetList);
        if(lv.getAdapter() instanceof ArrayAdapter) {
            ArrayAdapter adapter = (ArrayAdapter) lv.getAdapter();
            adapter.notifyDataSetChanged();
        } else {
            MainInterface.log("Something went horribly wrong while updating the Adapter which wasn't an ArrayAdapter as expected");
        }

        Iterator<String> input = targets.iterator();
        Set<String> output = new HashSet<>();
        while(input.hasNext()) {
	        String toBeAdded = input.next();
	        if(toBeAdded.contains(" : ")) {
		        output.add(toBeAdded.substring(toBeAdded.indexOf(" : ") + 3));
	        } else {
		        output.add(toBeAdded);
	        }
        }
        prefs.edit().putStringSet("accounts", output).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
        registerReceiver(notConnectedReceiver, new IntentFilter("LISTENER_NOT_CONNECTED"));
	    registerReceiver(rosterReceiver, new IntentFilter("ROSTER"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        targetListUpdated();
        unbindService(senderServiceConnection);
        unregisterReceiver(notConnectedReceiver);
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
        defaultTargets.add("yorrd@adornis.de");
        defaultTargets.add("fightcookie@adornis.de");
        Iterator<String> input = prefs.getStringSet("accounts", defaultTargets).iterator();
        ArrayList<String> output = new ArrayList<>();
        while(input.hasNext()) {
            output.add(input.next());
        }
        return output;
    }
}