package com.ttphoto.resource.watch.sdk.client;

import android.content.Context;
import android.os.Build;

import com.qiyi.xhook.XHook;

public class AnrWartch {

    /**
     * 定义ANR信息收集方式
     * 1. OUTPUT_REDIRECT
     *    用重定向的方式收集ANR trace信息， 系统ANR将收集不到我们的trace
     *    该方式需要hook的方法少，高效、稳定
     * 2. OUTPUT_COPY
     *    用复制的方式收集ANR trace信息， 系统ANR收集不受影响
     *    该方式需要hook更多的方法
     */
    public static int OUTPUT_REDIRECT = 0;
    public static int OUTPUT_COPY = 1;

    public static void init(Context context, String outputPath, int ouput_mode) {
        // 初始化xhook， 为dump traces做准备
        if (XHook.getInstance().init(context)) {
            try {
                System.loadLibrary("appWatch");
                installHooks(Build.VERSION.SDK_INT, outputPath, ouput_mode);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private native static void installHooks(int sdkVersion, String outputPath, int outputMode);
}
