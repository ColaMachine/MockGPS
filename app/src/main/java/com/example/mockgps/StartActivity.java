package com.example.mockgps;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * 开始类 负责
 */
public class StartActivity extends AppCompatActivity {

    private Button startBtn;
    private String permissionInfo;
    private final int SDK_PERMISSION_REQUEST = 127;
    private ArrayList<String> permissions;
    private Handler handler;
    private boolean isStart;
    private EditText et_name,et_pass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        startBtn=(Button)findViewById(R.id.startButton);

        et_name = (EditText) findViewById(R.id.usename);
        et_pass = (EditText) findViewById(R.id.password);


        permissions = new ArrayList<String>();
        getPersimmions();
        isStart=false;
        if (permissions.size()==0){
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (!isStart){
                        isStart=true;
                        startMain();
                    }
                }
            }, 2000);
        }
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isStart){
                    isStart=true;
                    startMain();
                }
//                //获取用户名
//                String userName=et_name.getText().toString();
//                String password = et_pass.getText().toString();
//                send(userName,password);
            }
        });
    }


    private void startMain(){
        Intent intent=new Intent(StartActivity.this,MainActivity.class);
        startActivity(intent);
        StartActivity.this.finish();
    }


    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            //悬浮窗
//            if (checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
//            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
//            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
//            }
            // 读取电话状态权限
//            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
//                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
//            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            } else {
                permissionsList.add(permission);
                return false;
            }

        } else {
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    private void send(String userName,String password) {

        final String userName2 = userName;
        final String password2 = md5(password);
        //开启线程，发送请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                OutputStreamWriter out =null;
                try {
                    URL url = new URL("http://www.dapark.top/home/sys/auth/login");
                    String content ="{\"loginName\":\""+userName2+"\",\"pwd\":\""+password2+"\",\"picCaptcha\":\"5719\"}";
                    connection = (HttpURLConnection) url.openConnection();
                    //设置请求方法
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type","application/json;charset=utf-8");
                    //设置连接超时时间（毫秒）
                    connection.setConnectTimeout(5000);
                    //设置读取超时时间（毫秒）
                    connection.setReadTimeout(5000);
                    connection.setDoOutput(true);
                    out=new OutputStreamWriter(connection.getOutputStream(),"utf-8");
                    out.write(content);
                    out.flush();
                    out.close();
                    //返回输入流
                    InputStream in = connection.getInputStream();

                    //读取输入流
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    show(result.toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {//关闭连接
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
    private void show(final String result) {



            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        JSONObject jsonObject = new JSONObject(result);

                          Integer r = (Integer)jsonObject.get("r");
                          if(r==0){
                              if (!isStart) {
                                  isStart = true;
                                  startMain();
                              }
                          }else{
                              String msg = (String)jsonObject.get("msg");
                              DisplayToast(msg);
                          }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(StartActivity.this, str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }

    public static void main(String args[]){

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest("awifi@123".getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            System.out.println(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
      //  System.out.println(md5("123456"));
    }
    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";

    }
}
