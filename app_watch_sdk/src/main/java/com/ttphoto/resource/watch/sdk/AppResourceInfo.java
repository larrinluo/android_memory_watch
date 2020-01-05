package com.ttphoto.resource.watch.sdk;

import android.content.Context;
import android.support.annotation.NonNull;

public class AppResourceInfo {

    public int pid;
    public long timestamp;
    public MemoryInfo memoryInfo;
    public ProcessInfo processInfo;

    public static AppResourceInfo dump(Context context, int pid) {

        AppResourceInfo resourceInfo = new AppResourceInfo();
        resourceInfo.pid = pid;
        resourceInfo.timestamp = System.currentTimeMillis();
        resourceInfo.processInfo = ProcessInfo.dump(pid);
        if (resourceInfo.processInfo.running) {
            resourceInfo.memoryInfo = MemoryInfo.dump(context, pid);
        }

        return resourceInfo;
    }

    @NonNull
    @Override
    public String toString() {

        if (!processInfo.running) {
            return "Process " + pid + " exists";
        }

        return String.format("%d\t:%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",
            pid,
            memoryInfo.mPss,
            memoryInfo.mJavaHeap,
            memoryInfo.mNativeHeap,
            memoryInfo.mGraphics,
            memoryInfo.mPrivateOther,
            memoryInfo.mOtherDev,
            memoryInfo.mGfxDev,
            memoryInfo.mEGLMTrack,
            memoryInfo.mGLMTrack,
            memoryInfo.mStack,
            processInfo.vss,
            processInfo.threads,
            processInfo.openFiles);
    }
}
