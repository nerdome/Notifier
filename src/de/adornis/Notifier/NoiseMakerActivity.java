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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Random;

public class NoiseMakerActivity extends Activity implements SoundPool.OnLoadCompleteListener {

	private int dur = 0;
	AsyncTask<Integer, Void, Void> noiseMaker = new AsyncTask<Integer, Void, Void>() {

		Vibrator vib;
		PowerManager.WakeLock wl;
		KeyguardManager.KeyguardLock kl;
		Camera cam;
		Camera.Parameters cp;
		SoundPool sp;
		AudioManager am;
		int oldVolume;

		@Override
		protected Void doInBackground(Integer... duration) {

			dur = duration[0];

			vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
			PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
			KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
			kl = km.newKeyguardLock("ALARM_CLOCK");
			cam = Camera.open();
			if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
				cp = cam.getParameters();
				cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				cam.setParameters(cp);
			}
			sp = new SoundPool(1, AudioManager.STREAM_ALARM, 100);
			sp.setOnLoadCompleteListener(NoiseMakerActivity.this);
			sp.load(getApplicationContext(), R.raw.toyphone_dialling, 1);
			am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			oldVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
			am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

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

			Random rnd = new Random();
			for (int i = 0; i < dur * 1000 && !isCancelled(); i += 100) {
				vib.vibrate(100);
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
					e.printStackTrace();
				}
			}

			stop();

			return null;
		}

		private void stop() {
			wl.release();
			kl.reenableKeyguard();
			if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
				cam.release();
			}
			sp.release();
			sp.stop(AudioManager.STREAM_ALARM);
			am.setStreamVolume(AudioManager.STREAM_ALARM, oldVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

			NoiseMakerActivity.this.finish();
		}
	};
	private int soundDuration = 1;

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

		noiseMaker.cancel(false);

		Intent startIntent = null;
		try {
			String packg = (new Preferences()).getAppAfterWake();
			startIntent = getPackageManager().getLaunchIntentForPackage(packg);
		} catch (UserNotFoundException e) {
			MainInterface.log("Couldn't initiate preferences in NoiseMakerActivity onDestroy, setting package to open empty");
		}

		if (startIntent == null) {
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
		soundPool.play(sampleId, 1.0F, 1.0F, 1, -1, 1);
	}
}