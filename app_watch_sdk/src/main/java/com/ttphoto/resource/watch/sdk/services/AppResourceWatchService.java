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

import com.ttphoto.resource.watch.sdk.CPUInfo;
import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient;
import com.ttphoto.resource.watch.sdk.AppResourceInfo;
import com.ttphoto.resource.watch.sdk.IAppResourceWatchService;
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

    IBinder mService = new IAppResourceWatchService.Stub() {

        @Override
        public void startWatch(long maxJavaHeap, IBinder watchClient) throws RemoteException {
            int pid = Binder.getCallingPid();
            if (Binder.getCallingUid() != myUid()) // 只为自己服务
                return;

            if (watcher != null && pid == watcher.mPid) // 重复绑定
                return;

            IAppResourceWatchClient client = IAppResourceWatchClient.Stub.asInterface(watchClient);
            if (pid == 0 || maxJavaHeap <= 16 * 1024 * 1024 || client == null) // invalidate request
                return;

            if (watcher != null) {
                watcher.stop();
            }

            watcher = new ResourceWatcher(pid, maxJavaHeap, client, AppResourceWatchService.this);
            watcher.start();
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
}

class ResourceWatcher {
    int mPid;
    long mMaxJavaHeap;

    private IAppResourceWatchClient mClient;
    private Context mContext;
    private long mStartTime;
    private boolean mRunning;
    private CPUInfo mCPUInfo;

    private Thread mWatchThread = new Thread() {
        @Override
        public void run() {

            mStartTime = System.currentTimeMillis();
            mRunning = true;
            int count = 0;

            while (mRunning) {
                if (count >= 5) {
                    mCPUInfo.update(true);
                    AppResourceInfo resourceInfo = AppResourceInfo.dump(mContext, mPid);
                    resourceInfo.processInfo.totalCpu = mCPUInfo.getmTotalCpu();
                    resourceInfo.processInfo.myCpu = mCPUInfo.getmMyCpu();
                    if (!resourceInfo.processInfo.running) {
                        Log.d("AppResource", String.format("process %d exists", mPid));
                        break;
                    }
                    Log.d("AppResource", resourceInfo.toString());
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

    ResourceWatcher(int pid, long maxJavaHeap, IAppResourceWatchClient client, Context context) {
        mPid = pid;
        mMaxJavaHeap = maxJavaHeap;
        mClient = client;
        mContext = context;
        mCPUInfo = new CPUInfo(pid);
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
