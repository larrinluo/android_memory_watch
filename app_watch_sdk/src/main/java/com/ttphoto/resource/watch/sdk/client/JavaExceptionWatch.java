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
// Created by larrin luo on 2020-01-22.
//
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
