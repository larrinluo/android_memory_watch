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
