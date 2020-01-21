package com.ttphoto.resource.watch.sdk.client;

import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.ttphoto.resource.watch.sdk.utils.FileUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Looper健康， 用途:
 * 1. 监控耗时的Msg
 * 2. 监控可能的ANR，并即使捕获trace
 */
public class LooperWatch {

    public interface Listener {

        /**
         * @param message
         * @param delay
         * @param timeout
         */
        void onAnrWarning(int message, long delay, long timeout);

        /**
         * Message处理慢的监控回调
         *
         * @param  looper
         * @param  message
         * @param  delay    deliver delay
         * @param  dispatch dispatch time
         */
        void onMessageSlow(final Looper looper, int message, long delay, long dispatch);
    }

    final static long DISPATCH_SLOW = 1000;

    /**
     * 开始对looper进行健康
     * @param looper
     * @param anrWarningTime ANR警报时间， 当一个消息的处理时间超过5秒时进行报警， 上层可以在这个时候获取trace文件
     * @param listener
     *
     */
    public static void startWatch(final Looper looper, final int anrWarningTime, final Listener listener) {

        Handler handler = new Handler(looper);
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (listener == null) { // 没有回调， 监控无意义
                    Log.w("LooperWatch", "LooperWatch should work with non null Listener!!");
                    return;
                }

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


                    Method[] methods = Debug.class.getDeclaredMethods();
                    for (Method method: methods) {
                        Log.d("METHOD_", method.getName());
                    }
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

                    final long dispatchStart = SystemClock.uptimeMillis();
                    final long dispatchEnd;

                    long delay = dispatchStart - msg.getWhen();

                    msg.getTarget().dispatchMessage(msg);

                    dispatchEnd = SystemClock.uptimeMillis();
                    long duration = dispatchEnd - dispatchStart;

                    if (duration > DISPATCH_SLOW) {
                        listener.onMessageSlow(looper, msg.what, delay, duration);
                    }

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

