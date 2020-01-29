package com.ttphoto.resource.watch.sdk.client;

import android.content.Context;
import android.os.Build;

import com.qiyi.xhook.XHook;

public class WatchSDK {

    /**
     * 定义ANR信息收集方式
     * 1. OUTPUT_REDIRECT
     *    用重定向的方式收集ANR trace信息， 系统ANR将收集不到我们的trace
     *    该方式需要hook的方法少，高效、稳定
     * 2. OUTPUT_COPY
     *    用复制的方式收集ANR trace信息， 系统ANR收集不受影响
     *    该方式需要hook更多的方法
     */
    public static int ANR_OUTPUT_REDIRECT = 0;
    public static int ANR_OUTPUT_COPY = 1;

    public static boolean init(Context context) {
        if (XHook.getInstance().init(context)) {
            try {
                System.loadLibrary("appWatch");
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static void enableANRWatch(String outputPath, int ouput_mode) {
        enableANRWatch(Build.VERSION.SDK_INT, outputPath, ouput_mode);
    }

    public static void enableDeadLockWatch(String targetSo, String outputPath) {
        enableDeadLockWatch(Build.VERSION.SDK_INT, targetSo, outputPath);
    }

    public static void start() {
        startWatch();
    }

    private native static void enableANRWatch(int sdkVersion, String outputPath, int outputMode);
    private native static void enableDeadLockWatch(int sdkVersion, String targetSo, String outputPath);
    private native static void startWatch();

}
