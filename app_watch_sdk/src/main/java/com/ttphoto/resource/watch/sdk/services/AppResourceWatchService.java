package com.ttphoto.resource.watch.sdk.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ttphoto.resource.watch.sdk.performance.CPUInfo;
import com.ttphoto.resource.watch.sdk.IAppWatchClient;
import com.ttphoto.resource.watch.sdk.AppPerformanceInfo;
import com.ttphoto.resource.watch.sdk.IAppWatchService;
import com.ttphoto.resource.watch.sdk.IAppWatchCallback;
import com.ttphoto.resource.watch.sdk.utils.Utils;

/**
 * 资源监视服务，可以运行在独立进程中，具有以下优势：
 * 1. 从主进程中独立出来，不影响主进程的统计数据
 * 2. 监视进程功能单纯，运行可靠，即使主进程崩溃，统计数据也可以用安全收集
 *
 * 设计原则：
 * 1. 安全性： service只为主进程服务，export必须设为false
 * 2. 隔离性： service必须在独立进程中运行
 * 2. 单一性:  servide只监控一个进程
 */
public class AppResourceWatchService extends Service {

    ResourceWatcher watcher = null;

    private static IAppWatchCallback sCallback = new IAppWatchCallback() {

        @Override
        public void onAppStart(int pid) {
            Log.d("AppResource", String.format("process %d start", pid));
        }

        @Override
        public void onUpdate(AppPerformanceInfo info) {
            Log.d("AppResource", info.toString());
        }

        @Override
        public void onAppExist(int pid) {
            Log.d("AppResource", String.format("process %d exists", pid));
        }

        @Override
        public void onAnr(int pid) {
            Log.w("AppWatch", String.format("Anr warning: pid = %d"));
        }

        @Override
        public void onMessageSlow(int pid, int message, long delay, long dispatch) {
            Log.w("AppWatch", String.format("OnMessage Slow: pid = %d, msg = %d, delay = %d, timeout = %d",
                    pid, message, delay, dispatch));
        }
    };

    IBinder mService = new IAppWatchService.Stub() {

        IAppWatchClient client;

        @Override
        public void startWatch(long maxJavaHeap, IBinder watchClient) throws RemoteException {
            int pid = Binder.getCallingPid();
            if (Binder.getCallingUid() != myUid()) // 只为自己服务
                return;

            if (watcher != null && pid == watcher.mPid) // 重复绑定
                return;

            client = IAppWatchClient.Stub.asInterface(watchClient);
            if (pid == 0 || maxJavaHeap <= 16 * 1024 * 1024 || client == null) // invalidate request
                return;

            if (watcher != null) {
                watcher.stop();
            }

            watcher = new ResourceWatcher(pid, maxJavaHeap, client, AppResourceWatchService.this, sCallback);
            watcher.start();
        }

        @Override
        public void onAnr() throws RemoteException {
            int pid = Binder.getCallingPid();
            sCallback.onAnr(pid);
        }

        @Override
        public void onMessageSlow(int message, long delay, long dispatch) throws RemoteException {
            sCallback.onMessageSlow(Binder.getCallingPid(), message, delay, dispatch);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mService;
    }


    private int myUid() {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            return ai.uid;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void setWatchCallback(IAppWatchCallback callback) {
        sCallback = callback;
    }
}

class ResourceWatcher {
    int mPid;
    long mMaxJavaHeap;

    private IAppWatchClient mClient;
    private Context mContext;
    private long mStartTime;
    private boolean mRunning;
    private CPUInfo mCPUInfo;
    private IAppWatchCallback mCallback;

    private Thread mWatchThread = new Thread() {
        @Override
        public void run() {

            mStartTime = System.currentTimeMillis();
            mRunning = true;
            int count = 0;

            if (mCallback != null) {
                mCallback.onAppStart(mPid);
            }

            while (mRunning) {
                if (count >= 5) {
                    mCPUInfo.update(true);
                    AppPerformanceInfo resourceInfo = AppPerformanceInfo.dump(mContext, mPid);
                    resourceInfo.processInfo.totalCpu = mCPUInfo.getmTotalCpu();
                    resourceInfo.processInfo.myCpu = mCPUInfo.getmMyCpu();
                    if (!resourceInfo.processInfo.running) {
                        if (mCallback != null) {
                            mCallback.onAppExist(mPid);
                        }
                        break;
                    }

                    if (mCallback != null) {
                        mCallback.onUpdate(resourceInfo);
                    }

                    count = 0;
                } else {
                    if (count == 4)
                        mCPUInfo.update(false);
                    count++;
                }

                synchronized (this) {
                    Utils.waitScilently(this, 1000);
                }
            }
        }
    };

    ResourceWatcher(int pid, long maxJavaHeap, IAppWatchClient client, Context context, IAppWatchCallback callback) {
        mPid = pid;
        mMaxJavaHeap = maxJavaHeap;
        mClient = client;
        mContext = context;
        mCPUInfo = new CPUInfo(pid);
        mCallback = callback;
    }

    public void start() {
        mWatchThread.start();
    }

    public void stop() {
        mRunning = false;
        synchronized (mWatchThread) {
            mWatchThread.notifyAll();
        }
    }
}
