package de.adornis.Notifier;

import android.app.Notification;
import android.app.Service;
import android.content.*;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.widget.Switch;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class Listener extends Service {

	public final static int STARTED = 0;
	public final static int STARTING = 1;
	public final static int STOPPED = 2;
	public final static int STOPPING = 3;

    private XMPPTCPConnection conn = null;

	private Preferences prefs;

	private ListenerThread listener;

	public void fetchInitialOnlineStates() {
		for(RosterEntry current : conn.getRoster().getEntries()) {
			try {
				prefs.getUser(current.getUser()).updatePresence(conn.getRoster().getPresence(current.getUser()));
			} catch (UserNotFoundException e) {
				// TODO get it to ask here whether to add the user to the list
			}
		}

		for(TargetUser current : prefs.getUsers()) {
			Message msg = new Message();
			msg.setTo(current.getJID() + "/NOTIFIER_RECEIVER");
			JivePropertiesManager.addProperty(msg, "PING", "request");
			try {
				conn.sendPacket(msg);
			} catch (SmackException.NotConnectedException e) {
				attemptConnect();
			}
		}
	}

	@Override
    public IBinder onBind(Intent intent) {
        return new ListenerBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

	    try {
		    prefs = new Preferences();
	    } catch (Preferences.NotInitializedException e) {
		    MainInterface.log("FATAL");
		    e.printStackTrace();
	    }

	    prefs.setListenerRunning(STARTING);

	    attemptConnect();

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

	private void attemptConnect() {
		prefs.setListenerRunning(STARTING);
		if(listener != null) {
			if(listener.getStatus().equals(AsyncTask.Status.FINISHED)) {
				listener = new ListenerThread();
				listener.execute();
			} else {
				disconnect();
			}
		} else {
			listener = new ListenerThread();
			listener.execute();
		}
	}

    private void disconnect() {
	    prefs.setListenerRunning(STOPPING);
        (new Thread(new Runnable() {
            @Override
            public void run() {
	            if(conn != null) {
		            try {
			            conn.disconnect();
		            } catch (SmackException.NotConnectedException e) {
			            MainInterface.log("already disconnected or not yet connected");
		            }
		            for(TargetUser current : prefs.getUsers()) {
			            current.updatePresence(null);
		            }
		            if(listener.getStatus().equals(AsyncTask.Status.FINISHED) && !conn.isConnected()) {
			            prefs.setListenerRunning(STOPPED);
		            }
	            }
            }
        })).start();
    }

	public Roster getRoster() {
		return conn.getRoster();
	}

	public void processMessage(final MessageConfiguration config) {
		new CountDownTimer(config.getDelay(), 1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				try {
					conn.sendPacket(config.getMessage(prefs));
				} catch (SmackException.NotConnectedException e) {
					MainInterface.log("FATAL connection error while sending message");
				}
			}
		}.start();
	}


	public class ListenerBinder extends Binder {
		public Service getService() {
			return Listener.this;
		}
	}

	private class ListenerThread extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			MainInterface.log("im here");

			conn = new XMPPTCPConnection(MainInterface.getConfig(prefs.getAppUser().getDomain(), 5222));
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
				conn.login(prefs.getAppUser().getUsername(), prefs.getAppUser().getPassword(), "NOTIFIER_RECEIVER");
				conn.sendPacket(new Presence(Presence.Type.available, "awaiting notifier notifications", 0, Presence.Mode.xa));

				if(conn.isConnected()) {
					prefs.setListenerRunning(STARTED);
				}

				fetchInitialOnlineStates();

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
											msg.setTo(message.getFrom());
											JivePropertiesManager.addProperty(msg, "PING", "reply");
											try {
												conn.sendPacket(msg);
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
	}
}
