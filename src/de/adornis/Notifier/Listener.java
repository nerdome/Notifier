package de.adornis.Notifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
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

	public final static int CONNECTED = 0;
	public final static int CONNECTING = 1;
	public final static int DISCONNECTED = 2;
	public final static int DISCONNECTING = 3;

	private XMPPTCPConnection conn = null;

	private Preferences prefs;

	private ListenerThread listener;

	private BroadcastReceiver credentialsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			reconnect();
		}
	};

	private BroadcastReceiver userEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fetchInitialOnlineStates(intent.getStringExtra("JID") == null ? "" : intent.getStringExtra("JID"));
		}
	};

	public void fetchInitialOnlineStates(String JID) {
		for (RosterEntry current : conn.getRoster().getEntries()) {
			try {
				if (JID.equals("") || current.getUser().equals(JID)) {
					prefs.getUser(current.getUser()).updatePresence(conn.getRoster().getPresence(current.getUser()));
				}
			} catch (UserNotFoundException e) {
				getApplicationContext().sendBroadcast(new Intent(Notifier.USER_PROPOSE_LIST).putExtra("JID", current.getUser()));
			}
		}

		for (TargetUser current : prefs.getUsers()) {
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
		registerReceiver(credentialsReceiver, new IntentFilter(Notifier.CREDENTIALS));
		registerReceiver(userEventReceiver, new IntentFilter(Notifier.USER_EVENT));

		try {
			prefs = new Preferences();
		} catch (UserNotFoundException e) {
			stopSelf();
		}
		prefs.setConnected(DISCONNECTED);

		attemptConnect();

		flags = START_STICKY;
		Notification.Builder nb = new Notification.Builder(getApplicationContext());
		nb.setDefaults(Notification.DEFAULT_VIBRATE);
		nb.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainInterface.class), 0));
		startForeground(1234, nb.build());
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		disconnect();
		super.onDestroy();
	}

	public void attemptConnect() {
		MainInterface.log(">> connecting");
		prefs.setConnected(CONNECTING);
		if (listener != null) {
			if (listener.getStatus().equals(AsyncTask.Status.FINISHED)) {
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

	public void reconnect() {
		disconnect(true);
	}

	public void disconnect() {
		disconnect(false);
	}

	private void disconnect(final boolean restart) {
		MainInterface.log("<< disconnecting");
		(new Thread(new Runnable() {
			@Override
			public void run() {
				prefs.setConnected(DISCONNECTING);
				if (conn != null) {
					try {
						conn.disconnect();
					} catch (SmackException.NotConnectedException e) {
						MainInterface.log("already disconnected or not yet connected");
					}
					for (TargetUser current : prefs.getUsers()) {
						current.updatePresence(null);
					}
					if (listener.getStatus().equals(AsyncTask.Status.FINISHED) && !conn.isConnected()) {
						// when the app user isn't authorized, this isn't reached and he's blocked from turning on the listener again
						prefs.setConnected(DISCONNECTED);
					}
					if (restart) {
						attemptConnect();
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
				config.tickInActivity((int) millisUntilFinished / 1000);
			}

			@Override
			public void onFinish() {
				try {
					conn.sendPacket(config.getMessage(prefs));
				} catch (SmackException.NotConnectedException e) {
					MainInterface.log("FATAL connection error while sending message");
				}
				config.tickInActivity(0);
			}
		}.start();
	}

	public void addToRoster(String JID) {
		try {
			conn.getRoster().createEntry(JID, JID.substring(0, JID.indexOf("@")), null);
		} catch (Exception ignored) {
			// whatever...
		}
	}

	public void invite(String JID) {
		Message msg = new Message();
		msg.setTo(JID);
		msg.setBody(prefs.getAppUser().getJID() + " invites you to join the Notifier force! Go here for more information: www.example.com");
		try {
			conn.sendPacket(msg);
		} catch (SmackException.NotConnectedException e) {
			MainInterface.log("error while sending invitation in Listener");
			e.printStackTrace();
		}
	}

	public class ListenerBinder extends Binder {
		public Service getService() {
			return Listener.this;
		}
	}

	private class ListenerThread extends AsyncTask<Void, String, Void> {

		public void connect() throws IOException, XMPPException, SmackException {
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
					} catch (UserNotFoundException ignore) {
						// user wasn't found, ignoring
					}
				}
			});

			prefs.setConnected(CONNECTING);

			conn.connect();
			conn.login(prefs.getAppUser().getUsername(), prefs.getAppUser().getPassword(), "NOTIFIER_RECEIVER");
			conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
			conn.sendPacket(new Presence(Presence.Type.available, "awaiting notifier notifications", 0, Presence.Mode.xa));

			if (conn.isConnected()) {
				prefs.setConnected(CONNECTED);
			}

			fetchInitialOnlineStates("");

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
								} else if (props.containsKey("PING")) {
									if (props.get("PING").equals("request")) {
										Message msg = new Message();
										msg.setTo(message.getFrom());
										JivePropertiesManager.addProperty(msg, "PING", "reply");
										try {
											conn.sendPacket(msg);
										} catch (SmackException.NotConnectedException e) {
											MainInterface.log("Couldn't ping back because there was a connection issue");
											e.printStackTrace();
										}
									} else if (props.get("PING").equals("reply")) {
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
		}

		@Override
		protected Void doInBackground(Void... params) {
			conn = new XMPPTCPConnection(MainInterface.getConfig(prefs.getAppUser().getDomain(), 5222));
			try {
				connect();
			} catch (SmackException.NoResponseException e) {
				MainInterface.log("NoResponseException while connecting in Listener " + e.getMessage());
				// probably got a timeout, try with bigger timeout
				MainInterface.log("Retrying with package timeout of " + conn.getPacketReplyTimeout() * 2 + " instead of " + conn.getPacketReplyTimeout());
				conn.setPacketReplyTimeout(conn.getPacketReplyTimeout() * 2);
				try {
					connect();
				} catch (SmackException.NoResponseException e1) {
					MainInterface.log("NoResponseException while connecting in Listener " + e.getMessage());
					// probably got a timeout, try with bigger timeout
					MainInterface.log("Retrying with package timeout of " + conn.getPacketReplyTimeout() * 2 + " instead of " + conn.getPacketReplyTimeout());
					conn.setPacketReplyTimeout(conn.getPacketReplyTimeout() * 2);
					try {
						connect();
					} catch (SmackException.NoResponseException e2) {
						MainInterface.log("Couldn't log in with longer timeout either, giving up.");
						e2.printStackTrace();
					} catch (Exception ignore) {
						MainInterface.log("Some other error happened now o.0");
					}
					getApplicationContext().sendBroadcast(new Intent("LISTENER_CONNECTED"));
					disconnect();
				} catch (Exception ignore) {
					MainInterface.log("Some other error happened now o.0");
				}
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
			} catch (SmackException.NotConnectedException e) {
				MainInterface.log("NotConnectedException while connecting in Listener " + e.getMessage());
				getApplicationContext().sendBroadcast(new Intent("LISTENER_CONNECTED"));
				disconnect();
			} catch (SmackException e) {
				MainInterface.log("General SmackException while connecting in Listener " + e.getMessage());
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
			Message out = new Message(message.getFrom());
			out.setBody("This message should probably not have landed in the NOTIFIER_RECEIVER resource, am I right? \"" + message.getBody() + " \"");
			MainInterface.log("returning message to " + out.getTo());
			if (!out.getTo().startsWith(prefs.getAppUser().getJID())) {
				try {
					conn.sendPacket(out);
				} catch (SmackException.NotConnectedException e) {
					MainInterface.log("couldn't send message back in returnMessage() in Listener");
					e.printStackTrace();
				}
			} else {
				MainInterface.log("Not sending back to myself");
			}
		}
	}
}
