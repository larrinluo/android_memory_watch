package com.ttphoto.resource.watch.sdk;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Looper健康， 用途:
 * 1. 监控耗时的Msg
 * 2. 监控可能的ANR，并即使捕获trace
 */
public class LooperWatch {

    /**
     * 开始对looper进行健康
     * @param looper
     */
    public static void startWatch(final Looper looper) {
        Handler handler = new Handler(looper);
        handler.post(new Runnable() {
            @Override
            public void run() {

                MessageQueue queue = null;
                Method nextMethod = null;
                Method recycleUnchecked = null;
                try {
                    Field queueField = Looper.class.getDeclaredField("mQueue");
                    nextMethod = MessageQueue.class.getDeclaredMethod("next");
                    recycleUnchecked = Message.class.getDeclaredMethod("recycleUnchecked");
                    queueField.setAccessible(true);
                    nextMethod.setAccessible(true);
                    recycleUnchecked.setAccessible(true);
                    queue = (MessageQueue) queueField.get(looper);

                } catch (Exception e){
                    e.printStackTrace();
                }

                if (queue == null || nextMethod == null)
                    return;

                Binder.clearCallingIdentity();
                final long ident = Binder.clearCallingIdentity();

                // 进入监控循环
                for (;;) {

                    Message msg = null;
                    try {
                        msg = (Message) nextMethod.invoke(queue); // might block
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (msg == null) { // exit watch loop
                        return;
                    }

                    Log.d("WATCH_LOOP", "new message + " + msg.what);
                    msg.getTarget().dispatchMessage(msg);

                    // Make sure that during the course of dispatching the
                    // identity of the thread wasn't corrupted.
                    final long newIdent = Binder.clearCallingIdentity();
                    if (ident != newIdent) {
                        Log.wtf("LooperWatch", "Thread identity changed from 0x"
                                + Long.toHexString(ident) + " to 0x"
                                + Long.toHexString(newIdent) + " while dispatching to "
                                + msg.getTarget().getClass().getName() + " "
                                + msg.getCallback() + " what=" + msg.what);
                    }

                    try {
                        recycleUnchecked.invoke(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}

