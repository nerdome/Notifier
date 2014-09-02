package de.adornis.Notifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import java.io.IOException;
import java.util.Map;

public class Listener extends Service {
    XMPPTCPConnection conn = null;

	public static boolean running  = false;

    private AsyncTask<ConnectionConfiguration, String, Void> xmppWorkerThread = new AsyncTask<ConnectionConfiguration, String, Void>() {

        @Override
        protected Void doInBackground(ConnectionConfiguration... params) {
	        running = true;

            conn = new XMPPTCPConnection(params[0]);
            try {
	            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	            String user = prefs.getString("user", "yorrd@adornis.de");
	            String password = prefs.getString("password", "123vorbei");

                conn.connect();
                conn.login(user.substring(0, user.indexOf('@')), password, "NOTIFIER_RECEIVER");
	            conn.sendPacket(new Presence(Presence.Type.available, "awaiting notifier notifications", 0, Presence.Mode.xa));

                ChatManager.getInstanceFor(conn).addChatListener(new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean createdLocally) {
                        if (!createdLocally) {
                            chat.addMessageListener(new MessageListener() {
                                @Override
                                public void processMessage(Chat chat, Message message) {
                                    Map<String, Object> props = JivePropertiesManager.getProperties(message);
                                    if (props.containsKey("ALARM")) {
                                        publishProgress((String) props.get("ALARM"));
                                    }

                                    // echo for testing
                                    Message msg = new Message();
                                    JivePropertiesManager.addProperty(msg, "ALARM", "wake");
                                    try {
                                        chat.sendMessage(msg);
                                    } catch (SmackException.NotConnectedException e) {
                                        MainInterface.log("Wasn't connected in Listener --> ChatListener" + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                });
            } catch (SmackException e) {
                MainInterface.log("SmackException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_NOT_CONNECTED"));
                disconnect();
            } catch (IOException e) {
                MainInterface.log("IOException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_NOT_CONNECTED"));
                disconnect();
            } catch (XMPPException e) {
                MainInterface.log("XMPPException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_NOT_CONNECTED"));
                disconnect();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Intent i = new Intent(getApplicationContext(), NoiseMakerActivity.class);
            i.putExtra("DURATION", 13);
            i.putExtra("MESSAGE", values[0]);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        xmppWorkerThread.execute(MainInterface.connectionConfiguration);

        flags = START_STICKY;
        Notification.Builder nb = new Notification.Builder(getApplicationContext());
        nb.setDefaults(Notification.DEFAULT_ALL);
        startForeground(1234, nb.build());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    private void disconnect() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    conn.disconnect();
                } catch (SmackException.NotConnectedException e) {
                    MainInterface.log("NotConnectedException in Listener --> onCancelled()" + e.getMessage());
                }
            }
        })).start();
    }
}
