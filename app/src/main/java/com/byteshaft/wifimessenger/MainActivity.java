package com.byteshaft.wifimessenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.byteshaft.wifimessenger.services.LongRunningService;
import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

public class MainActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener, ListView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppGlobals.setVrigin(false);
        if (AppGlobals.isVirgin()) {

        } else {
            ListView peerList = (ListView) findViewById(R.id.lv_peer_list);
            ServiceHelpers.startPeerDiscovery(this, peerList);
        }

        Switch serviceSwitch = (Switch) findViewById(R.id.switch_service);
        serviceSwitch.setOnCheckedChangeListener(this);
//        String[] anArray = new String[] {"omer", "falak", "mullanh"};
//
//        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, anArray);
//
//        peerList.setAdapter(arrayAdapter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_service:
                if (isChecked) {
                    startService(new Intent(getApplicationContext(), LongRunningService.class));
                } else {
                    stopService(new Intent(getApplicationContext(), LongRunningService.class));
                }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
