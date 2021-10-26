package com.kong.bjxyqxun.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amap.api.maps.model.LatLng;
import com.kong.bjxyqxun.callback.LatLngCallback;

public class GpsReceiver extends BroadcastReceiver {
    private static final String TAG = "MyReceiver";
    private LatLngCallback mLatLngCallback = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        int type=intent.getIntExtra("type",0);
        String num=intent.getStringExtra("num");
        LatLng latLng = intent.getParcelableExtra("LatLng");
        if (mLatLngCallback != null) {
            mLatLngCallback.getLatLng(type,num,latLng);
        }
    }

    public void setLatLngListen(LatLngCallback vLatLngCallback) {
        mLatLngCallback = vLatLngCallback;
    }

}