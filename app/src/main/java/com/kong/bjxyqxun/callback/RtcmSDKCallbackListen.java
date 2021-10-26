package com.kong.bjxyqxun.callback;

public interface RtcmSDKCallbackListen {
    void onData( int type, byte[] bytes);
    void isStart(boolean isStart);
}
