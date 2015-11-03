package com.byteshaft.wifimessenger.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.wifimessenger.R;
import com.byteshaft.wifimessenger.database.MessagesDatabase;
import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends Activity implements View.OnClickListener {

    private TextView textViewContactName;
    private ImageButton buttonSend;
    private String ipAddress;
    private EditText editTextMessage;
    private String contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        textViewContactName = (TextView) findViewById(R.id.tv_contact_name_chat);
        editTextMessage = (EditText) findViewById(R.id.et_chat);
        buttonSend = (ImageButton) findViewById(R.id.button_chat_send);
        buttonSend.setOnClickListener(this);

        Intent intent = getIntent();
        contactName = intent.getStringExtra("CONTACT_NAME");
        ipAddress = intent.getStringExtra("IP_ADDRESS");
        MessagesDatabase database = new MessagesDatabase(this);
        String tableName = intent.getStringExtra("user_table");
        ArrayList<HashMap> messages = database.getMessagesForContact(tableName);

        textViewContactName.setText(contactName);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_chat_send:
                String message = editTextMessage.getText().toString();
                if (message.trim().length() < 1) {
                    Toast.makeText(this, "Message field is empty", Toast.LENGTH_SHORT).show();
                } else {
                    String deviceId = AppGlobals.getDeviceId();
                    String currentTime = String.valueOf(System.currentTimeMillis());
                    String realMessage = String.format(
                            "{\"device_id\": %s,\"sender\": \"%s\", \"text\": \"%s\", \"time\": \"%s\"}",
                            deviceId, contactName, message, currentTime);
                    MessagingHelpers.sendMessage("MSG:" + realMessage, ipAddress,
                            ServiceHelpers.BROADCAST_PORT);
                    MessagesDatabase database = new MessagesDatabase(getApplicationContext());
                    database.addNewMessageToThread(contactName+""+deviceId, message, "0", currentTime);
                    editTextMessage.getText().clear();
                }
        }
    }
}
