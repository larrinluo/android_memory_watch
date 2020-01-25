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
package com.ttphoto.resource.watch.sdk.client.performance;

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
