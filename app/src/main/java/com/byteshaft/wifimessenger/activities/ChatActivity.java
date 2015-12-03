package com.byteshaft.wifimessenger.activities;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.wifimessenger.R;
import com.byteshaft.wifimessenger.database.MessagesDatabase;
import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewContactName;
    private ImageButton buttonSend;
    private String ipAddress;
    private EditText editTextMessage;
    private String contactName;

    private String mContextUserTable;
    private ArrayList<HashMap> messages = new ArrayList<>();

    public ChatArrayAdapter adapter;

    public boolean isChatVisibleForContact(String name) {
        return contactName != null && name.equals(contactName);
    }

    private static ChatActivity sInstance;

    public static ChatActivity getInstance() {
        return sInstance;
    }

    public static boolean isRunning() {
        return sInstance != null;
    }

    public void updateAdapter(HashMap<String, String> map) {
        adapter.add(map);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        sInstance = this;
        editTextMessage = (EditText) findViewById(R.id.et_chat);
        buttonSend = (ImageButton) findViewById(R.id.button_chat_send);
        buttonSend.setOnClickListener(this);
        ListView bubbleList = (ListView) findViewById(R.id.lv_chat);

        Intent intent = getIntent();
        contactName = intent.getStringExtra("CONTACT_NAME");
        ipAddress = intent.getStringExtra("IP_ADDRESS");
        MessagesDatabase database = new MessagesDatabase(this);
        mContextUserTable = intent.getStringExtra("user_table");

        setTitle(contactName);

        try {
            messages = database.getMessagesForContact(mContextUserTable);
        } catch (SQLiteException e) {
            e.printStackTrace();
            // Apparently no table exists.
        }

        adapter = new ChatArrayAdapter(
                this, R.layout.activity_chat_singlemessage, messages);
        bubbleList.setAdapter(adapter);
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
                            "{\"device_id\": \"%s\",\"sender\": \"%s\", \"text\": \"%s\", \"time\": \"%s\"}",
                            deviceId, AppGlobals.getName(), message, currentTime);
                    MessagingHelpers.sendMessage("MSG:" + realMessage, ipAddress,
                            ServiceHelpers.BROADCAST_PORT);
                    MessagesDatabase database = new MessagesDatabase(getApplicationContext());
                    database.addNewMessageToThread(mContextUserTable, message, "0", currentTime);
                    HashMap<String, String> mapTemp = new HashMap<>();
                    mapTemp.put("direction", "0");
                    mapTemp.put("body", message);
                    adapter.add(mapTemp);
                    editTextMessage.getText().clear();
                }
        }
    }

    private class ChatArrayAdapter extends ArrayAdapter {

        public ChatArrayAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.activity_chat_singlemessage, parent, false);
                holder = new ViewHolder();
                holder.layout = (LinearLayout) convertView.findViewById(R.id.singleMessageContainer);
                holder.title = (TextView) holder.layout.findViewById(R.id.singleMessage);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (!messages.isEmpty()) {
                if (messages.get(position).get("direction").equals("0")) {
                    holder.layout.setGravity(Gravity.RIGHT);
                    holder.title.setBackgroundResource(R.drawable.bubble_b);
                } else {
                    holder.layout.setGravity(Gravity.LEFT);
                    holder.title.setBackgroundResource(R.drawable.bubble_a);
                }
                holder.title.setText((String) messages.get(position).get("body"));
            }
            return convertView;
        }
    }

    static class ViewHolder {
        public TextView title;
        public LinearLayout layout;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id  == R.id.action_call) {
            Intent intent = new Intent(ChatActivity.this, CallActivity.class);
                        intent.putExtra("CONTACT_NAME", contactName);
                        intent.putExtra("CALL_STATE", "OUTGOING");
                        intent.putExtra("IP_ADDRESS", ipAddress);
                        startActivity(intent);
                        MessagingHelpers.sendCallRequest(contactName, ipAddress, ServiceHelpers.BROADCAST_PORT);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_call, menu);
        return true;
    }
}
