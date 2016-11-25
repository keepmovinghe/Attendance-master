package com.keepmoving.he.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends Activity {

    private Button button = null;
    private Button buttonList = null;
    private EditText et1 = null;
    private EditText et2 = null;
    public static final int PORT = 3358;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    JSONObject saveDate(String phone_ID, String link_flag, String name, String number, String clientIp){
        JSONObject jsonObject = new JSONObject();
        // db实例
        DatabaseHelper dbh = new DatabaseHelper(MainActivity.this,"SamG_Checkin");
        SQLiteDatabase sdRead = dbh.getReadableDatabase();
        // 查询是否已签到
        Cursor cursor=sdRead.query("CheckinTable", new String[]{"name","number"}, "number=?", new String[]{number}, null, null, null);
        if(cursor.getCount()>0){
            // 已签到
            cursor.close();
            sdRead.close();
            try {
                jsonObject.put("result","您已签到！");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
        // 查询是否替签
        cursor=sdRead.query("CheckinTable", new String[]{"name","number"}, "phone_ID=?", new String[]{phone_ID}, null, null, null);
        if(cursor.getCount()>0){
            // 该设备已签到
            cursor.close();
            sdRead.close();
            try {
                jsonObject.put("result","该设备已签到！");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
        // 保存数据
        SQLiteDatabase sd = dbh.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone_ID",phone_ID);
        values.put("link_flag",String.valueOf(link_flag));
        values.put("name", name);
        values.put("number", number);
        sd.insert("CheckinTable", null, values);
        sd.close();
        try {
            jsonObject.put("result","签到成功！");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    private void init(){
        //做按钮跳转
        button = (Button)findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v) {
                // TODO Auto-generated method stub
                et1 = (EditText)findViewById(R.id.editText);
                et2 = (EditText)findViewById(R.id.editText1);
                if((et1.getText().toString()).equals("请输入姓名")||((et1.getText().toString()).equals(""))) {
                    Toast.makeText(MainActivity.this, "请输入姓名！", Toast.LENGTH_SHORT).show();
                    et1.setFocusable(true);
                    et1.setFocusableInTouchMode(true);
                    et1.requestFocus();
                    et1.findFocus();
                    //dialog("请输入姓名！");
                }
                else if((et2.getText().toString()).equals("请输入学号")||((et2.getText().toString()).equals(""))) {
                    Toast.makeText(MainActivity.this, "请输入学号", Toast.LENGTH_SHORT).show();
                    et2.setFocusable(true);
                    et2.setFocusableInTouchMode(true);
                    et2.requestFocus();
                    et2.findFocus();
                    //dialog("请输入学号！");
                }
                else {
                    // 设备ID
                    TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    String deviceId = tm.getDeviceId();
                    // wifi ip
                    WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String clientIp = intToIp(wifiInfo.getIpAddress());
                    String serverIp = intToIp(wifiManager.getDhcpInfo().serverAddress);
                    String name = et1.getText().toString();
                    String number = et2.getText().toString();
                    // 保存数据到本地
                    //saveDate(deviceId,serverIp,name,number);

                    JSONObject jsonObject=new JSONObject();
                    try {
                        jsonObject.put("phone_ID", deviceId);
                        jsonObject.put("link_flag", serverIp);
                        jsonObject.put("name", name);
                        jsonObject.put("number", number);
                        jsonObject.put("clientIp", clientIp);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                    WifiManager wifiManage = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    DhcpInfo info = wifiManage.getDhcpInfo();
                    WifiInfo wifiinfo = wifiManage.getConnectionInfo();
                    String ip = intToIp(wifiinfo.getIpAddress());
                    String serverAddress = intToIp(info.serverAddress);
                    new Sender(serverAddress,PORT,jsonObject).start();
                    Log.w("robin", "ip:" + ip + "serverAddress:" + serverAddress + info);

                    //发送广播
                    //sendBroadcas(values);
                }
            }
        });

        buttonList = (Button) findViewById(R.id.button);
        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ListActivity.class);
                startActivity(intent);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 启动监听端口
        Receiver service = new Receiver();
        service.start();
    }

    // 将获取的int转为真正的ip地址,参考的网上的，修改了下
    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    volatile Socket   mSocket;
    ServerSocket server;
    volatile String callbackResult="";
    private Handler mHandler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if(msg.what==0x02){
                new Thread(new  Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("客户端连接", "读取客户端发来的数据");
                            InputStream ins=mSocket.getInputStream();
                            ByteArrayOutputStream os=new ByteArrayOutputStream();
                            int len=0;
                            byte[] buffer=new byte[1024];
                            while((len=ins.read(buffer))!=-1){
                                os.write(buffer);
                            }
                            //第一步，生成Json字符串格式的JSON对象
                            JSONObject jsonObject=new JSONObject(os.toString());
                            //第二步，从JSON对象中取值如果JSON 对象较多，可以用json数组
                            String phone_ID=jsonObject.getString("phone_ID");
                            String link_flag=jsonObject.getString("link_flag");
                            String name=jsonObject.getString("name");
                            String number=jsonObject.getString("number");
                            String clientIp = jsonObject.getString("clientIp");
                            // 保存数据
                            jsonObject = saveDate(phone_ID,link_flag,name,number,clientIp);
                            //System.out.println("111111"+name+number+clientIp);
                            callbackResult = jsonObject.getString("result");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }finally{
                            if(mSocket!=null){
                                try {
                                    mSocket.close();
                                    mSocket=null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }else if (msg.what==000000){
                // 回调
                //System.out.println("msg.obj---->"+msg.obj);
                if(!"".equals(msg.obj)) {
                    display((String) msg.obj);
                }
            }
            //System.out.println("333333333"+callbackResult);
        }
    };

    /**
     * 接收信息
     */
    class Receiver extends Thread {
        Receiver(){
            super();
        }

        public void run() {
            try {
                server = new ServerSocket(PORT);
                while (true) {
                    //Looper.prepare();
                    Message message = Message.obtain();
                    mSocket = server.accept();
                    message.what = 0x02;
                    mHandler.sendMessage(message);
                    //Looper.loop();
                    //回复消息给客户端
                    OutputStream out = mSocket.getOutputStream();
                    out.write(callbackResult.getBytes());
                    out.flush();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送socket信息
     */
    class Sender extends Thread {
        String serverIp;
        int port;
        JSONObject jsonObject=null;

        Sender(String serverAddress,int port,JSONObject jsonObject) {
            super();
            serverIp = serverAddress;
            this.port= port;
            this.jsonObject=jsonObject;
        }

        public void run() {
            Socket sock = null;
            try {

                // 声明sock，其中参数为服务端的IP地址与自定义端口
                sock = new Socket(serverIp, port);
                Log.w("robin", "I am try to writer"+sock);

                if (sock != null) {
                    // 声明输出流out，向服务端输出“Output Message！！”
                    OutputStream out = sock.getOutputStream();
                    out.write(jsonObject.toString().getBytes());
                    out.flush();
                    sock.shutdownOutput();
                }
                sleep(500);
                InputStream ins=sock.getInputStream();
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                int len=0;
                byte[] buffer=new byte[1024];
                while((len=ins.read(buffer))!=-1){
                    os.write(buffer);
                }
                //System.out.println("555555"+os.toString());
                callbackResult = os.toString().trim();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = Message.obtain();
                        message.what = 000000;
                        message.obj=callbackResult;
                        mHandler.sendMessage(message);
                    }
                }).start();
            } catch (UnknownHostException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = Message.obtain();
                        message.what = 000000;
                        message.obj="签到失败，无法连接到设备！";
                        mHandler.sendMessage(message);
                    }
                }).start();
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ConnectException e){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = Message.obtain();
                        message.what = 000000;
                        message.obj="签到失败，设备没开启接收！";
                        mHandler.sendMessage(message);
                    }
                }).start();
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (sock != null) {
                        sock.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 发送广播
     * @param values
     */
    public void sendBroadcas(ContentValues values){
        Intent intent = new Intent("com.keepmoving.he.attendance");
        //intent.putExtra("values", values);
        intent.putExtra("phone_ID",String.valueOf(values.get("phone_ID")));
        intent.putExtra("link_flag",String.valueOf(values.get("link_flag")));
        intent.putExtra("name", String.valueOf(values.get("name")));
        intent.putExtra("number", String.valueOf(values.get("number")));
        //sendBroadcast(intent);
        //发送广播
        System.out.println(values.get("number"));
        sendBroadcast(intent);
    }

    @Override
    //做菜单栏
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

    //Toast数据
    private void display(String msg){
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
    }
    //做文本输入对话框
    private void dialog(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(msg);
        builder.show();
    }
}