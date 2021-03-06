// MIT License
//
// Copyright (c) 2019 larrinluo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// Created by larrin luo on 2020-01-18.
//
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

import com.ttphoto.resource.watch.sdk.IAppWatchClient;
import com.ttphoto.resource.watch.sdk.IAppWatchService;
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchService;
import com.ttphoto.resource.watch.sdk.utils.FileUtil;
import com.ttphoto.resource.watch.sdk.utils.Utils;

// Run in application process
public class AppWatchClient {

    private static IAppWatchService service;
    private static Context sApplicationContext;

    /**
     * Start App resource watch. call this function in Application.onCreate
     *
     * @param context
     */
    public static void start(Context context, String deadlockSo) {
        Intent intent = new Intent();
        intent.setClass(context, AppResourceWatchService.class);
        context.bindService(intent, sConnection, Context.BIND_AUTO_CREATE);

        sApplicationContext = context.getApplicationContext();

        String traceFile = String.format("/sdcard/app_watch/%d/traces.txt", Process.myPid());

        if (WatchSDK.init(context)) {
            WatchSDK.enableANRWatch(traceFile, WatchSDK.ANR_OUTPUT_COPY);

            if (deadlockSo != null) {
                WatchSDK.enableDeadLockWatch(deadlockSo, "");
            }

            WatchSDK.start();
        }

        JavaExceptionWatch.init();
    }

    public static void startMainLooper(int anrWarningTime, Looper looper) {

        if (!Utils.isMainProcess())
            return;

        if (anrWarningTime < 0) {
            Log.w("AppWatchClient", "startWartchMainLooper warning not started cause startWartchMainLooper < 0: " + anrWarningTime);
            return;
        }

        if (anrWarningTime > 10000) {
            Log.w("AppWatchClient", "startWartchMainLooper adjust anrWarningTime to 10000 sec : " + anrWarningTime);
            anrWarningTime = 10000;
        }

        LooperWatch.startWatch(looper, anrWarningTime, new LooperWatch.Listener() {

            @Override
            public void onAnrWarning(int message, long delay, long timeout) {

                // 首先在本进程完成trace dump, 然后通知service
                if (service != null) {
                    try {
                        service.onAnr();
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

    public static void onUnhandledException(String exceptionMessage) {
        // service通过FileObserver监控exception, 现在的状态下， 已经不时候做过多事情
        String exceptionFile = String.format("/sdcard/app_watch/%d/exception.txt", Process.myPid());
        FileUtil.writeStringToFile(exceptionFile, exceptionMessage);
    }

    //  implementation here
    private static IAppWatchClient.Stub sBinder = new IAppWatchClient.Stub() {

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
        }
    };

    private static ServiceConnection sConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IAppWatchService.Stub.asInterface(binder);
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
