package de.adornis.Notifier;

import android.app.Notification;
import android.app.Service;
import android.content.*;
import android.os.AsyncTask;
import android.os.IBinder;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Listener extends Service {
    private XMPPTCPConnection conn = null;

	private static boolean running  = false;

	private Preferences prefs;

    private AsyncTask<ConnectionConfiguration, String, Void> xmppWorkerThread = new AsyncTask<ConnectionConfiguration, String, Void>() {

        @Override
        protected Void doInBackground(ConnectionConfiguration... params) {

	        conn = new XMPPTCPConnection(params[0]);
            try {
	            conn.getRoster().addRosterListener(new RosterListener() {
		            @Override
		            public void entriesAdded(Collection<String> addresses) {

		            }

		            @Override
		            public void entriesUpdated(Collection<String> addresses) {

		            }

		            @Override
		            public void entriesDeleted(Collection<String> addresses) {

		            }

		            @Override
		            public void presenceChanged(Presence presence) {
			            try {
				            prefs.getUser(presence.getFrom().substring(0, presence.getFrom().indexOf("/"))).updatePresence(presence);
			            } catch (UserNotFoundException e) {
				            MainInterface.log("User " + e.getUser() + " wasn't found");
			            }
		            }
	            });

                conn.connect();
                conn.login(prefs.getAppUser().getJID().substring(0, prefs.getAppUser().getJID().indexOf('@')), prefs.getAppUser().getPassword(), "NOTIFIER_RECEIVER");
	            conn.sendPacket(new Presence(Presence.Type.available, "awaiting notifier notifications", 0, Presence.Mode.xa));

	            setRunning(conn.isConnected());
	            sendBroadcast(new Intent("LISTENER_CONNECTED"));

	            for(RosterEntry current : conn.getRoster().getEntries()) {
		            try {
			            prefs.getUser(current.getUser()).updatePresence(conn.getRoster().getPresence(current.getUser()));
		            } catch (UserNotFoundException e) {
			            MainInterface.log(e.getMessage());
		            }
	            }

	            for(TargetUser current : prefs.getUsers()) {
		            Message msg = new Message();
		            msg.setTo(current.getJID() + "NOTIFIER_RECEIVER");
		            JivePropertiesManager.addProperty(msg, "PING", "request");
		            conn.sendPacket(msg);
	            }

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
                                    } else if(props.containsKey("PING")) {
										if(props.get("PING").equals("request")) {
											Message msg = new Message();
	                                        JivePropertiesManager.addProperty(msg, "PING", "reply");
											try {
												chat.sendMessage(msg);
											} catch (SmackException.NotConnectedException e) {
												MainInterface.log("Couldn't ping back because there was a connection issue");
												e.printStackTrace();
											}
										} else if(props.get("PING").equals("reply")) {
											try {
												prefs.getUser(message.getFrom().substring(0, message.getFrom().indexOf("/"))).incomingPing();
											} catch (UserNotFoundException e) {
												MainInterface.log("User " + e.getUser() + " pinged back but isn't on the list");
											}
										}
	                                } else {
	                                    returnMessage(message);
                                    }
                                }
                            });
                        }
                    }
                });
            } catch (SmackException e) {
                MainInterface.log("SmackException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_CONNECTED"));
                disconnect();
            } catch (IOException e) {
                MainInterface.log("IOException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_CONNECTED"));
                disconnect();
            } catch (XMPPException e) {
                MainInterface.log("XMPPException while connecting in Listener " + e.getMessage());
                getApplicationContext().sendBroadcast(new Intent("LISTENER_CONNECTED"));
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

	    private void returnMessage(Message message) {
		    message.setBody("This message should probably not have landed in the NOTIFIER_RECEIVER resource, am I right? " + message.getBody());
		    message.setTo(message.getFrom());
		    message.setFrom(prefs.getAppUser().getJID() + "/NOTIFIER_RECEIVER");
	    }
    };

	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

	    try {
		    prefs = new Preferences();
	    } catch (Preferences.NotInitializedException e) {
		    MainInterface.log("FATAL");
		    e.printStackTrace();
	    }

	    if(!xmppWorkerThread.getStatus().equals(AsyncTask.Status.RUNNING)) {
		    xmppWorkerThread.execute(MainInterface.connectionConfiguration);
	    } else {
		    disconnect();
		    xmppWorkerThread.execute(MainInterface.connectionConfiguration);
	    }

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
	            if(conn != null) {
		            try {
			            conn.disconnect();
		            } catch (SmackException.NotConnectedException e) {
			            MainInterface.log("NotConnectedException in Listener --> onCancelled()" + e.getMessage());
		            }
		            setRunning(conn.isConnected());
		            for(TargetUser current : prefs.getUsers()) {
			            current.updatePresence(null);
		            }
	            }
            }
        })).start();
    }

	public static void setRunning(boolean running) {
		Listener.running = running;
	}

	public static boolean isRunning() {
		return Listener.running;
	}
}
