package com.kong.bjxyqxun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;


import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.kong.bjxyqxun.callback.LatLngCallback;

import com.kong.bjxyqxun.entity.PolylineLine;
import com.kong.bjxyqxun.receive.GpsReceiver;


import com.kong.bjxyqxun.util.MyLog;
import com.kong.bjxyqxun.util.Utils;


import java.util.ArrayList;

import java.util.List;

import android_path_smooth.PathSmoothTool;


public class MainActivity extends AppCompatActivity implements AMap.OnMapLoadedListener, LatLngCallback, RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "kly";

    private static final int TXUPDATE = 1;
    private static final int MAP = 2;
    private final MyHandle mHandle = new MyHandle();


    private RadioGroup rg;
    private TextView mTv;
    // 串口通讯
    private String lastGGA;// 低精度解
    private String height2GGA;
    private String height5GGA;

    private MapView mMapView = null;
    private AMap aMap = null;
    private PathSmoothTool mPathSmoothTool;
    private List<LatLng> mOriginList = new ArrayList<>();
    private Polyline mOriginPolyline, mkalmanPolyline;

    private GpsReceiver gpsReceiver = null;

    public int count1 = 0;
    public int count4 = 0;
    public int count2 = 0;
    public int count5 = 0;
    List<LatLng> points1 = new ArrayList<>();
    List<LatLng> points4 = new ArrayList<>();
    List<LatLng> points2 = new ArrayList<>();
    List<LatLng> points5 = new ArrayList<>();

    Polyline polyline1;
    Polyline polyline2;
    Polyline polyline4;
    Polyline polyline5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        initView();

        //获取地图控件引用
        initMap(savedInstanceState);

        mPathSmoothTool = new PathSmoothTool();
        mPathSmoothTool.setIntensity(4);

        initReceiver();
        mOriginList = Utils.readTxt();
        Log.d("kly1", "onCreate: len=" + mOriginList.size());

        addLocpath();
    }

    private void initView() {
        rg = findViewById(R.id.rg);
        mTv = findViewById(R.id.tv_zhil);
        rg.setOnCheckedChangeListener(this);
    }

    private void initReceiver() {
        // 注册广播接受者
        if (gpsReceiver == null) {
            gpsReceiver = new GpsReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("gps.lang");//要接收的广播
        registerReceiver(gpsReceiver, intentFilter);//注册接收者

        // 设置定位监听
        gpsReceiver.setLatLngListen(this);
    }

    //    //在地图上添加本地轨迹数据，并处理
    private void addLocpath() {
        if (mOriginList != null && mOriginList.size() > 0) {
            mOriginPolyline = aMap.addPolyline(new PolylineOptions().addAll(mOriginList).color(Color.GREEN));
            aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getBounds(mOriginList), 200));
            mOriginPolyline.setVisible(false);
//            amap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOriginList.get(0),15));
        }
        pathOptimize(mOriginList);
    }


    //轨迹平滑优化
    public List<LatLng> pathOptimize(List<LatLng> originlist) {
        List<LatLng> pathoptimizeList = mPathSmoothTool.pathOptimize(originlist);
        mkalmanPolyline = aMap.addPolyline(new PolylineOptions().addAll(pathoptimizeList).color(Color.parseColor("#FFC125")));
        mkalmanPolyline.setVisible(false);
        return pathoptimizeList;
    }

    private void initMap(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        //初始化地图控制器对象
        if (aMap == null) aMap = mMapView.getMap();
        aMap.setOnMapLoadedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        unregisterReceiver(gpsReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapLoaded() {
//        根据详细的经纬度绘制：两个经纬度位置之间用一条直线连接，多条直线连接在一起近似于轨迹

////List
//
//        list;
////路径、起点、终点
//        BitmapDescriptor trace = BitmapDescriptorFactory.fromResource(R.mipmap.trace);
//        BitmapDescriptor start = BitmapDescriptorFactory.fromResource(R.mipmap.start);
//        BitmapDescriptor end = BitmapDescriptorFactory.fromResource(R.mipmap.end);
//        if (list.size() > 0) {
//            //添加起点、终点
//            OverlayOptions startOptions = new MarkerOptions().position(list.get(0)).icon(start);
//            OverlayOptions endOptions = new MarkerOptions().position(list.get(list.size() - 1)).icon(end);
//            mBaiduMap.addOverlay(startOptions);
//            mBaiduMap.addOverlay(endOptions);
//        }
////绘制直线
//        PolylineOptions polylineOptions = new PolylineOptions().width(20).customTexture(trace).points(list).dottedLine(false);
//        mBaiduMap.addOverlay(polylineOptions);
    }

    // 广播监听
    @Override
    public void getLatLng(int type, String num, LatLng latLng) {
//        mOriginList.add(gd.getLatLng());
        MyLog.d("高德坐标", "type=" + type + "   坐标：" + latLng.toString());
        Message msg = mHandle.obtainMessage();
        msg.what = TXUPDATE;
        Bundle bundle = new Bundle();
        bundle.putString("str", "当前质量因子：" + type + "  \n 经纬度：[" + latLng.longitude + "，" + latLng.latitude + "] \n" +
                "使用卫星数量：" + num);
        msg.setData(bundle);
        mHandle.sendMessage(msg);

        switch (type) {
            case 1:
                points1.add(count1, latLng);
                count1++;
                drawLines(type);
                break;
            case 2:
                points2.add(count2, latLng);
                count2++;
                drawLines(type);
                break;
            case 4:
                points4.add(count4, latLng);
                count4++;
                drawLines(type);
                break;
            case 5:
                points5.add(count5, latLng);
                count5++;
                drawLines(type);
                break;
        }
    }


    /**
     * 绘制路线
     */
    public void drawLines(int type) {
        PolylineOptions options = new PolylineOptions();
//        options.setCustomTexture(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round));
        int color = Color.YELLOW;//-256
        List<LatLng> points = new ArrayList<>();
        switch (type) {
            case 1:
                color = Color.YELLOW;//
                points = points1;
                options.geodesic(true).setDottedLine(false).color(color).addAll(points).useGradient(true).width(5).visible(true);
                polyline1 = aMap.addPolyline(options);
                break;
            case 2:
                color = Color.GREEN;//-65536
                points = points2;
                options.geodesic(true).setDottedLine(false).color(color).addAll(points).useGradient(true).width(5).visible(true);
                polyline2 = aMap.addPolyline(options);
                break;
            case 4:
                color = Color.RED;//-16711936
                points = points4;
                options.geodesic(true).setDottedLine(false).color(color).addAll(points).useGradient(true).width(5).visible(true);
                polyline4 = aMap.addPolyline(options);
                break;
            case 5:
                color = Color.BLUE;//16776961
                points = points5;
                options.geodesic(true).setDottedLine(false).color(color).addAll(points).useGradient(true).width(5).visible(true);
                polyline5 = aMap.addPolyline(options);
                break;
        }

        Log.d(TAG, "drawLines:  type = " + type + " color = " + color);
        updateMap(points);
    }

    private void updateMap(List<LatLng> points) {
        // 获取轨迹坐标点
        LatLngBounds.Builder b = LatLngBounds.builder();

        for (int i = 0; i < points.size(); i++) {
            b.include(points.get(i));
        }

        LatLngBounds bounds = b.build();

        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, 100);//
        aMap.animateCamera(update);
    }

    private LatLngBounds getBounds(List<LatLng> pointlist) {
        LatLngBounds.Builder b = LatLngBounds.builder();
        if (pointlist == null) {
            return b.build();
        }
        for (int i = 0; i < pointlist.size(); i++) {
            b.include(pointlist.get(i));
        }
        return b.build();

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Message msg = mHandle.obtainMessage();
        msg.what = MAP;
        Bundle bundle = new Bundle();
        switch (checkedId) {
            case R.id.rbt0:
                bundle.putInt("type", 0);
                msg.setData(bundle);
                mHandle.sendMessage(msg);
                break;
            case R.id.rbt1:
                Log.d(TAG, "onCheckedChanged: ===> 1");
                bundle.putInt("type", 1);
                msg.setData(bundle);
                mHandle.sendMessage(msg);
                break;
            case R.id.rbt2:
                Log.d(TAG, "onCheckedChanged: ===> 2");
                bundle.putInt("type", 2);
                msg.setData(bundle);
                mHandle.sendMessage(msg);
                break;
            case R.id.rbt4:

                Log.d(TAG, "onCheckedChanged: ===> 4 points4:" + points4.toString());
                bundle.putInt("type", 4);
                msg.setData(bundle);
                mHandle.sendMessage(msg);
                break;
            case R.id.rbt5:

                Log.d(TAG, "onCheckedChanged: ===> 5");
                bundle.putInt("type", 5);
                msg.setData(bundle);
                mHandle.sendMessage(msg);
                break;
        }
    }

    class MyHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TXUPDATE:
                    String str = msg.getData().getString("str");
                    mTv.setText(str);
                    break;
                case MAP:
                    int type = msg.getData().getInt("type");
                    if (type == 0) {
                        mkalmanPolyline.setVisible(!mkalmanPolyline.isVisible());
                        mOriginPolyline.setVisible(!mOriginPolyline.isVisible());

                        if (polyline1 != null) polyline1.setVisible(false);
                        if (polyline2 != null) polyline2.setVisible(false);
                        if (polyline4 != null) polyline4.setVisible(false);
                        if (polyline5 != null) polyline5.setVisible(false);
                    }
                    if (type == 1) {
                        if (polyline1 != null) polyline1.setVisible(true);
                        if (polyline2 != null) polyline2.setVisible(false);
                        if (polyline4 != null) polyline4.setVisible(false);
                        if (polyline5 != null) polyline5.setVisible(false);
                        updateMap(points1);
                    } else if (type == 2) {
                        if (polyline2 != null) polyline2.setVisible(true);
                        if (polyline1 != null) polyline1.setVisible(false);
                        if (polyline4 != null) polyline4.setVisible(false);
                        if (polyline5 != null) polyline5.setVisible(false);
                        updateMap(points2);
                    } else if (type == 4) {
                        if (polyline1 != null) polyline1.setVisible(false);
                        if (polyline2 != null) polyline2.setVisible(false);
                        if (polyline4 != null) polyline4.setVisible(true);
                        if (polyline5 != null) polyline5.setVisible(false);
                        updateMap(points4);
                    } else if (type == 5) {
                        if (polyline1 != null) polyline1.setVisible(false);
                        if (polyline2 != null) polyline2.setVisible(false);
                        if (polyline4 != null) polyline4.setVisible(false);
                        if (polyline5 != null) polyline5.setVisible(true);
                        updateMap(points5);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void checkPermission() {
        int targetSdkVersion = 0;
        String[] PermissionString = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};
        try {
            final PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;//获取应用的Target版本
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
//            Log.e("err", "检查权限_err0");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Build.VERSION.SDK_INT是获取当前手机版本 Build.VERSION_CODES.M为6.0系统
            //如果系统>=6.0
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                //第 1 步: 检查是否有相应的权限
                boolean isAllGranted = checkPermissionAllGranted(PermissionString);
                if (isAllGranted) {
                    //Log.e("err","所有权限已经授权！");
                    return;
                }
                // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
                ActivityCompat.requestPermissions(this,
                        PermissionString, 1);
            }
        }
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                //Log.e("err","权限"+permission+"没有授权");
                return false;
            }
        }
        return true;
    }

    //申请权限结果返回处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean isAllGranted = true;
            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                // 所有的权限都授予了
                Log.e("err", "权限都授权了");
            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                //容易判断错
                //MyDialog("提示", "某些权限未开启,请手动开启", 1) ;
            }
        }
    }

}