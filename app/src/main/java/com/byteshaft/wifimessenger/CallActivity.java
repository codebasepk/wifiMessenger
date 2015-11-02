package com.byteshaft.wifimessenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.byteshaft.wifimessenger.utils.AudioCall;
import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CallActivity extends Activity implements SensorEventListener {

    TextView textViewContactName;
    ImageButton buttonCallAccept;
    ImageButton buttonCallReject;
    AudioCall audioCall;
    private Ringtone ringtone;
    private Vibrator vibrator;
    public static boolean IN_CALL;
    private static CallActivity sInstance;
    private static boolean callActivityVisible;

    AudioManager mAudioManager;

    public static CallActivity getInstance() {
        return sInstance;
    }

    public static boolean isRunning() {
        return sInstance != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_activity);
        callActivityVisible = true;
        sInstance = this;

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        textViewContactName = (TextView) findViewById(R.id.tv_contact_name_call);
        buttonCallAccept = (ImageButton) findViewById(R.id.button_call_accept);
        buttonCallReject = (ImageButton) findViewById(R.id.button_call_reject);
        Intent intent = getIntent();
        String callSate = intent.getStringExtra("CALL_STATE");
        final String ipAddress = intent.getStringExtra("IP_ADDRESS");
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            audioCall = AudioCall.getInstance(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (callSate.equals("OUTGOING")) {
            String contact = intent.getStringExtra("CONTACT_NAME");
            textViewContactName.setText("Calling: " + contact);
            buttonCallAccept.setVisibility(View.GONE);
        } else if (callSate.equals("INCOMING")) {
            String contact = intent.getStringExtra("CONTACT_NAME");
            textViewContactName.setText("Incoming: " + contact);
            buttonCallAccept.setVisibility(View.VISIBLE);
            vibrator.vibrate(500);
            ringtone.play();
        }

        buttonCallAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
                setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, 14);
                vibrator.cancel();
                audioCall.startCall();
                IN_CALL = true;
                MessagingHelpers.sendMessage("ACC:", ipAddress, ServiceHelpers.BROADCAST_PORT);
                buttonCallAccept.setVisibility(View.GONE);
            }
        });

        buttonCallReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
                if (IN_CALL) {
                    audioCall.endCall();
                    MessagingHelpers.sendMessage("END:", ipAddress, ServiceHelpers.BROADCAST_PORT);
                    IN_CALL = false;
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                } else {
                    MessagingHelpers.sendMessage("REJ:", ipAddress, ServiceHelpers.BROADCAST_PORT);
                }
                finish();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk >= Build.VERSION_CODES.LOLLIPOP) {
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor mProximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_UI);
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = manager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Your Tag");
            if (event.values[0] != mProximitySensor.getMaximumRange() && IN_CALL && callActivityVisible) {
                wl.acquire();
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = 0;
                getWindow().setAttributes(params);
                Log.e("onSensorChanged", "NEAR");
            } else {
                if (wl.isHeld()) {
                    wl.release();
                }
                Log.e("onSensorChanged", "FAR");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        callActivityVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        callActivityVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        callActivityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        callActivityVisible = false;
    }
}
