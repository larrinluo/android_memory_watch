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
// Created by larrin luo on 2020-01-04.
//
package com.ttphoto.resource.watch.sdk;

import android.content.Context;

import com.ttphoto.resource.watch.sdk.client.performance.MemoryInfo;
import com.ttphoto.resource.watch.sdk.client.performance.ProcessInfo;

public class AppPerformanceInfo {

    public int pid;
    public long timestamp;
    public MemoryInfo memoryInfo;
    public ProcessInfo processInfo;

    public static AppPerformanceInfo dump(Context context, int pid) {

        AppPerformanceInfo resourceInfo = new AppPerformanceInfo();
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
            "PSS",
            "Java",
            "Native",
            "Graphics",
            "Other",
            "OtherDev",
            "GfxDev",
            "EGLMTrack",
            "GLMTrack",
            "Stack",
            "Vss",
            "Threads",
            "Fds",
            "Cpu",
            "myCpu"
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
