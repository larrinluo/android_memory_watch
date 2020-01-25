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
// Created by larrin luo on 2020-01-18.
//
package com.ttphoto.resource.watch.report.utils;

import android.os.FileObserver;
import android.os.Process;
import android.support.annotation.Nullable;

import com.ttphoto.resource.watch.sdk.IAppWatchClient;

public class DumpUtil {

    static class TraceFileObserver extends FileObserver {

        boolean done = false;
        final static int MASK = FileObserver.CLOSE_WRITE | FileObserver.CLOSE_NOWRITE;
        public TraceFileObserver(String path) {
            super(path, MASK);
        }

        @Override
        public void onEvent(int event, @Nullable String path) {
            if ((event & MASK) != 0) {
                done = true;
            }

            synchronized (this) {
                notifyAll();
            }
        }

        public void waitComplete() {
            synchronized (this) {
                try {
                    this.wait(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            stopWatching();
        }
    }

    /**
     * Dump Trace信息到指定文件中
     * @param pid
     * @param timeout
     * @param traceFile
     * @return
     */
    public static boolean dumpTrace(IAppWatchClient client, int pid, long timeout, String traceFile) {

        try {
            client.dumpTrace(traceFile);
            TraceFileObserver observer = new TraceFileObserver(traceFile);
            observer.startWatching();
            Process.sendSignal(pid, Process.SIGNAL_QUIT);
            observer.waitComplete();
            return observer.done;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
