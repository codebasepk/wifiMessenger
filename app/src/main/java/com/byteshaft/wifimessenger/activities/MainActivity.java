package com.byteshaft.wifimessenger.activities;

import android.app.Activity;
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

import com.byteshaft.wifimessenger.R;
import com.byteshaft.wifimessenger.services.LongRunningService;
import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener, ListView.OnItemClickListener, View.OnClickListener,
        EditText.OnFocusChangeListener {

    private LinearLayout layoutUsername;
    private RelativeLayout layoutMain;
    private LinearLayout layoutMainTwo;
    private EditText editTextUsername;
    private TextView showUsername;
    private ListView peerList;
    public MenuItem menuItemService;
    public MenuItem menuItemRefresh;

    private static MainActivity sInstance;

    public static boolean isRunning() {
        return sInstance != null;
    }

    public static MainActivity getInstance() {
        return sInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sInstance = this;
//        startActivity(new Intent(this, MessagesListActivity.class));
        layoutUsername = (LinearLayout) findViewById(R.id.layout_username);
        layoutMain = (RelativeLayout) findViewById(R.id.layout_main);
        layoutMainTwo = (LinearLayout) findViewById(R.id.layout_main_two);
        showUsername = (TextView) findViewById(R.id.tv_username);
//        serviceSwitch = (Switch) findViewById(R.id.switch_service);
//        serviceSwitch.setOnCheckedChangeListener(this);
        peerList = (ListView) findViewById(R.id.lv_peer_list);
        peerList.setOnItemClickListener(this);

        if (AppGlobals.isVirgin()) {
            layoutMain.setVisibility(View.GONE);
            layoutUsername.setVisibility(View.VISIBLE);
            editTextUsername = (EditText) findViewById(R.id.editTextDisplayName);
            editTextUsername.setOnFocusChangeListener(this);
            Button startButton = (Button) findViewById(R.id.buttonStart);
            startButton.setOnClickListener(this);
        } else {
            notVirgin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ServiceHelpers.stopDiscover();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppGlobals.isVirgin() && AppGlobals.isServiceOn() && !ServiceHelpers.DISCOVER &&
                LongRunningService.isRunning()) {

            ServiceHelpers.discover(MainActivity.this, peerList);
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
                    ServiceHelpers.discover(MainActivity.this, peerList);
                } else {
                    stopService(new Intent(getApplicationContext(), LongRunningService.class));
                    layoutMainTwo.setVisibility(View.GONE);
                    AppGlobals.setService(false);
                    showUsername.setTextColor(Color.parseColor("#F44336"));
                    ServiceHelpers.stopDiscover();
                }
                invalidateOptionsMenu();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (!ServiceHelpers.isPeerListEmpty()) {
            ArrayList<HashMap> peers = ServiceHelpers.getPeersList();
            String name = parent.getItemAtPosition(position).toString();
            String ipAddress = (String) peers.get(position).get("ip");
            String userTable = (String) peers.get(position).get("user_table");
            showActionsDialog(name, ipAddress, userTable);
        }
    }

    private void notVirgin() {
        layoutMain.setVisibility(View.VISIBLE);
        layoutUsername.setVisibility(View.GONE);
        AppGlobals.setVirgin(false);
        showUsername.setText("Username: " + AppGlobals.getName());
        if (AppGlobals.isServiceOn()) {
            layoutMainTwo.setVisibility(View.VISIBLE);
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
        menuItemRefresh = menu.findItem(R.id.action_refresh);
        menuItemService = menu.findItem(R.id.enable_service);
        if (AppGlobals.isServiceOn()) {
            menu.findItem(R.id.action_refresh).setTitle("Refresh");
            menu.findItem(R.id.enable_service).setTitle("Disable Service");
        } else {
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.enable_service).setTitle("Enable Service");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            peerList.setAdapter(null);
            if (!ServiceHelpers.DISCOVER) {
                ServiceHelpers.discover(MainActivity.this, peerList);
            }
            return true;
        } if (id == R.id.enable_service) {
            if (!AppGlobals.isServiceOn()) {
                System.out.println("ok");
                startService(new Intent(getApplicationContext(), LongRunningService.class));
                layoutMainTwo.setVisibility(View.VISIBLE);
                AppGlobals.setService(true);
                item.setTitle("Disable Service");
                menuItemRefresh.setVisible(true);
                showUsername.setTextColor(Color.parseColor("#4CAF50"));
                ServiceHelpers.discover(MainActivity.this, peerList);
                return true;
            } else {
                System.out.println("that");
                stopService(new Intent(getApplicationContext(), LongRunningService.class));
                layoutMainTwo.setVisibility(View.GONE);
                AppGlobals.setService(false);
                menuItemRefresh.setVisible(false);
                item.setTitle("Enable Service");
                showUsername.setTextColor(Color.parseColor("#F44336"));
                ServiceHelpers.stopDiscover();
            }
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    public void showActionsDialog(final String username, final String ipAddress, final String userTable) {

        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("CONTACT_NAME", username);
        intent.putExtra("IP_ADDRESS", ipAddress);
        intent.putExtra("user_table", userTable);
        startActivity(intent);
//
//        AlertDialog.Builder actionDialog = new AlertDialog.Builder(this);
//        actionDialog.setTitle("Choose Action")
//                .setMessage("Selected User: " + username)
//                .setPositiveButton("Text", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
////                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
////                        intent.putExtra("CONTACT_NAME", username);
////                        intent.putExtra("IP_ADDRESS", ipAddress);
////                        intent.putExtra("user_table", userTable);
////                        startActivity(intent);
//                    }
//                })
//
//                .setNegativeButton("Call", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                        Intent intent = new Intent(MainActivity.this, CallActivity.class);
//                        intent.putExtra("CONTACT_NAME", username);
//                        intent.putExtra("CALL_STATE", "OUTGOING");
//                        intent.putExtra("IP_ADDRESS", ipAddress);
//                        startActivity(intent);
//
//                        MessagingHelpers.sendCallRequest(username, ipAddress, ServiceHelpers.BROADCAST_PORT);
//                    }
//                })
//                .create().show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonStart:
                Log.i("wifiMessenger", "Start button pressed");
                String username = editTextUsername.getText().toString();
                if (username.trim().length() < 1) {
                    Toast.makeText(getApplicationContext(), "Invalid Username", Toast.LENGTH_SHORT).show();
                } else {
                    AppGlobals.putName(username);
                    notVirgin();
                }
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.editTextDisplayName:
                if (!hasFocus) {
                    hideKeyboard(v);
                }
                break;
        }
    }
}