package de.adornis.Notifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MainInterface extends Activity implements DialogInterface.OnDismissListener {

    public static String USER = "yorrd@adornis.de";
    public static String PASSWORD = "123vorbei";

    SharedPreferences prefs;
    private String currentTarget = "";
    private ArrayList<String> targets;

    private String self = "yorrd@adornis.de";

    private AlertDialog credentialsDialog;
    private DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    // actually doesn't do anything different than dismiss, there is no good way to not call onDismiss()
                    // putting in existing values as a halfway workaround
                    dialog.cancel();
                case DialogInterface.BUTTON_POSITIVE:
                    dialog.dismiss();
                default:
                    dialog.cancel();
            }
        }
    };

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

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        targets = getTargets();
        USER = prefs.getString("user", "yorrd@adornis.de");
        PASSWORD = prefs.getString("password", "123vorbei");

        findViewById(R.id.notify).setEnabled(false);

        ((Button) findViewById(R.id.self)).setText(self);
        findViewById(R.id.self).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder db = new AlertDialog.Builder(MainInterface.this);
                db.setTitle("Account...");
                db.setView(MainInterface.this.getLayoutInflater().inflate(R.layout.change_user_popup, null));
                db.setOnDismissListener(MainInterface.this);
                db.setPositiveButton("OK", dialogListener);
                db.setNegativeButton("Cancel", dialogListener);
                credentialsDialog = db.create();
                credentialsDialog.show();
                ((EditText) credentialsDialog.findViewById(R.id.account)).setText(USER);
                ((EditText) credentialsDialog.findViewById(R.id.password)).setText(PASSWORD);
            }
        });

        ListView lv = (ListView) findViewById(R.id.targetList);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, targets));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentTarget = targets.get(position);
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

        findViewById(R.id.addTarget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targets.add(((EditText) findViewById(R.id.targetText)).getText().toString());
                ((EditText) findViewById(R.id.targetText)).setText("");
                targetListUpdated();
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
            output.add(input.next());
        }
        prefs.edit().putStringSet("accounts", output).apply();
    }

    private void credentialsUpdated() {
        prefs.edit().putString("user", USER).apply();
        prefs.edit().putString("password", PASSWORD).apply();
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
        credentialsUpdated();
        targetListUpdated();
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        MainInterface.log("dismissing");
        MainInterface.USER = ((EditText) credentialsDialog.findViewById(R.id.account)).getText().toString();
        MainInterface.PASSWORD = ((EditText) credentialsDialog.findViewById(R.id.password)).getText().toString();
        ((Button) findViewById(R.id.self)).setText(MainInterface.USER);
        ((Switch) findViewById(R.id.receiver)).setChecked(false);
        credentialsUpdated();
    }
}