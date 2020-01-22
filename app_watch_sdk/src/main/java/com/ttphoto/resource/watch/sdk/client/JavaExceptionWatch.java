package com.ttphoto.resource.watch.sdk.client;

import android.os.Process;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JavaExceptionWatch {

    private static Thread.UncaughtExceptionHandler preUncaughtExceptionHandler;

    public static  void init() {
        preUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {

                StringBuilder builder = new StringBuilder();

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                builder.append("[").append(format.format(new Date())).append("] ");
                builder.append("Unhandled java exception, pid= ").append(Process.myPid())
                        .append(", Thread= (").append(t.getId()).append(", ").append(t.getName())
                        .append(")\n");
                builder.append("Exception: ").append(e).append("\n");
                builder.append("Exception message: ").append(e.getMessage()).append("\n");

                if (e.getCause() != null) {
                    builder.append("Exception root cause:");
                    builder.append(e.getCause().getMessage());
                }

                StackTraceElement[] traceElements = e.getStackTrace();
                if (traceElements != null) {
                    builder.append("Thread stack:\n");

                    for (StackTraceElement element: traceElements) {
                        builder.append("    ");
                        if (element.isNativeMethod())
                            builder.append("[N] ");
                        else
                            builder.append("[J] ");
                        builder.append(element.getClassName()).append(".")
                                .append(element.getMethodName()).append(" (")
                                .append(element.getFileName()).append(":")
                                .append(element.getLineNumber()).append(")\n");
                    }
                }


                AppWatchClient.onUnhandledException(builder.toString());

                if (preUncaughtExceptionHandler != null) {
                    preUncaughtExceptionHandler.uncaughtException(t, e);
                }
            }
        });
    }
}
