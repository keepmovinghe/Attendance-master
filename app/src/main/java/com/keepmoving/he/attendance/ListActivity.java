package com.keepmoving.he.attendance;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListActivity extends android.app.ListActivity {


    private void dialog(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(msg);
        builder.show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        this.getIntent();
        // wifi ip
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        // 数据库实例
        DatabaseHelper dbh = new DatabaseHelper(ListActivity.this,"SamG_Checkin");
        SQLiteDatabase sd = dbh.getReadableDatabase();
        Cursor cursor=sd.query("CheckinTable", new String[]{"name","number"}, "link_flag=?", new String[]{String.valueOf(ipAddress)}, null, null, null);
        final ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
        final HashMap<String,String> titleMap = new HashMap<String,String>();
        titleMap.put("getname", "姓名");
        titleMap.put("getnumber", "学号");
        list.add(titleMap);

        while(cursor.moveToNext()){
            for(int i=0;i<cursor.getCount();i++){
                cursor.moveToPosition(i);
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String number = cursor.getString(cursor.getColumnIndex("number"));
                //String name=_intent.getStringExtra("name");
                //String number=_intent.getStringExtra("number");
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("getname", name);
                map.put("getnumber", number);
                list.add(map);
            }
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.user, new String[]{"getname","getnumber"}, new int[]{R.id.txt1,R.id.txt2});
        setListAdapter(adapter);
        cursor.close();
        Button btn1 = (Button)findViewById(R.id.button2);
        btn1.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v) {
                // TODO Auto-generated method stub
                DatabaseHelper dbh = new DatabaseHelper(ListActivity.this,"SamG_Checkin");
                SQLiteDatabase sd = dbh.getReadableDatabase();
                sd.delete("CheckinTable", null, null);
                sd.close();
                list.clear();
                list.add(titleMap);
                SimpleAdapter adapter = new SimpleAdapter(getBaseContext(),list,R.layout.user,new String[]{"getname","getnumber"},new int[]{R.id.txt1,R.id.txt2});
                setListAdapter(adapter);
                System.out.println("已清除数据库！");
            }

        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        System.out.println("id--------"+id);
        System.out.println("position--------"+position);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("退出");
        menu.add("关于我们");
        return true;
    }
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        if(item.getTitle().equals("退出"))
            finish();
        else if(item.getTitle().equals("关于我们"));
        return super.onMenuItemSelected(featureId, item);

    }

}
