package com.kong.bjxyqxun.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.model.LatLng;
import com.kong.bjxyqxun.R;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationService extends Service implements AMapLocationListener {

    private static final String TAG = "gd";
    //    // 定位相关
//    private LocationClient mLocClient;
//    //
    private int msgId = -1;
    private PowerManager.WakeLock wakeLock = null;

    //声明AMapLocationClient类对象
    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption locationOption;
    private final int INTERVAL_ACTION_PATROL_FREQUENCY = 1000;
    private final int INTERVAL_ACTION_FAST_LOCATION = 1000 * 3;//如果速度过快，改为3秒定一次
    private AMapLocation location = null;
    private int mInterval = 1;
    //android 8.0后台定位权限
    private static final String NOTIFICATION_CHANNEL_NAME = "BG_LOCTION";
    private NotificationManager notificationManager = null;
    boolean isCreateChannel = false;
    public static long mLocationMs = 0;
    public static long mUploadStampMs = 0;
    private List<LatLng> latLngPool = new ArrayList<>();//存连续定位五个点


    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(12123, buildNotification());
        initGaoDe();
    }

    private void initGaoDe() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        locationOption = new AMapLocationClientOption();
        /**
         * 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
         */
//        locationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
        //设置定位模式为AMapLocationMode.Device_Sensors，仅设备模式。
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        locationOption.setOnceLocation(false);

        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
//        option.setOnceLocationLatest(true);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        locationOption.setInterval(1000 * 1);
        //设置是否返回地址信息（默认返回地址信息）
        locationOption.setNeedAddress(true);
        //设置是否允许模拟位置,默认为true，允许模拟位置
        locationOption.setMockEnable(true);
        locationOption.setGpsFirst(true);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        locationOption.setHttpTimeOut(50000);
        //关闭缓存机制
        locationOption.setLocationCacheEnable(false);
        if (null != mLocationClient && mInterval != 0) {
            mLocationClient.setLocationOption(locationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
    }

    //
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            msgId = intent.getIntExtra("msgId", -1);
        }
        flags = START_STICKY;
        acquireWakeLock();
        // 刷新定位
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.startLocation();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation == null) {
            Log.i(TAG, "amapLocation is null!");
            return;
        }
        if (amapLocation.getErrorCode() != 0) {
            Log.i(TAG, "amapLocation has exception errorCode:" + amapLocation.getErrorCode());
            return;
        } else {
            /**做点位上传或者保存操作 */
        }
        Double longitude = amapLocation.getLongitude();//获取经度
        Double latitude = amapLocation.getLatitude();//获取纬度
        String longitudestr = String.valueOf(longitude);
        String latitudestr = String.valueOf(latitude);
        Log.d(TAG, "longitude:" + longitude + ",latitude：" + latitude);
        if (amapLocation.getErrorCode() == 0) {


        }
    }

    /**
     * 获取时间
     *
     * @return
     */
    public String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        return str;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        if (mLocationClient != null) {
            mLocationClient.unRegisterLocationListener(this);
            mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
            mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * PARTIAL_WAKE_LOCK:保持CPU 运转，屏幕和键盘灯有可能是关闭的。
     * SCREEN_DIM_WAKE_LOCK：保持CPU 运转，允许保持屏幕显示但有可能是灰的，允许关闭键盘灯
     * SCREEN_BRIGHT_WAKE_LOCK：保持CPU 运转，允许保持屏幕高亮显示，允许关闭键盘灯
     * FULL_WAKE_LOCK：保持CPU 运转，保持屏幕高亮显示，键盘灯也保持亮度
     * ACQUIRE_CAUSES_WAKEUP：强制使屏幕亮起，这种锁主要针对一些必须通知用户的操作.
     * ON_AFTER_RELEASE：当锁被释放时，保持屏幕亮起一段时间
     */
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, getClass()
                    .getCanonicalName());
            if (null != wakeLock) {
                //   Log.i(TAG, "call acquireWakeLock");
                Log.d("33333", "call acquireWakeLock");
                wakeLock.acquire();
            }
        }
    }

    private Notification buildNotification() {

        Notification.Builder builder = null;
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (null == notificationManager) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            String channelId = getPackageName();
            if (!isCreateChannel) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId,
                        NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.enableLights(false);//是否在桌面icon右上角展示小圆点
                notificationChannel.setLightColor(Color.BLUE); //小圆点颜色
                notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                notificationManager.createNotificationChannel(notificationChannel);
                isCreateChannel = true;
            }
            builder = new Notification.Builder(getApplicationContext(), channelId);
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("后台定位服务")
                .setContentText("")
                .setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            return builder.getNotification();
        }
        return notification;
    }

    // 释放设备电源锁
    private void releaseWakeLock() {
        if (null != wakeLock && wakeLock.isHeld()) {
            Log.d("33333", "call releaseWakeLock");
            //   Log.i(TAG, "call releaseWakeLock");
            wakeLock.release();
            wakeLock = null;
        }
    }
}