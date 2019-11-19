package com.example.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.mockgps.MainActivity;
import com.example.mockgps.R;

import java.lang.reflect.Field;

public class FloatWindow implements View.OnTouchListener {

    private Context mContext;
    private WindowManager.LayoutParams mWindowParams;
    private WindowManager mWindowManager;

    private View mFloatLayout;
    private float mInViewX;
    private float mInViewY;
    private float mDownInScreenX;
    private float mDownInScreenY;
    private float mInScreenX;
    private float mInScreenY;

    private long firstClickTime = 0;

    FloatWindow(Service context) {
        this.mContext = context;

        initFloatWindow();
    }

    @SuppressLint("InflateParams")
    private void initFloatWindow(){
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if(inflater == null)
            return;
        mFloatLayout = (View) inflater.inflate(R.layout.float_button, null);
        mFloatLayout.setOnTouchListener(this);

        Button northButton = (Button)mFloatLayout.findViewById(R.id.northButton);

        northButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePos(0,250);
            }
        });


        Button southButton = (Button)mFloatLayout.findViewById(R.id.southButton);

        southButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePos(0,-250);
            }
        });

        Button eastButton = (Button)mFloatLayout.findViewById(R.id.eastButton);

        eastButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePos(250,0);
            }
        });

        Button westButton = (Button)mFloatLayout.findViewById(R.id.westButton);

        westButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePos(-250,0);
            }
        });


        mWindowParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {//8.0新特性
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{
            mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mWindowParams.format = PixelFormat.RGBA_8888;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowParams.gravity = Gravity.START | Gravity.TOP;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) mContext.getSystemService(Service.WINDOW_SERVICE);
        if (manager != null) {
            manager.getDefaultDisplay().getMetrics(metrics);
        }

        float density = metrics.density;


        mWindowParams.width =(int)( 150*density);
        mWindowParams.height =(int)( 150*density);
//        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        Log.d("FLOAT","initFloatWindow finish");



    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return floatLayoutTouch(motionEvent);
    }

    private boolean floatLayoutTouch(MotionEvent motionEvent){

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("FLOAT","ACTION_DOWN");
                clicks(500);
                // 获取相对View的坐标，即以此View左上角为原点
                mInViewX = motionEvent.getX();
                mInViewY = motionEvent.getY();
                // 获取相对屏幕的坐标，即以屏幕左上角为原点
                mDownInScreenX = motionEvent.getRawX();
                mDownInScreenY = motionEvent.getRawY() - getSysBarHeight(mContext);
                mInScreenX = motionEvent.getRawX();
                mInScreenY = motionEvent.getRawY() - getSysBarHeight(mContext);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("FLOAT","ACTION_MOVE");
                // 更新浮动窗口位置参数
//                mInViewX = motionEvent.getX();
//                mInViewY = motionEvent.getY();

                mInScreenX = motionEvent.getRawX();
                mInScreenY = motionEvent.getRawY() - getSysBarHeight(mContext);
                mWindowParams.x = (int) (mInScreenX- mInViewX);
                mWindowParams.y = (int) (mInScreenY - mInViewY);
                // 手指移动的时候更新小悬浮窗的位置

//                if(moveAble) {
//                    updatePos(mInScreenX- mInViewX, mInScreenY - mInViewY);
//                }else{
                    mWindowManager.updateViewLayout(mFloatLayout, mWindowParams);
//                }

                break;
            case MotionEvent.ACTION_UP:
                Log.d("FLOAT","ACTION_UP");
                // 如果手指离开屏幕时，xDownInScreen和xInScreen相等，且yDownInScreen和yInScreen相等，则视为触发了单击事件。
                if (mDownInScreenX  == mInScreenX && mDownInScreenY == mInScreenY){

                }
                nowPisX = 0;
                nowPisY=0;
                break;
        }
        return true;
    }

    public void showFloatWindow(){
        if (mFloatLayout.getParent() == null){
            DisplayMetrics metrics = new DisplayMetrics();
            //默认固定位置，靠屏幕右边缘的中间
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            mWindowParams.x = metrics.widthPixels;
            mWindowParams.y = metrics.heightPixels/3*2 - getSysBarHeight(mContext);
            mWindowManager.addView(mFloatLayout, mWindowParams);
        }
    }

    public void hideFloatWindow(){
        if (mFloatLayout.getParent() != null)
            mWindowManager.removeView(mFloatLayout);
    }

    public void setFloatLayoutAlpha(boolean alpha){
        if (alpha)
            mFloatLayout.setAlpha((float) 0.5);
        else
            mFloatLayout.setAlpha(1);
    }

    public static float nowPisX= 0;
    public static float nowPisY= 0;
    private void  updatePos(float pisX,float pisY){
//        nowPisX = pisX;
////        nowPisY=pisY;

        String latLngStr[]=MockGpsService.latLngInfo.split("&");

        double lat = Double.valueOf(latLngStr[0]) + (pisX) / 250 * 0.00008;
        latLngStr[0]=lat+"";
        double lng = Double.valueOf(latLngStr[1]) + (pisY) / 250 * 0.00008;
        latLngStr[1]=lng+"";
        MockGpsService.latLngInfo = latLngStr[0]+"&"+latLngStr[1];

//
//        DisplayToast(MockGpsService.targetlatLngInfo );
    }
    private boolean moveAble=true;
    private void clicks(int intervalTime){ // 最长间隔时间

        DisplayToast("danji");


        if(firstClickTime > 0){
            if(System.currentTimeMillis() - firstClickTime < intervalTime){

                moveAble = !moveAble;
                DisplayToast("小窗口移动："+moveAble);
//                Log.d("TEST","双击事件");
//                firstClickTime = 0; // 将第一次点击时间置为0
//
//                //唤起MainActivity
//                Intent intent = new Intent(mContext, MainActivity.class);
//                intent.addCategory(Intent.CATEGORY_LAUNCHER);
//                intent.setAction(Intent.ACTION_MAIN);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//                mContext.startActivity(intent);
            }
        }

        firstClickTime = System.currentTimeMillis();
    }

    // 获取系统状态栏高度
    @SuppressLint("PrivateApi")
    private static int getSysBarHeight(Context contex) {
        Class<?> c;
        Object obj;
        Field field;
        int x;
        int sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = contex.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return sbar;
    }
    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(mContext, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }
}