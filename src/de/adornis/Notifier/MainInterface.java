package de.adornis.Notifier;

import android.app.Activity;
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
	private EditText messageEditText;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        targets = getTargets();

		targetListView = (ListView) findViewById(R.id.targetList);
		notifyButton = (Button) findViewById(R.id.notify);
		refreshButton = (Button) findViewById(R.id.refresh);
		addTargetButton = (Button) findViewById(R.id.addTarget);
		receiverSwitch = (Switch) findViewById(R.id.receiver);
		targetEditText = (EditText) findViewById(R.id.targetText);
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
	        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		        String toRemove = ((TextView) view).getText().toString();
		        targets.remove(toRemove);
		        targetListUpdated();
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
		        i.putExtra("TYPE", Sender.TIMED);
		        i.putExtra("timer_duration", 5);
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
		        targets.add(targetEditText.getText().toString());
		        targetEditText.setText("");
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
        registerReceiver(notConnectedReceiver, new IntentFilter("LISTENER_NOT_CONNECTED"));
	    registerReceiver(rosterReceiver, new IntentFilter("ROSTER"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        targetListUpdated();
	    prefs.edit().putBoolean("receiver_online", Listener.running).commit();
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