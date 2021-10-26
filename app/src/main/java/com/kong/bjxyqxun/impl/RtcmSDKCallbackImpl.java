package com.kong.bjxyqxun.impl;

import android.util.Log;

import com.kong.bjxyqxun.callback.RtcmSDKCallbackListen;
import com.kong.bjxyqxun.util.DigitalTrans;
import com.kong.bjxyqxun.util.MyLog;
import com.kong.bjxyqxun.util.Utils;
import com.qxwz.sdk.core.CapInfo;
import com.qxwz.sdk.core.Constants;
import com.qxwz.sdk.core.IRtcmSDKCallback;
import com.qxwz.sdk.core.RtcmSDKManager;

import java.util.List;

import static com.qxwz.sdk.core.Constants.QXWZ_SDK_CAP_ID_NOSR;
import static com.qxwz.sdk.core.Constants.QXWZ_SDK_STAT_AUTH_SUCC;

public class RtcmSDKCallbackImpl implements IRtcmSDKCallback {
    private static final String TAG = "RtRcmCallbackImpl";
    private RtcmSDKCallbackListen mRtcmSDKCallbackListen;

    public void setSerialListen(RtcmSDKCallbackListen vRtcmSDKCallbackListen) {
        mRtcmSDKCallbackListen = vRtcmSDKCallbackListen;
    }

    @Override
    public void onData(int dataType, byte[] bytes) {
        Log.d(TAG, "onData, dataType:" + dataType + ", len:" + bytes.length);
        MyLog.d(TAG, "rtcm data len=" + bytes.length + " " + DigitalTrans.byte2HexStr(bytes));
        mRtcmSDKCallbackListen.onData(dataType, bytes);
    }

    @Override
    public void onStatus(int status) {
        Log.d(TAG, "status changed to " + status);
    }


    @Override
    public void onAuth(int code, List<CapInfo> caps) {
        if (code == QXWZ_SDK_STAT_AUTH_SUCC) {
            MyLog.d(TAG, "鉴权成功！");
            for (CapInfo capInfo : caps) {
                Log.d(TAG, "capInfo:" + capInfo.toString());
            }
            /* if you want to call the start api in the callback function, you must invoke it in a new thread. */
            // 鉴权成功后可启动能⼒获取相应服务，能⼒启动接⼝是异步⽅法，能⼒启动结束后通过调⽤初始化时传
            // ⼊的回调接⼝实例中的onStart⽅法通知⽤户启动结果，
            new Thread() {
                public void run() {
                    // 3. 开启启动能力
                    RtcmSDKManager.getInstance().start(QXWZ_SDK_CAP_ID_NOSR);
                }
            }.start();
        } else {
            Log.d(TAG, "failed to auth, code is " + code);
        }
    }

    @Override
    public void onStart(int code, int capId) {
        if (code == Constants.QXWZ_SDK_STAT_CAP_START_SUCC) {
            MyLog.d(TAG, "启动能⼒成功！");
            // 4. 开启上传GGA 数据
//            isStart = true;
            mRtcmSDKCallbackListen.isStart(true);
        } else {
            Log.d(TAG, "failed to start, code is " + code);
        }
    }
}
