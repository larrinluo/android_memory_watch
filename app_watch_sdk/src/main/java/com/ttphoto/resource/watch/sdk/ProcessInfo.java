package com.ttphoto.resource.watch.sdk;

import android.os.Process;

import java.io.File;

public class ProcessInfo {

    public int threads;         // 线程数量
    public int openFiles;       // 文件句柄数量

    public static ProcessInfo dump() {
        ProcessInfo processInfo = new ProcessInfo();
        processInfo.openFiles = getSubFileCountUnderProcFolder("fd");
        processInfo.threads = getSubFileCountUnderProcFolder("task");
        return processInfo;
    }

    public static void dumpFileDetail() {
    }

    public static void dumpThreadDetail() {
    }

    private static int getSubFileCountUnderProcFolder(String folder) {
        String fdPath = String.format("/proc/%d/%s", Process.myPid(), folder);
        File dir = new File(fdPath);
        if (dir.isDirectory()) {
            String[] names = dir.list();
            return names == null ? 0 : names.length;
        }

        return 0;
    }

}
