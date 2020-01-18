package com.ttphoto.resource.watch.sdk.client;

import android.os.Handler;
import android.os.HandlerThread;

public class WatchDog {

    private static HandlerThread handlerThread;
    private static Handler handler;

    public static Handler handler() {

        synchronized (WatchDog.class) {
            if (handler == null) {
                handlerThread = new HandlerThread("WatchThread");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
            }
        }

        return handler;
    }
}
