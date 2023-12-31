package com.vfi.smartpos.system_service.util;

import android.annotation.SuppressLint;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.util.regex.Pattern;

/**
 * Created by RYX on 2016/11/30.
 */

public class NetUtils {
    /*
     * convert subMask string to prefix length
     */
    public static int maskStr2InetMask(String maskStr) {
        StringBuffer sb;
        String str;
        int inetmask = 0;
        int count = 0;
        /*
         * check the subMask format
         */
        Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
        if (pattern.matcher(maskStr).matches() == false) {
            Log.e("33333", "subMask is error");
            return 0;
        }

        String[] ipSegment = maskStr.split("\\.");
        for (int n = 0; n < ipSegment.length; n++) {
            sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
            str = sb.reverse().toString();
            count = 0;
            for (int i = 0; i < str.length(); i++) {
                i = str.indexOf("1", i);
                if (i == -1)
                    break;
                count++;
            }
            inetmask += count;
        }
        return inetmask;
    }


    public static Inet4Address getIPv4Address(String text) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> threadClazz = Class.forName("android.net.NetworkUtils");
            Method method = threadClazz.getMethod("numericToInetAddress", String.class);
            return (Inet4Address) method.invoke(null, text);
        } catch (IllegalArgumentException | ClassCastException | ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}
