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
// Created by larrin luo on 2020-01-05.
//
package com.ttphoto.resource.watch.sdk.utils;

import android.os.Process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPOutputStream;

public class Utils {

    public static void closeSilently(Closeable it) {
        if (it != null) {
            try {
                it.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void waitScilently(Object obj, int timeout) {
        try {
            obj.wait(timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readProcFile(String filePath, int pid) {
        BufferedReader reader = null;
        try {
            String path = String.format("/proc/%d/%s", pid, filePath);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "iso-8859-1"));
            StringBuilder processName = new StringBuilder();

            int c;
            while ((c = reader.read()) > 0) {
                processName.append((char) c);
            }

            return processName.toString();

        } catch (Exception e) {
        } finally {
            closeSilently(reader);
        }

        return null;
    }

    public static String getProcessName() {
        String cmdline = readProcFile("cmdline", Process.myPid());
        return cmdline;
    }

    public static String getAndroidProcessName() {
        String processName = getProcessName();
        if (processName == null)
            return null;

        int idx = processName.indexOf(":");
        if (idx == -1 || idx == processName.length() - 1)
            return null;

        return processName.substring(idx + 1);
    }

    public static boolean isMainProcess() {
        return getAndroidProcessName() == null;
    }

    public static boolean isWatchProcess(String processName) {
        if (processName == null)
            return false;

        return processName.equals(getAndroidProcessName());
    }

    public static byte[] gzip(byte[] input) throws Exception {
        GZIPOutputStream gzipOS = null;
        ByteArrayOutputStream byteArrayOS = null;
        try {
            byteArrayOS = new ByteArrayOutputStream();
            gzipOS = new GZIPOutputStream(byteArrayOS);
            gzipOS.write(input);
            gzipOS.flush();
            gzipOS.close();
            gzipOS = null;
            return byteArrayOS.toByteArray();
        } finally {
            Utils.closeSilently(byteArrayOS);
            Utils.closeSilently(gzipOS);
        }
    }
}
