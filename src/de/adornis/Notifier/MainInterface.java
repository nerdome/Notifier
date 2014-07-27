package de.adornis.Notifier;

import android.app.Activity;
import android.app.IntentService;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;

import java.util.ArrayList;

public class MainInterface extends Activity {

    public final static String USER = "yorrd";
    public final static String PASSWORD = "123vorbei";

    private String currentTarget = "";
    private static ArrayList<String> targets = new ArrayList<String>();
    static {
        targets.add("yorrd@adornis.de");
        targets.add("fightcookie@adornis.de");
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        findViewById(R.id.notify).setEnabled(false);

        ListView lv = (ListView) findViewById(R.id.targetList);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, targets));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentTarget = targets.get(position);
                findViewById(R.id.notify).setEnabled(true);
            }
        });
        lv.setSelection(1);

        findViewById(R.id.notify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainInterface.this, Sender.class);
                i.putExtra("RECEIVER", currentTarget);
                startService(i);
            }
        });

        findViewById(R.id.addTarget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targets.add(((EditText) findViewById(R.id.targetText)).getText().toString());
            }
        });

        ((Switch) findViewById(R.id.receiver)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(MainInterface.this, Listener.class));
                } else {
                    stopService(new Intent(MainInterface.this, Listener.class));
                }
            }
        });

        ((Switch) findViewById(R.id.receiver)).setChecked(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, Sender.class), senderServiceConnection, IntentService.BIND_AUTO_CREATE);
        registerReceiver(notConnectedReceiver, new IntentFilter("LISTENER_NOT_CONNECTED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(senderServiceConnection);
        unregisterReceiver(notConnectedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, Listener.class));
    }

    static void log(String message) {
        if(message == null) {
            Log.e("ADORNIS", "empty message (probably error)");
        } else {
            Log.e("ADORNIS", message);
        }
    }
}