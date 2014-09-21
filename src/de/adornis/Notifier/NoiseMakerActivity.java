package de.adornis.Notifier;

import android.app.ActionBar;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.SoundPool;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Random;

public class NoiseMakerActivity extends Activity implements SoundPool.OnLoadCompleteListener {

	private int dur = 0;
	private int soundDuration = 1;

    AsyncTask<Integer, Void, Void> noiseMaker = new AsyncTask<Integer, Void, Void>() {

        @Override
        protected Void doInBackground(Integer... duration) {

	        dur = duration[0];

            Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
	        @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
            KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
	        @SuppressWarnings("deprecation")
            KeyguardManager.KeyguardLock kl = km.newKeyguardLock("ALARM_CLOCK");
	        Camera cam = Camera.open();
	        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
		        Camera.Parameters p = cam.getParameters();
		        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		        cam.setParameters(p);
	        }
	        SoundPool sp = new SoundPool(1, AudioManager.STREAM_ALARM, 100);
	        sp.setOnLoadCompleteListener(NoiseMakerActivity.this);
	        sp.load(getApplicationContext(), R.raw.toyphone_dialling, 1);
	        AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	        int oldVolume = mgr.getStreamVolume(AudioManager.STREAM_ALARM);
	        mgr.setStreamVolume(AudioManager.STREAM_ALARM, mgr.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

	        wl.acquire();
            kl.disableKeyguard();

            NoiseMakerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WindowManager.LayoutParams layout = getWindow().getAttributes();
                    layout.screenBrightness = 1F;
                    getWindow().setAttributes(layout);
                }
            });

            vib.vibrate(dur * 1000);

            Random rnd = new Random();
            for(int i = 0; i < dur * 1000; i += 100) {
                final int color = Color.argb(255, 160 + rnd.nextInt(96), 160 + rnd.nextInt(96), 160 + rnd.nextInt(96));
                NoiseMakerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.coloredBackground).setBackgroundColor(color);
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    MainInterface.log("Sleep interrupted in NoiseMaker --> onCreate()" + e.getMessage());
                }
            }

            wl.release();
            kl.reenableKeyguard();
	        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
		        cam.release();
	        }
	        mgr.setStreamVolume(AudioManager.STREAM_ALARM, oldVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

            NoiseMakerActivity.this.finish();

            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noise_maker);

	    // 4.4 and up
	    this.setImmersive(true);
	    // 4.1 and up
	    View decorView = getWindow().getDecorView();
	    // Hide the status bar.
	    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
	    decorView.setSystemUiVisibility(uiOptions);
	    // Remember that you should never show the action bar if the
	    // status bar is hidden, so hide that too if necessary.
	    ActionBar actionBar = getActionBar();
	    actionBar.hide();

	    MediaMetadataRetriever data = new MediaMetadataRetriever();
	    data.setDataSource(getApplicationContext(), Uri.parse("android.resource://de.adornis.Notifier/" + R.raw.toyphone_dialling));
	    String dur = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
	    soundDuration = Integer.parseInt(dur) / 1000;
	    data.release();

        noiseMaker.execute(getIntent().getIntExtra("DURATION", 3));

        ((TextView) findViewById(R.id.message)).setText(getIntent().getStringExtra("MESSAGE"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

	    Intent startIntent = null;
	    try {
		    String packg = (new Preferences()).getAppAfterNotified();
		    startIntent = getPackageManager().getLaunchIntentForPackage(packg);
	    } catch (UserNotFoundException e) {
		    MainInterface.log("Couldn't initiate preferences in NoiseMakerActivity onDestroy, setting package to open empty");
	    }

	    if(startIntent == null) {
		    MainInterface.log("Either the user hasn't entered one or the activity to be opened cannot be opened");
		    // open main activity
		    Intent uiIntent = new Intent(this, MainInterface.class);
		    uiIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		    uiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    this.startActivity(uiIntent);
	    } else {
		    startActivity(startIntent);
	    }
    }

	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		soundPool.play(sampleId, 1.0F, 1.0F, 1, dur / soundDuration - 1, 1);
	}
}