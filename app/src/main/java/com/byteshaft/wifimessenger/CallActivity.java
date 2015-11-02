package com.byteshaft.wifimessenger;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.byteshaft.wifimessenger.utils.AudioCall;
import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CallActivity extends Activity {

    TextView textViewContactName;
    ImageButton buttonCallAccept;
    ImageButton buttonCallReject;
    Ringtone ringtone;
    AudioCall audioCall;
    public static boolean IN_CALL;
    private static CallActivity sInstance;

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
        sInstance = this;

        textViewContactName = (TextView) findViewById(R.id.tv_contact_name_call);
        buttonCallAccept = (ImageButton) findViewById(R.id.button_call_accept);
        buttonCallReject = (ImageButton) findViewById(R.id.button_call_reject);
        Intent intent = getIntent();
        String contact = intent.getStringExtra("CONTACT_NAME");
        String callSate = intent.getStringExtra("CALL_STATE");
        final String ipAddress = intent.getStringExtra("IP_ADDRESS");
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            audioCall = new AudioCall(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

        if (callSate.equals("OUTGOING")) {
            textViewContactName.setText("Calling: " + contact);
            buttonCallAccept.setVisibility(View.GONE);
        } else if (callSate.equals("INCOMING")) {
            textViewContactName.setText("Incoming: " + contact);
            buttonCallAccept.setVisibility(View.VISIBLE);
            ringtone.play();

        }

        buttonCallAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
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
                } else {
                    MessagingHelpers.sendMessage("REJ:", ipAddress, ServiceHelpers.BROADCAST_PORT);
                }
                finish();
            }
        });
    }
}
