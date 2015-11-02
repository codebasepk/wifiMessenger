package com.byteshaft.wifimessenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.wifimessenger.services.LongRunningService;
import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.MessagingHelpers;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.net.InetAddress;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener, ListView.OnItemClickListener {

    LinearLayout layoutUsername;
    RelativeLayout layoutMain;
    LinearLayout layoutMainTwo;
    EditText editTextUsername;
    TextView showUsername;
    ListView peerList;
    Switch serviceSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layoutUsername = (LinearLayout) findViewById(R.id.layout_username);
        layoutMain = (RelativeLayout) findViewById(R.id.layout_main);
        layoutMainTwo = (LinearLayout) findViewById(R.id.layout_main_two);
        showUsername = (TextView) findViewById(R.id.tv_username);
        serviceSwitch = (Switch) findViewById(R.id.switch_service);
        serviceSwitch.setOnCheckedChangeListener(this);
        peerList = (ListView) findViewById(R.id.lv_peer_list);
        peerList.setOnItemClickListener(this);

        if (AppGlobals.isVirgin()) {
            System.out.println("First time");
            layoutMain.setVisibility(View.GONE);
            layoutUsername.setVisibility(View.VISIBLE);
            editTextUsername = (EditText) findViewById(R.id.editTextDisplayName);
            Button startButton = (Button) findViewById(R.id.buttonStart);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("wifiMessenger", "Start button pressed");
                    String username = editTextUsername.getText().toString();
                    if (username.trim().length() < 1) {
                        Toast.makeText(getApplicationContext(), "Invalid Username", Toast.LENGTH_SHORT).show();
                    } else {
                        AppGlobals.putName(username);
                        notVirgin();
                    }
                }
            });

            editTextUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideKeyboard(v);
                    }
                }
            });
        } else {
            notVirgin();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_service:
                if (isChecked) {
                    startService(new Intent(getApplicationContext(), LongRunningService.class));
                    layoutMainTwo.setVisibility(View.VISIBLE);
                    AppGlobals.setService(true);
                    showUsername.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    stopService(new Intent(getApplicationContext(), LongRunningService.class));
                    layoutMainTwo.setVisibility(View.GONE);
                    AppGlobals.setService(false);
                    showUsername.setTextColor(Color.parseColor("#F44336"));
                }
                invalidateOptionsMenu();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (!ServiceHelpers.isPeerListEmpty()) {
            System.out.println(String.format("Item: %d clicked", position));
            HashMap<String, InetAddress> peers = ServiceHelpers.getPeersList();
            String name = parent.getItemAtPosition(position).toString();
            String ipAddress = peers.get(name).getHostAddress();
            showActionsDialog(name, ipAddress, peers);
        }
    }

    private void notVirgin() {
        layoutMain.setVisibility(View.VISIBLE);
        layoutUsername.setVisibility(View.GONE);
        AppGlobals.setVirgin(false);
        showUsername.setText("Username: " + AppGlobals.getName());
        serviceSwitch.setChecked(AppGlobals.isServiceOn());
        if (AppGlobals.isServiceOn()) {
            layoutMainTwo.setVisibility(View.VISIBLE);
            ServiceHelpers.startPeerDiscovery(MainActivity.this, peerList);
            showUsername.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            layoutMainTwo.setVisibility(View.GONE);
            showUsername.setTextColor(Color.parseColor("#F44336"));
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (AppGlobals.isServiceOn()) {
            menu.findItem(R.id.action_refresh).setVisible(true);
        } else {
            menu.findItem(R.id.action_refresh).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {


            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showActionsDialog(final String username, final String ipAddress,  final HashMap<String, InetAddress> peers) {

        AlertDialog.Builder actionDialog = new AlertDialog.Builder(this);
        actionDialog.setTitle("Choose Action")
                .setMessage("Selected User: " + username)
                .setPositiveButton("Text", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra("CONTACT_NAME", username);
                        startActivity(intent);

                        MessagingHelpers.sendMessage("MSG:Hello", ipAddress,
                                ServiceHelpers.BROADCAST_PORT);
                    }
                })

                .setNegativeButton("Call", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(MainActivity.this, CallActivity.class);
                        intent.putExtra("CONTACT_NAME", username);
                        intent.putExtra("CALL_STATE", "OUTGOING");
                        intent.putExtra("IP_ADDRESS", ipAddress);
                        startActivity(intent);

                        MessagingHelpers.sendCallRequest(username, ipAddress, ServiceHelpers.BROADCAST_PORT);
                    }
                })
                .create().show();
    }
}