package com.kong.bjxyqxun.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.kong.bjxyqxun.service.KeepLiveService;
import com.kong.bjxyqxun.service.LocationService;
import com.kong.bjxyqxun.util.Utils;

public class MyApplication extends Application {
    private static Context instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = getApplicationContext();

        //设置前台Service，提升App进程优先级
        startService(new Intent(this, KeepLiveService.class));
//        startService(new Intent(this, LocationService.class));


    }

    public static Context getContext() {
        return instance;
    }

}