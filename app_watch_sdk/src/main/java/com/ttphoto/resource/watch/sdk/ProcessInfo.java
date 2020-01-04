package com.ttphoto.resource.watch.sdk;

import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

public class ProcessInfo {

    public int threads;         // 线程数量
    public int openFiles;       // 文件句柄数量
    public int vss;             // 虚存

    public static ProcessInfo dump() {
        ProcessInfo processInfo = new ProcessInfo();
        dumpProcessInfoFromStatus(processInfo);
        return processInfo;
    }

    private static void dumpProcessInfoFromStatus(ProcessInfo processInfo) {

        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            String path = String.format("/proc/%d/status", Process.myPid());
            fileReader = new FileReader(path);
            reader = new BufferedReader(fileReader);
            String line;
            int targets = 3;
            while (targets > 0 && (line= reader.readLine()) != null) {

                if (line.startsWith("FDSize:")) {
                    String subStr = line.substring(7).trim();
                    processInfo.openFiles = Integer.parseInt(subStr);
                    targets--;
                } else if (line.startsWith("Threads:")) {
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

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void dumpFileDetail() {
    }

    public static void dumpThreadDetail() {
    }

    private static String readProcFile(String filePath) {
        FileInputStream inputStream = null;
        try {
            String path = String.format("/proc/%d/%s", Process.myPid(), filePath);
            File file = new File(path);

            if (file.exists()) {
                inputStream = new FileInputStream(file);
                int bytes = 0;
                byte[] buffer = new byte[1024]; //无法预先获取文件长度，动态调整buffer大小
                while (true) {
                    int b = inputStream.read(buffer, bytes, buffer.length - bytes);
                    if (b <= 0)
                        break;

                    bytes += b;
                    if (bytes == buffer.length) { //need expend buffer
                        byte[] newBuffer = new byte[buffer.length + 1024];
                        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                        buffer = newBuffer;
                    }
                }

                if (bytes > 0) {
                    return new String(buffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream !=  null) {
                try {
                    inputStream.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

        return null;
    }

    private static int getFileCountUnderProcFolder(String folder) {
        String fdPath = String.format("/proc/%d/%s", Process.myPid(), folder);
        File dir = new File(fdPath);
        if (dir.isDirectory()) {
            String[] names = dir.list();
            return names == null ? 0 : names.length;
        }

        return 0;
    }

}
