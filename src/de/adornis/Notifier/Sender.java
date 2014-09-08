package de.adornis.Notifier;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import java.io.IOException;

public class Sender extends IntentService {

	public final static int DEFAULT = 0;
	public final static int TIMED = 1;

    private XMPPTCPConnection conn = null;
	public static Roster roster = null;

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
	                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	                String user = prefs.getString("user", "yorrd@adornis.de");
	                String password = prefs.getString("password", "123vorbei");

                    conn.connect();
                    conn.login(user.substring(0, user.indexOf('@')), password, "NOTIFIER_SENDER");
	                conn.sendPacket(new Presence(Presence.Type.available, "sending notifier notifications", 0, Presence.Mode.away));

					roster = conn.getRoster();
	                for(RosterEntry current : roster.getEntries()) {
		                Intent i = new Intent("ROSTER");
						if(conn.getRoster().getPresence(current.getUser()).getType() == Presence.Type.available) {
							i.putExtra("ONLINE", current.getUser());
						} else if(conn.getRoster().getPresence(current.getUser()).getType() == Presence.Type.unavailable) {
							i.putExtra("OFFLINE", current.getUser());
						} else if(conn.getRoster().getPresence(current.getUser()).getType() == Presence.Type.unsubscribed) {
			                i.putExtra("NOT_IN_ROSTER", current.getUser());
		                }
		                sendBroadcast(i);
	                }
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
        String receiver = intent.getStringExtra("RECEIVER");
        msg = new Message();
        msg.setTo(receiver + "/NOTIFIER_RECEIVER");
        msg.setBody("YOU SHOULDN'T SEE THIS :: this is an alarm notification :: NOTIFIER App for Android");
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
					    MainInterface.log("im here");
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
