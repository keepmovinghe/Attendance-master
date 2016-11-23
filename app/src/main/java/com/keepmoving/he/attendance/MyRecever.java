package com.keepmoving.he.attendance;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by hdb on 2016/11/21.
 */

public class MyRecever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(intent.getStringExtra("name"));
        // 保存数据
        DatabaseHelper dbh = new DatabaseHelper(context,"SamG_Checkin");
        SQLiteDatabase sd = dbh.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone_ID",intent.getStringExtra("phone_ID"));
        values.put("link_flag",intent.getStringExtra("link_flag"));
        values.put("name", intent.getStringExtra("name"));
        values.put("number", intent.getStringExtra("number"));
        sd.insert("CheckinTable", null, values);
        sd.close();
    }
}
