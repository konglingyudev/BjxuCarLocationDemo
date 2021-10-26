package com.kong.bjxyqxun.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.LongDef;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.model.LatLng;
import com.kong.bjxyqxun.application.MyApplication;
import com.kong.bjxyqxun.callback.RtcmSDKCallbackListen;
import com.kong.bjxyqxun.callback.SerialListen;

import com.kong.bjxyqxun.impl.RtcmSDKCallbackImpl;
import com.kong.bjxyqxun.util.MyLog;
import com.kong.bjxyqxun.util.SerialPortUtil;
import com.kong.bjxyqxun.util.Utils;
import com.qxwz.sdk.configs.AccountInfo;
import com.qxwz.sdk.configs.SDKConfig;
import com.qxwz.sdk.core.RtcmSDKManager;
import com.qxwz.sdk.types.KeyType;

import static com.qxwz.sdk.core.Constants.QXWZ_SDK_CAP_ID_NOSR;


//import static com.qxwz.sdk.core.Constants.QXWZ_SDK_CAP_ID_NOSR;

public class KeepLiveService extends Service {

    private static final int SERVICE_ID = 1;
    private static final String TAG = "KeepLiveService";

    private static final String AK ="A4941qqf8t1m"; //"538179";
    private static final String AS ="05917040e8201515"; //"098728cf4ebbcb1b151e9ade7cb171af66dd9eb18a632aa070405b8bf2925933";
    private static final String DEVICE_ID ="70804192021"; //"70804190077";
    private static final String DEVICE_TYPE ="bjxy-test";// "ipad-android7";
    private static final String GGA = "$GPGGA,080353.20,3120.49244127,N,12129.84774365,E,1,23,0.6,69.2205,M,11.7973,M,,*5E";

    // RtcmSDK 开放能力
    private boolean isStart = false;

    // 串口通讯
    private String lastGGA;// 低精度解
    private String height2GGA;
    private String height5GGA;

    //重构后的代码
    private SerialPortUtil serialPortUtil;
    private RtcmSDKCallbackImpl mRtcmSDKCallbackImpl;
    private SendGgaThread mSendGgaThread = null;


