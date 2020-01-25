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
// Created by larrin on 20-1-22.
//

#ifndef ANDROID_MEMORY_WATCH_APP_WATCH_SDK_H
#define ANDROID_MEMORY_WATCH_APP_WATCH_SDK_H

#include <jni.h>

class JNI {

    static JavaVM *jvm;

public:

    static jobject gClassLoader;
    static jmethodID gLoadClassMethod;

    static void init(JavaVM *jvm) {
        JNI::jvm = jvm;
    }

    static JavaVM * Jvm() {
        return JNI::jvm;
    }

    static jclass FindClass(JNIEnv *env, const char *classname);

};

#endif //ANDROID_MEMORY_WATCH_APP_WATCH_SDK_H
