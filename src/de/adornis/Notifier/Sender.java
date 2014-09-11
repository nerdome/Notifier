package de.adornis.Notifier;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import java.io.IOException;
import java.util.Collection;

public class Sender extends IntentService {

	public final static int DEFAULT = 0;
	public final static int TIMED = 1;

    private XMPPTCPConnection conn = null;
	public static Roster roster = null;
	private Preferences prefs;

	private Message msg;

    public Sender() {
        super("XMPP Wakeup Call Sender");
    }

    @Override
    public IBinder onBind(Intent intent) {
        connect();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }

    private void connect() {

        (new Thread(new Runnable() {
            @Override
            public void run() {
                conn = new XMPPTCPConnection(MainInterface.connectionConfiguration);
                try {
	                String user = prefs.getAppUser().getJID();
	                String password = prefs.getAppUser().getPassword();

                    conn.connect();
                    conn.login(user.substring(0, user.indexOf('@')), password, "NOTIFIER_SENDER");
	                conn.sendPacket(new Presence(Presence.Type.available, "sending notifier notifications", 0, Presence.Mode.away));

	                roster = conn.getRoster();
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
			                Intent i = new Intent(MainInterface.ROSTER);
			                String user = presence.getFrom().substring(0, presence.getFrom().indexOf("/"));
			                MainInterface.log(user);
							if(presence.getType() == Presence.Type.available) {
								if(presence.getFrom().endsWith("NOTIFIER_RECEIVER")) {
									i.putExtra("user", user);
									i.putExtra("status", TargetUser.ONLINE);
								} else {
									i.putExtra("user", user);
									i.putExtra("status", TargetUser.HALF_ONLINE);
								}
							} else if(presence.getType() == Presence.Type.unavailable) {
								i.putExtra("user", user);
								i.putExtra("status", TargetUser.OFFLINE);
							}
			                sendBroadcast(i);
		                }
	                });
                } catch (SmackException e) {
                    MainInterface.log("SmackException in Sender --> connect() " + e.getMessage());
                } catch (IOException e) {
                    MainInterface.log("IOException in Sender --> connect() " + e.getMessage());
                } catch (XMPPException e) {
                    MainInterface.log("XMPPException in Sender --> connect() " + e.getMessage());
                }
            }
        })).start();

    }

    private void disconnect() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    conn.disconnect();
                } catch (SmackException.NotConnectedException e) {
                    MainInterface.log("NotConnectedException in Sender --> disconnect() " + e.getMessage());
                }
            }
        })).start();

    }

    @Override
    protected void onHandleIntent(Intent intent) {

		if(prefs == null) {
			try {
				prefs = new Preferences();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

        String receiver = intent.getStringExtra("RECEIVER");
        msg = new Message();
        msg.setTo(receiver + "/NOTIFIER_RECEIVER");
        msg.setBody(prefs.getAppUser().getJID() + " sent you this message via Notifier: " + intent.getStringExtra("MESSAGE") + "<br>This is an alarm notification :: NOTIFIER App for Android");
        JivePropertiesManager.addProperty(msg, "ALARM", intent.getStringExtra("MESSAGE"));

	    switch(intent.getIntExtra("TYPE", DEFAULT)) {
	        case DEFAULT:
		        sendMessage();
		        break;
		    case TIMED:
			    final long duration = intent.getIntExtra("timer_duration", 10) * 1000;
			    new CountDownTimer(duration, 1000) {
				    @Override
				    public void onTick(long millisUntilFinished) {

				    }

				    @Override
				    public void onFinish() {
						sendMessage();
				    }
			    }.start();
			    break;
		    default:
			    MainInterface.log("issue identifying the type of the message to be sent in Sender service");
			    break;
	    }
    }

	private void sendMessage() {
		try {
			conn.sendPacket(msg);
		} catch (SmackException.NotConnectedException e) {
			// try to reestablish the connection
			MainInterface.log("was logged out, logging back in! " + e.getMessage());
			connect();
			try {
				conn.sendPacket(msg);
			} catch (SmackException.NotConnectedException e1) {
				MainInterface.log(e1.getMessage());
				// really can't start for some reason
			}
		}
	}
}
