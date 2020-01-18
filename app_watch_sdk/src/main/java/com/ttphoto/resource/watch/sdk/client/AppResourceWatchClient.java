package com.ttphoto.resource.watch.sdk.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Debug;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient;
import com.ttphoto.resource.watch.sdk.IAppResourceWatchService;
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchService;
import com.ttphoto.resource.watch.sdk.utils.Utils;

// Run in application process
public class AppResourceWatchClient {

    private static IAppResourceWatchService service;
    private static Context sApplicationContext;

    /**
     * Start App resource watch. call this function in Application.onCreate
     *
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, AppResourceWatchService.class);
        context.bindService(intent, sConnection, Context.BIND_AUTO_CREATE);

        sApplicationContext = context.getApplicationContext();
        TraceDumper.init(sApplicationContext);
    }

    public static void startWartchMainLooper(int anrWarningTime) {

        if (!Utils.isMainProcess())
            return;

        if (anrWarningTime < 0) {
            Log.w("AppResourceWatchClient", "startWartchMainLooper warning not started cause startWartchMainLooper < 0: " + anrWarningTime);
            return;
        }

        if (anrWarningTime > 10000) {
            Log.w("AppResourceWatchClient", "startWartchMainLooper adjust anrWarningTime to 10000 sec : " + anrWarningTime);
            anrWarningTime = 10000;
        }

        LooperWatch.startWatch(Looper.getMainLooper(), anrWarningTime, new LooperWatch.Listener() {

            @Override
            public void onAnrWarning(int message, long delay, long timeout) {

                // 首先在本进程完成trace dump, 然后通知service
                if (service != null) {
                    try {
                        String traceFile = String.format("/sdcard/app_watch/%d/traces.txt", Process.myPid());
                        TraceDumper.setTracePath(traceFile);
                        service.onAnrWarnning(message, delay, timeout, traceFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onMessageSlow(Looper looper, int message, long delay, long dispatch) {
                if (service != null) {
                    try {
                        service.onMessageSlow(message, delay, dispatch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    //  implementation here
    private static IAppResourceWatchClient.Stub sBinder = new IAppResourceWatchClient.Stub() {

        @Override
        public void dumpJavaHeap(String fileName) throws RemoteException {
            try {
                Debug.dumpHprofData(fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void dumpTrace(String traceFile) throws RemoteException {
            TraceDumper.setTracePath(traceFile);
        }
    };

    private static ServiceConnection sConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IAppResourceWatchService.Stub.asInterface(binder);
            if (service != null) {
                try {
                    service.startWatch(Runtime.getRuntime().maxMemory(), sBinder);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
}
