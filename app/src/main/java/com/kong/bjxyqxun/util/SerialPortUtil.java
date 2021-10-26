package com.kong.bjxyqxun.util;

import android.os.SystemClock;
import android.util.Log;

import com.kong.bjxyqxun.callback.SerialListen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SerialPortUtil {

    private static final String TAG = "kly";
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private ReceiveThread mReceiveThread = null;
    private boolean isStart = false;

    private SerialListen mSerialListen = null;

    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * 打开串口，接收数据
     * 通过串口，接收R21发送来的数据
     */
    public void openSerialPort(String path, int baudrate, int flags) {
        // 串口通讯
        SerialPortFinder mSerialPortFinder = new SerialPortFinder();
        // 得到所有设备文件地址的数组
        // 实际上该操作并不需要，这里只是示例打印出所有的设备信息
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();

        try {
            SerialPort serialPort = new SerialPort(new File(path), baudrate, flags);
            //调用对象SerialPort方法，获取串口中"读和写"的数据流
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isStart = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        getSerialPort();
    }

    public void setSerialListen(SerialListen vSerialListen) {
        mSerialListen = vSerialListen;
    }

    /**
     * 关闭串口
     * 关闭串口中的输入输出流
     */
    public void closeSerialPort() {
        Log.i("test", "关闭串口");
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送数据
     * 通过串口，发送数据到单片机
     *
     * @param bytes 要发送的数据
     */
    public void sendSerialPort(byte[] bytes) {
        try {
            if (bytes.length > 0) {
                outputStream.write(bytes);// 写入数据
                outputStream.flush();// 清空缓存并输出流
                Log.d(TAG, "rtcm======>发送给串口成功 ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getSerialPort() {
        if (mReceiveThread == null) {
            mReceiveThread = new ReceiveThread();
        }
        try {
            mReceiveThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收串口数据的线程
     */
    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();
            //条件判断，只要条件为true，则一直执行这个线程
            byte[] buff = new byte[1024];
            StringBuffer gga = new StringBuffer();
            boolean flag = false;
            int count = 0;
            while (isStart) {
                try {
                    int size = inputStream.read(buff);
                    if (size > 0) {
                        final String message = new String(buff, 0, size);
                        Log.d(TAG, "received data =>" + message + "  换行符：" + message.indexOf("\r\n") +
                                "  size=" + size +
                                "  count=" + count);
                        //查询字符串起始位置
                        int start = message.indexOf("$GPGGA");
                        count++;
                        if (start != -1 && !flag) {
                            //如果以$GPGGA开头的并且是没有拼接过数据
                            flag = true; // 标记开始拼接

                            gga.append(message);
                        } else {// 不是以$GPGGA开头的
                            // 不是以$GPGGA开头的
                            int end = message.indexOf("\r\n");
                            if (flag && end != -1) {
                                // 有$非GPGGA标签 表示一个完整的GGA数据结束
                                String endMsg = message.substring(0, end);
                                gga.append(endMsg);//数据拼接完成
                                Log.d(TAG, "数据拼接完成: " + gga);
                                flag = false;
                                // 保存数据
                                if (mSerialListen != null) {
                                    mSerialListen.getSerial(new String(gga));
                                }
                                // 清空临时gga数据
                                gga.setLength(0);
                                count = 0;
                                SystemClock.sleep(200);
                                Log.d(TAG, "数据拼接完成清空: " + gga);
                            } else {
                                //没有换行符且falg=true表示拼接未完成 继续拼接
                                if (flag) gga.append(message);
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
