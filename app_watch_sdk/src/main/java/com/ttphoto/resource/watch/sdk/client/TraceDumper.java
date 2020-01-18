package com.ttphoto.resource.watch.sdk.client;

import android.content.Context;
import android.os.Build;

import com.qiyi.xhook.XHook;

public class TraceDumper {

    public static void init(Context context) {
        // 初始化xhook， 为dump traces做准备
        if (XHook.getInstance().init(context)) {
            try {
                System.loadLibrary("appWatch");
                installHooks(Build.VERSION.SDK_INT);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private native static void installHooks(int sdkVersion);
    public native static void setTracePath(String traceFile);
}
