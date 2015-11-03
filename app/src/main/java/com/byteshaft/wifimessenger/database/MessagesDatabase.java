package com.byteshaft.wifimessenger.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class MessagesDatabase extends SQLiteOpenHelper {

    public MessagesDatabase(Context context) {
        super(context, DatabaseConstants.DATABASE_NAME, null, DatabaseConstants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseConstants.TABLE_CREATE_MAIN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + DatabaseConstants.TABLE_CREATE_MAIN);
        onCreate(db);
    }

    private void updateTableIndex(SQLiteDatabase db, String name,
                                  String lastMsg, String lastMsgTime) {

        String query = "SELECT * FROM "
                + DatabaseConstants.TABLE_NAME
                + " WHERE "
                + DatabaseConstants.TABLES
                + "="
                + String.format("'%s'", name);
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int id = cursor.getInt(cursor.getColumnIndex(DatabaseConstants.ID_COLUMN));

        ContentValues values = new ContentValues();
        values.put(DatabaseConstants.LAST_MESSAGE, lastMsg);
        values.put(DatabaseConstants.LAST_MESSAGE_TIME, lastMsgTime);
        db.update(DatabaseConstants.TABLE_NAME, values, "ID=" + id, null);
        System.out.println("Last message saved");
        cursor.close();
    }

    private void addNewTableIndex(SQLiteDatabase db, String name,
                                  String lastMsg, String lastMsgTime) {

        ContentValues values = new ContentValues();
        System.out.println("Adding: " + name);
        values.put(DatabaseConstants.TABLES, name);
        values.put(DatabaseConstants.LAST_MESSAGE, lastMsg);
        values.put(DatabaseConstants.LAST_MESSAGE_TIME, lastMsgTime);
        db.insert(DatabaseConstants.TABLE_NAME, null, values);
    }

    private boolean doesTableExist(SQLiteDatabase db, String name) {
        boolean exists = false;
        Cursor cursor = db.rawQuery(
                "select DISTINCT tbl_name from sqlite_master where tbl_name = '"+name+"'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exists = true;
            }
            cursor.close();
        }
        return exists;
    }

    private void createNewThreadTableIfNotExists(SQLiteDatabase db, String tableName,
                                                 String lastMsg, String lastMsgTime) {

        db.execSQL(DatabaseConstants.getThreadDefinition(tableName));
        addNewTableIndex(db, tableName, lastMsg, lastMsgTime);
    }

    public void addNewMessageToThread(String uniqueId, String body, String direction, String time) {
        SQLiteDatabase db = getWritableDatabase();

        if (doesTableExist(db, uniqueId)) {
            updateTableIndex(db, uniqueId, body, time);
        } else {
            createNewThreadTableIfNotExists(db, uniqueId, body, time);
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseConstants.BODY, body);
        values.put(DatabaseConstants.DIRECTION, direction);
        values.put(DatabaseConstants.TIME, time);
        db.insert(uniqueId, null, values);
        db.close();
    }

    public ArrayList<HashMap> getAllTablesIndexData() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseConstants.TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<HashMap> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            int unique_id = cursor.getInt(
                    cursor.getColumnIndex(DatabaseConstants.ID_COLUMN));
            String tableName = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.TABLES));
            String body = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.LAST_MESSAGE));
            String time = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.LAST_MESSAGE_TIME));
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("unique_id", String.valueOf(unique_id));
            hashMap.put("table_name", tableName);
            hashMap.put("body", body);
            hashMap.put("time_stamp", time);
            list.add(hashMap);
        }
        db.close();
        cursor.close();
        return list;
    }

    public ArrayList<HashMap> getMessagesForContact(String tableName) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + tableName;
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<HashMap> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            int unique_id = cursor.getInt(
                    cursor.getColumnIndex(DatabaseConstants.ID_COLUMN));
            String messageBody = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.BODY));
            String messageDirection = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.DIRECTION));
            String messageTime = cursor.getString(
                    cursor.getColumnIndex(DatabaseConstants.TIME));
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("unique_id", String.valueOf(unique_id));
            hashMap.put("body", messageBody);
            hashMap.put("direction", messageDirection);
            hashMap.put("time_stamp", messageTime);
            list.add(hashMap);
        }
        db.close();
        cursor.close();
        return list;
    }
}
