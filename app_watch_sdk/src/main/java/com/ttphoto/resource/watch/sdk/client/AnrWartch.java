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

import android.content.Context;
import android.os.Build;

import com.qiyi.xhook.XHook;

public class AnrWartch {

    /**
     * 定义ANR信息收集方式
     * 1. OUTPUT_REDIRECT
     *    用重定向的方式收集ANR trace信息， 系统ANR将收集不到我们的trace
     *    该方式需要hook的方法少，高效、稳定
     * 2. OUTPUT_COPY
     *    用复制的方式收集ANR trace信息， 系统ANR收集不受影响
     *    该方式需要hook更多的方法
     */
    public static int OUTPUT_REDIRECT = 0;
    public static int OUTPUT_COPY = 1;

    public static void init(Context context, String outputPath, int ouput_mode) {
        // 初始化xhook， 为dump traces做准备
        if (XHook.getInstance().init(context)) {
            try {
                System.loadLibrary("appWatch");
                installHooks(Build.VERSION.SDK_INT, outputPath, ouput_mode);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private native static void installHooks(int sdkVersion, String outputPath, int outputMode);
}
