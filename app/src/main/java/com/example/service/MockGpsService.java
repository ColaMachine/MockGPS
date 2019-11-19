package com.example.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.example.mockgps.MainActivity;
import com.example.mockgps.R;

import java.util.UUID;

public class MockGpsService extends Service {


    private String TAG = "MockGpsService";

    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isStop=true;

    //经纬度字符串
    public static String latLngInfo="104.06121778639009&30.544111926165282";
//    public static String targetlatLngInfo="104.06121778639009&30.544111926165282";
    //悬浮窗
    private FloatWindow floatWindow;
    private boolean isFloatWindowStart=false;


    public static final int RunCode=0x01;
    public static final int StopCode=0x02;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        LogUtil.configLog();
        Log.d(TAG, "onCreate");
        log.debug(TAG + ": onCreate");
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //for test
        getProviders();
        //remove default network location provider
        rmNetworkTestProvider();
        //remove gps provider
        rmGPSTestProvider();

        //add a new test network location provider
        setNetworkTestProvider();
//        add a GPS test Provider
        setGPSTestProvider();

        //thread
        handlerThread=new HandlerThread(getUUID(),-2);
        handlerThread.start();

        handler=new Handler(handlerThread.getLooper()){
            public void handleMessage(Message msg){
                try {
                    Thread.sleep(200);
                    if (!isStop){11
                        setNetworkLocation();
                        sendEmptyMessage(0);
                        //broadcast to MainActivity
                        Intent intent=new Intent();
                        intent.putExtra("statusCode", RunCode);
                        intent.setAction("com.example.service.MockGpsService");
                        sendBroadcast(intent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "setNetworkLocation error");
                    Thread.currentThread().interrupt();
                }
            }
        };
        handler.sendEmptyMessage(0);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DisplayToast("onStartCommand");
        Log.d(TAG, "onStartCommand");
//        DisplayToast("Mock Location Service Start");
        //

        String channelId = "channel_01";
        String name="channel_name";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
            Log.i(TAG, mChannel.toString());
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
            notification = new Notification.Builder(this)
                    .setChannelId(channelId)
                    .setContentTitle("位置模拟服务已启动")
                    .setContentText("MockLocation service is running")
                    .setSmallIcon(R.mipmap.ic_launcher).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("位置模拟服务已启动")
                    .setContentText("MockLocation service is running")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setChannelId(channelId);//无效
            notification = notificationBuilder.build();
        }
        startForeground(1,notification);
        //

        //get location info from mainActivity
        latLngInfo=intent.getStringExtra("key");
        //targetlatLngInfo=latLngInfo;
        Toast ts = Toast.makeText(MockGpsService.this,"接收到新的定位!", Toast.LENGTH_LONG);
        Log.d(TAG, "dataFromMain is "+latLngInfo);
        //start to refresh location
        isStop=false;

        //这里开启悬浮窗
        if (!isFloatWindowStart){
            floatWindow=new FloatWindow(this);
            floatWindow.showFloatWindow();
            isFloatWindowStart=true;
        }




//        return START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        DisplayToast("Mock Loction Service finish");
        Log.d(TAG, "onDestroy");
//        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();

//        DisplayToast("Mock Loction Service finish");
        isStop=true;

        //移除悬浮窗
        floatWindow.hideFloatWindow();
        isFloatWindowStart=false;

        handler.removeMessages(0);
        handlerThread.quit();
        rmNetworkProvider();

        stopForeground(true);

        //broadcast to MainActivity
        Intent intent=new Intent();
        intent.putExtra("statusCode", StopCode);
        intent.setAction("com.example.service.MockGpsService");
        sendBroadcast(intent);

        super.onDestroy();
    }


    //generate a location
    public Location generateLocation(LatLng latLng) {
        Location loc = new Location("gps");


        loc.setAccuracy(2.0F);
        loc.setAltitude(55.0D);
        loc.setBearing(1.0F);
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 7);
        loc.setExtras(bundle);


        loc.setLatitude(latLng.latitude);
        loc.setLongitude(latLng.longitude);
