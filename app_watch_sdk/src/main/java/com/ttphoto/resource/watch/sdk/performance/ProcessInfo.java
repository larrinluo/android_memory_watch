package com.ttphoto.resource.watch.sdk.performance;

import com.ttphoto.resource.watch.sdk.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ProcessInfo {

    public boolean running;     // 是否还在运行
    public int threads;         // 线程数量
    public int openFiles;       // 文件句柄数量
    public int vss;             // 虚存
    public float totalCpu;      // 总cpu使用率
    public float myCpu;         //  进程cpu使用率

    public static ProcessInfo dump(int pid) {
        ProcessInfo processInfo = new ProcessInfo();
        dumpProcessInfoFromStatus(processInfo, pid);
        return processInfo;
    }

    // implemetation
    private static void dumpProcessInfoFromStatus(ProcessInfo processInfo, int pid) {

        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            String path = String.format("/proc/%d/status", pid);
            File file = new File(path);
            processInfo.running = file.exists();
            if (!processInfo.running) {
                return;
            }

            fileReader = new FileReader(path);
            reader = new BufferedReader(fileReader);
            String line;
            int targets = 2;
            while (targets > 0 && (line= reader.readLine()) != null) {

                if (line.startsWith("Threads:")) {
                    String subStr = line.substring(8).trim();
                    processInfo.threads = Integer.parseInt(subStr);
                    targets--;
                } else if (line.startsWith("VmSize:")) {
                    String subStr = line.substring(7).trim();
                    int lastIdx = subStr.indexOf(" ");
                    subStr = subStr.substring(0, lastIdx).trim();
                    processInfo.vss = Integer.parseInt(subStr);
                    targets--;
                }
            }

            File fdDir = new File(String.format("/proc/%d/fd", pid));
            String[] fds = fdDir.list();
            if (fds != null) {
                processInfo.openFiles = fds.length;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.closeSilently(fileReader);
            Utils.closeSilently(reader);
        }
    }

    public static void dumpFileDetail() {
    }

    public static void dumpThreadDetail() {
    }

}
