package de.adornis.Notifier;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
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

    XMPPTCPConnection conn = null;

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
                    conn.connect();
                    conn.login(MainInterface.USER.substring(0, MainInterface.USER.indexOf('@')), MainInterface.PASSWORD, "NOTIFIER_SENDER");
	                conn.sendPacket(new Presence(Presence.Type.available, "sending notifier notifications", 0, Presence.Mode.chat));

	                for(RosterEntry current : conn.getRoster().getEntries()) {
		                Intent i = new Intent("ROSTER");
						if(conn.getRoster().getPresence(current.getUser()).getType() == Presence.Type.available) {
							i.putExtra("ONLINE", current.getUser());
						} else {
							i.putExtra("OFFLINE", current.getUser());
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
        Message msg = new Message();
        msg.setTo(receiver + "/NOTIFIER_RECEIVER");
        msg.setBody("YOU SHOULDN'T SEE THIS :: this is an alarm notification :: NOTIFIER App for Android");
        JivePropertiesManager.addProperty(msg, "ALARM", intent.getStringExtra("MESSAGE"));

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
