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
