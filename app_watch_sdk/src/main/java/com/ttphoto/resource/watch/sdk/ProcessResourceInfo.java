package com.ttphoto.resource.watch.sdk;

import android.content.Context;
import android.support.annotation.NonNull;

public class ProcessResourceInfo {

    public int pid;
    public long timestamp;
    public MemoryInfo memoryInfo;
    public ProcessInfo processInfo;
    public long vss;                    //虚存

    public static ProcessResourceInfo dump(Context context, int pid) {
        ProcessResourceInfo resourceInfo = new ProcessResourceInfo();
        resourceInfo.pid = pid;
        resourceInfo.timestamp = System.currentTimeMillis();
        resourceInfo.memoryInfo = MemoryInfo.dump(context, pid);
        resourceInfo.processInfo = ProcessInfo.dump(pid);
        return resourceInfo;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",
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