//        loc.setAccuracy(1.0F);
//        loc.setAltitude(10);
//        loc.setBearing(90);
        loc.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= 17) {
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
//        Log.d("WATCH",loc.toString());
        return loc;
    }
    private long lastMoveTime=0;
    private long excuteTimes =0;
    //set network location
    private void setNetworkLocation() { //DisplayToast("setNetworkLocation"+(excuteTimes++));
        //default location 30.5437233 104.0610342 成都长虹科技大厦
        String latLngStr[]=latLngInfo.split("&");
//        String targetlatLngStr[]=latLngInfo.split("&");
//        long nowTime = System.currentTimeMillis();
//        String ary[] = MockGpsService.targetlatLngInfo .split("&");
        if(Math.abs(FloatWindow.nowPisX)>100 ||Math.abs(FloatWindow.nowPisY)>100  ){
            DisplayToast("over range "+FloatWindow.nowPisX+"&"+FloatWindow.nowPisY);
        }
        if(FloatWindow.nowPisX!=0  || FloatWindow.nowPisY!=0) {
            double lat = Double.valueOf(latLngStr[0]) + (FloatWindow.nowPisX) / 250 * 0.00008;
            latLngStr[0]=lat+"";
            double lng = Double.valueOf(latLngStr[1]) + (FloatWindow.nowPisY) / 250 * 0.00008;
            latLngStr[1]=lng+"";
            latLngInfo = latLngStr[0]+"&"+latLngStr[1];
            //.targetlatLngInfo = lat + "&" + lng;
        }
//        double nowLat= Double.valueOf(latLngStr[1]);
//        double nowLng= Double.valueOf(latLngStr[0]);
        LatLng latLng = new LatLng(Double.valueOf(latLngStr[1]), Double.valueOf(latLngStr[0]));

//        if(targetlatLngInfo==null || targetlatLngInfo.equals("")){
//            targetlatLngInfo = latLngInfo;
//        }
//       if(!targetlatLngInfo.equals(latLngInfo)){
//           latLngInfo=targetlatLngInfo;

//
//           if(lastMoveTime==0){
//               lastMoveTime=nowTime;
//
//
//           }
//           if(nowTime-lastMoveTime>3000){
//               lastMoveTime=nowTime;
//
//           }
//           double targetLat= Double.valueOf(targetlatLngStr[1]);
//           double targetLng= Double.valueOf(targetlatLngStr[0]);
//         //  LatLng targetlatLng = new LatLng(Double.valueOf(targetlatLngStr[1]), Double.valueOf(targetlatLngStr[0]));
//          boolean changed =false;
//           if(Math.abs(targetLat-nowLat)>0.000008){
//               changed=true;
//               nowLat+=((targetLat-nowLat)<0?-1:1 )*0.000008  * (nowTime-lastMoveTime)/1000;//移动这一时间间隔里移动的距离 //1秒1米
//           }
//           if(Math.abs(targetLng-nowLng)>0.000008){
//               changed=true;
//               nowLng +=((targetLng-nowLng)<0?-1:1 )*0.000008  * (nowTime-lastMoveTime)/1000;//移动这一时间间隔里移动的距离 //1秒1米
//           }
//
//           if(!changed){
//               latLngInfo=targetlatLngInfo;
//           }else{
//               latLngInfo=nowLng+"&"+nowLat;
//           }
//
//           //如果都等于了
//
//           //latLng =new LatLng(Double.valueOf(targetlatLngStr[1]), Double.valueOf(targetlatLngStr[0]));
//
//           latLng = new LatLng(nowLat, nowLng);


//       }
        String providerStr = LocationManager.NETWORK_PROVIDER;
        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
            //for test
//            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, generateLocation(latLng));
        } catch (Exception e) {
            Log.d(TAG, "setNetworkLocation error");
            e.printStackTrace();
        }
        //慢慢的走过去

    //set gps location
    private void setGPSLocation(){
        //default location 30.5437233 104.0610342 成都长虹科技大厦
        Log.d(TAG, "setGPSLocation: " + latLngInfo);
        log.debug(TAG + ": setGPSLocation: " + latLngInfo);
        String latLngStr[] = latLngInfo.split("&");
        LatLng latLng = new LatLng(Double.valueOf(latLngStr[1]), Double.valueOf(latLngStr[0]));
        String providerStr = LocationManager.GPS_PROVIDER;
//        String providerStr2 = LocationManager.GPS_PROVIDER;
        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
//            locationManager.setTestProviderLocation(providerStr2, generateLocation(latLng));
            //for test
//            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, generateLocation(latLng));
        } catch (Exception e) {
            Log.d(TAG, "setGPSLocation error");
            log.debug(TAG + ": setGPSLocation error");
            e.printStackTrace();
        }
    }

    //remove network provider
    private void rmNetworkTestProvider() {
        try {
            for (int i = 0; i < 3; i++) {
                String providerStr = LocationManager.NETWORK_PROVIDER;
                if (locationManager.isProviderEnabled(providerStr)) {
                    Log.d(TAG, "now remove NetworkProvider: try_" + i);
                    log.debug(TAG + ": now remove NetworkProvider: try_" + i);
//                locationManager.setTestProviderEnabled(providerStr,true);
                    locationManager.removeTestProvider(providerStr);
                } else {
                    Log.d(TAG, "NetworkProvider is not enabled: try_" + i);
                    log.debug(TAG + ": NetworkProvider is not enabled: try_" + i);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "rmNetworkProvider error");
            log.debug(TAG + ": rmNetworkProvider error");
        }
    }

    //set new network provider
    private void setNetworkTestProvider() {
        String providerStr = LocationManager.NETWORK_PROVIDER;
//        String providerStr = LocationManager.GPS_PROVIDER;
        try {
            locationManager.addTestProvider(providerStr, false, false,
                    false, false, false, false,
                    false, 1, Criteria.ACCURACY_FINE);
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] success");
//            locationManager.setTestProviderStatus("network", LocationProvider.AVAILABLE, null,
//                    System.currentTimeMillis());
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] error");
        }
        if (!locationManager.isProviderEnabled(providerStr)) {
            try {
                locationManager.setTestProviderEnabled(providerStr, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "setTestProviderEnabled[NETWORK_PROVIDER] error");
                log.debug(TAG + ": setTestProviderEnabled[NETWORK_PROVIDER] error");
            }
        }
    }

    // for test: set GPS provider
    private void rmGPSTestProvider() {
        try {
            for (int i = 0; i < 3; i++) {
                String providerStr = LocationManager.GPS_PROVIDER;
                if (locationManager.isProviderEnabled(providerStr)) {
                    Log.d(TAG, "now remove GPSProvider: try_" + i);
                    log.debug(TAG + ": now remove GPSProvider: try_" + i);
//                locationManager.setTestProviderEnabled(providerStr,true);
                    locationManager.removeTestProvider(providerStr);
                } else {
                    Log.d(TAG, "GPSProvider is not enabled: try_" + i);
                    log.debug(TAG + ": GPSProvider is not enabled: try_" + i);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "rmGPSProvider error");
            log.debug(TAG + ": rmGPSProvider error");
        }
    }

    private void setGPSTestProvider() {
        LocationProvider provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
//        if (provider != null) {
//            locationManager.addTestProvider(
//                    provider.getName()
//                    , provider.requiresNetwork()
//                    , provider.requiresSatellite()
//                    , provider.requiresCell()
//                    , provider.hasMonetaryCost()
//                    , provider.supportsAltitude()
//                    , provider.supportsSpeed()
//                    , provider.supportsBearing()
//                    , provider.getPowerRequirement()
//                    , provider.getAccuracy());
//        } else {
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, true,
                    false, true, true, true, 0, 5);
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] success");
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] error");
        }

//        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "setTestProviderEnabled[GPS_PROVIDER] error");
                log.debug(TAG + ": setTestProviderEnabled[GPS_PROVIDER] error");
            }

        }


        //新
        locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null,
                System.currentTimeMillis());
    }


    //uuid random
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }


    //get service
    public class ServiceBinder extends Binder {
        public MockGpsService getService() {
            return MockGpsService.this;
        }
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }

}


