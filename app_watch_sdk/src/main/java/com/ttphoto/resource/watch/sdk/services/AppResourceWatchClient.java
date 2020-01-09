package com.ttphoto.resource.watch.sdk.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;

import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient;
import com.ttphoto.resource.watch.sdk.IAppResourceWatchService;

public class AppResourceWatchClient {

    /**
     * Start App resource watch. call this function in Application.onCreate
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, AppResourceWatchService.class);
        context.bindService(intent, sConnection, Context.BIND_AUTO_CREATE);
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
    };

    private static ServiceConnection sConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            IAppResourceWatchService service = IAppResourceWatchService.Stub.asInterface(binder);
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
