package de.adornis.Notifier;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Random;

public class NoiseMakerActivity extends Activity {

    AsyncTask<Integer, Void, Void> noiseMaker = new AsyncTask<Integer, Void, Void>() {

        @Override
        protected Void doInBackground(Integer... duration) {

            Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
            KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock kl = km.newKeyguardLock("ALARM_CLOCK");

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

            vib.vibrate(duration[0] * 1000);

            Random rnd = new Random();
            for(int i = 0; i < duration[0] * 1000; i += 100) {
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

            NoiseMakerActivity.this.finish();

            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noise_maker);

        noiseMaker.execute(getIntent().getIntExtra("DURATION", 3));

        ((TextView) findViewById(R.id.message)).setText(getIntent().getStringExtra("MESSAGE"));

        MainInterface.log("NOISE NOISE NOISE");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // open main activity
        Intent uiIntent = new Intent(this, MainInterface.class);
        uiIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        uiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(uiIntent);
    }
}