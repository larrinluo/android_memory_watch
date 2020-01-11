package com.ttphoto.resource.watch.sdk;

import android.content.Context;
import android.support.annotation.NonNull;

import com.ttphoto.resource.watch.sdk.resources.MemoryInfo;
import com.ttphoto.resource.watch.sdk.resources.ProcessInfo;

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

    public static String[] getCSVHeaders() {
        return new String[] {
            "Pss",
            "Java",
            "Native",
            "Graphics",
            "Other",
            "GfxDev",
            "EGLMTrack",
            "GLMTrack",
            "Stack",
            "Vss",
            "Threads",
            "Fds",
            "CPU",
            "MyCpu"
        };
    }

    public static String getCSVHeaderString() {
        StringBuilder builder = new StringBuilder();
        String[] headers = getCSVHeaders();
        for (int i = 0; i < headers.length; i++) {
            if (i > 0)
                builder.append("\t");

            builder.append(headers[i]);
        }

        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {

        if (!processInfo.running) {
            return "Process " + pid + " exists";
        }

        return String.format("%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.1f\t%.1f",
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
            processInfo.openFiles,
            processInfo.totalCpu,
            processInfo.myCpu);
    }
}
