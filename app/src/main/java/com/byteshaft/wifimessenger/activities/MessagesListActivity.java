package com.byteshaft.wifimessenger.activities;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.byteshaft.wifimessenger.R;
import com.byteshaft.wifimessenger.database.MessagesDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessagesListActivity extends ListActivity {

    private ArrayList<String> namesList = new ArrayList<>();
    private ArrayList<String> lastMessage = new ArrayList<>();
    private ArrayList<String> lastMessageTime = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        MessagesDatabase database = new MessagesDatabase(this);
        ArrayList<HashMap> tableMain = database.getAllTablesIndexData();
        for (HashMap map: tableMain) {
            namesList.add((String) map.get("table_name"));
            lastMessage.add((String) map.get("body"));
            lastMessageTime.add((String) map.get("time_stamp"));
        }

        setListAdapter(new MessagesArray(
                getApplicationContext(), R.layout.thread_list_layout, namesList));
    }

    static class ViewHolder {
        public TextView title;
        public TextView subText;
        public TextView timeStamp;
    }

    private class MessagesArray extends ArrayAdapter {

        public MessagesArray(Context context, int resource, List objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.thread_list_layout, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.thread_list_title);
                holder.subText = (TextView) convertView.findViewById(R.id.thread_list_sub_text);
                holder.timeStamp = (TextView) convertView.findViewById(R.id.thread_time);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.title.setText((String)getItem(position));
            holder.subText.setText(lastMessage.get(position));
            holder.timeStamp.setText(lastMessageTime.get(position));
            return convertView;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra("user_table", namesList.get(position));
        startActivity(intent);
    }
}