    @Override
    public void onCreate() {
        super.onCreate();
        // 首次创建服务时，系统将调用此方法。服务如果已经运行，则不好调用此方法
        // 该方法只调用一次
        Log.d(TAG, "开启ForegroundService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 5. 停⽌能⼒
        isStart = false;
        RtcmSDKManager.getInstance().stop(QXWZ_SDK_CAP_ID_NOSR);
        // 6. 关闭sdk
        RtcmSDKManager.getInstance().cleanup();
        Log.d(TAG, "销毁ForegroundService");

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 当另一个组件通过调用startService()请求启动服务时，系统将调用此方法

        // 判断版本 启动service
        versionSelect();
        // 初始化
        initSerialPort();
        initRtcmSDK();

        return START_STICKY;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void versionSelect() {
        //判断版本
        if (Build.VERSION.SDK_INT < 18) {//Android4.3以下版本
            //将Service设置为前台服务，可以取消通知栏消息
            startForeground(SERVICE_ID, new Notification());

        } else if (Build.VERSION.SDK_INT < 24) {//Android4.3 - 7.0之间
            //将Service设置为前台服务，可以取消通知栏消息
            startForeground(SERVICE_ID, new Notification());
            startService(new Intent(this, InnerService.class));

        } else {//Android 8.0以上
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel("channel", "name", NotificationManager.IMPORTANCE_NONE);
                manager.createNotificationChannel(channel);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel");

                //将Service设置为前台服务,Android 8.0 App启动不会弹出通知栏消息，退出后台会弹出通知消息
                //Android9.0启动时候会立刻弹出通知栏消息
                startForeground(SERVICE_ID, new Notification());
            }
        }
    }

    private void initRtcmSDK() {
        // 千寻SDK 开放能力
        mRtcmSDKCallbackImpl = new RtcmSDKCallbackImpl();
        mRtcmSDKCallbackImpl.setSerialListen(new RtcmSDKCallbackListen() {
            @Override
            public void onData(int type, byte[] bytes) {
                // 收到千寻SDK 返回的Rtcm数据后，发送给串口解算，返回校正后的gga数据
                Log.d(TAG, "收到千寻的Rtcm 数据: " + bytes.length);
                sendSerialPort(bytes);
            }

            @Override
            public void isStart(boolean vIsStart) {
                Log.d(TAG, "isStart:====> " + vIsStart);
                isStart = vIsStart;
                // 开启给千寻SDK线程
                if (mSendGgaThread == null) {
                    mSendGgaThread = new SendGgaThread();
                }
                try {
                    mSendGgaThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        SDKConfig.Builder builder = SDKConfig.builder()
                .setAccountInfo(//设置账号
                        AccountInfo.builder()
                                .setKeyType(KeyType.QXWZ_SDK_KEY_TYPE_AK)//设置账号类型
                                .setKey(AK)//设置账号
                                .setSecret(AS)//设置秘钥
                                .setDeviceId(DEVICE_ID)//设置设备ID
                                .setDeviceType(DEVICE_TYPE)//设置设备类型
                                .build())// 账号信息
                .setRtcmSDKCallback(mRtcmSDKCallbackImpl);//设置回调

        // 1. 初始化
        RtcmSDKManager.getInstance().init(builder.build());
        // 2. 鉴权
        //  SDK需完成鉴权后才能提供服务，鉴权⽅法是异步⽅法，鉴权结束后通过调⽤初始化时传⼊的回调接⼝
        //  实例中的onAuth⽅法通知⽤户鉴权结果
        RtcmSDKManager.getInstance().auth();
    }

    private void initSerialPort() {
        // 开启串口
        serialPortUtil = new SerialPortUtil();
        serialPortUtil.openSerialPort("/dev/ttyS6", 115200, 0);
        // 监听串口返回消息
        serialPortUtil.setSerialListen(new SerialListen() {
            @Override
            public void getSerial(String msg) {
                // 接受到串口的数据
                doGGAMsg(msg);
            }
        });
    }

    private void sendSerialPort(byte[] bytes) {
        // 开启子线程进行发送数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                serialPortUtil.sendSerialPort(bytes);
            }
        }).start();
    }

    /**
     * 对从串口返回的GGA数据进行筛选
     *
     * @param msg
     */
    private void doGGAMsg(String msg) {
        // 0=未定位，1=GPS单点定位固定解，2=差分定位，3=PPS解；
        // 4=RTK固定解；5=RTK浮点解；6=估计值；7=手工输入模式；8=模拟模式；

        // 4=RTK固定解，拥有固定解意味着解算出了正确的解。在常规条件下，你拥有了1~3cm的测量精度。
        // 5=RTK浮点解，又称差分解，此时算法尚未得到固定解(fix)。由于没有固定解（fix），因此提供了一种Float解决方案，
        // 它的位置始终比固定(fix)解决方案的精度低，此时的定位精度介于厘米级和米级之间
        String[] parts = msg.split(",");
        if (parts.length > 6) Log.d(TAG, "拼接后的数据====> 质量因子：" + parts[6] + "  " + msg);

        try {
            if (parts.length > 13 && !parts[6].equals("") && Utils.isNumeric(parts[6]) && Integer.parseInt(parts[6]) != 0) {
                Log.d(TAG, "gga数据解析=>" + "质量因子：" + parts[6] + "  水平精度因子：" + parts[8] + "  差分时间：" + parts[13]);
                if (Utils.isNumeric(parts[6])) {
                    int type = Integer.parseInt(parts[6]);
                    if (type != 0) GpsToGd(type, msg, parts);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void GpsToGd(int type, String msg, String[] parts) {
        lastGGA = msg;
        double lon = Double.parseDouble(Utils.parseLon(parts[4], parts[5]));//经度
        double lat = Double.parseDouble(Utils.parseLat(parts[2], parts[3]));

        LatLng lang = new LatLng(lat, lon);
        Log.d(TAG, "GPS  经纬度：[" + lang.longitude + "，" + lang.latitude + "]");
        CoordinateConverter converter = new CoordinateConverter(MyApplication.getContext());
        // CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
        // 转换
        converter.coord(lang);
        // 获取转换之后的高德坐标
        LatLng result = converter.convert();

        // 发送广播
        Intent intent = new Intent();
        intent.setAction("gps.lang");
        intent.putExtra("type", type);
        intent.putExtra("LatLng", result);
        intent.putExtra("num",parts[7]);
        sendBroadcast(intent);
    }

    /**
     * 内部类 子线程
     * 发送gga数据 => RtcmSDK
     */
    private class SendGgaThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isStart) {
                //能⼒启动成功后可通过每隔⼀段时间(间隔时间不短于1秒)上传GGA获取差分数据，差分数据通过调⽤初
                //始化时传⼊的回调接⼝实例中的onData⽅法传递给⽤户
                if (lastGGA != null) {
                    String[] parts = lastGGA.split(",");
                    if (parts.length > 6) {
                        MyLog.d(TAG, "传给千寻的 GGA 质量因子：" + parts[6] + "  ==> " + lastGGA);
                    }
                    RtcmSDKManager.getInstance().sendGga(lastGGA);
                    SystemClock.sleep(1000);
                }
            }
        }
    }

    public static class InnerService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(SERVICE_ID, new Notification());
            stopForeground(true);//移除通知栏消息
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
    }
}