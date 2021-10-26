package com.kong.bjxyqxun.util;

import android.util.Log;

import com.amap.api.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static List<LatLng> readTxt() {
        List<LatLng> list = new ArrayList<>();

        try {
            /* 读入TXT文件 */
            String pathname = "/storage/emulated/0/Pictures/test.txt"; // 绝对路径或相对路径都可以，这里是绝对路径，写入文件时演示相对路径
            File filename = new File(pathname); // 要读取以上路径的input。txt文件
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(filename)); // 建立一个输入流对象reader
            BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
            String line = "";
            line = br.readLine();
            while (line != null) {
                line = br.readLine(); // 一次读入一行数据
                String[] sts = line.trim().split(",");
//                Log.d("kly1", "readTxt: "+sts[0]+" sts[1]="+sts[1]);
                LatLng latLng = new LatLng(Double.parseDouble(sts[0]), Double.parseDouble(sts[1]));
                list.add(latLng);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 解析纬度
     *
     * @param lat
     * @return
     */
    public static String parseLat(String lat, String type) {
        //纬度
        double latitude = Double.parseDouble(lat.substring(0, 2));
        latitude += Double.parseDouble(lat.substring(2)) / 60;
        if ("N".equals(type)) { //北纬
            return String.valueOf(latitude);
        } else { //南纬
            return "-" + String.valueOf(latitude);
        }
    }

    /**
     * 解析经度
     *
     * @param lon
     * @return
     */
    public static String parseLon(String lon, String type) {
        //经度
        double longitude = Double.parseDouble(lon.substring(0, 3));
        longitude += Double.parseDouble(lon.substring(3)) / 60;
        if ("E".equals(type)) {  //东经
            return String.valueOf(longitude);
        } else {  //西经
            return "-" + String.valueOf(longitude);
        }
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
     *
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
//            String hv = Integer.toHexString(v);// 字母小写
            String hv = Integer.toHexString(v).toUpperCase();
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    //将指定byte数组以16进制的形式打印到控制台
    public static void printHexString(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            System.out.print(hex.toUpperCase());
        }
    }

}
