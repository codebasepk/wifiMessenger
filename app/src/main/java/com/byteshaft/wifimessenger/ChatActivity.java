package com.byteshaft.wifimessenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

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
                    String realMessage = String.format(
                            "{\"sender\": \"%s\", \"text\": \"%s\", \"time\": \"%s\"}",
                            contactName, message, String.valueOf(System.currentTimeMillis()));
                    MessagingHelpers.sendMessage("MSG:" + realMessage, ipAddress,
                            ServiceHelpers.BROADCAST_PORT);
                    editTextMessage.getText().clear();
                }
        }
    }
}
