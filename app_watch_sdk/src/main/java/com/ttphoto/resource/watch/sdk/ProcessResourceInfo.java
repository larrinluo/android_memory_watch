package com.ttphoto.resource.watch.sdk;

import android.content.Context;
import android.support.annotation.NonNull;

public class ProcessResourceInfo {

    public long timestamp;
    public MemoryInfo memoryInfo;
    public ProcessInfo processInfo;
    public long vss;                    //虚存

    public static ProcessResourceInfo dump(Context context) {
        ProcessResourceInfo resourceInfo = new ProcessResourceInfo();
        resourceInfo.timestamp = System.currentTimeMillis();
        resourceInfo.memoryInfo = MemoryInfo.dump(context);
        resourceInfo.processInfo = ProcessInfo.dump();
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
