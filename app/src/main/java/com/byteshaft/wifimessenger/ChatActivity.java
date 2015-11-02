package com.byteshaft.wifimessenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ChatActivity extends Activity {

    TextView textViewContactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        textViewContactName = (TextView) findViewById(R.id.tv_contact_name_chat);

        Intent intent = getIntent();
        String contact = intent.getStringExtra("CONTACT_NAME");

        textViewContactName.setText(contact);
    }
}
