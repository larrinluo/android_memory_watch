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
// Created by larrin luo on 2020-01-30.
//

#include "jni_utils.h"
#include <jni.h>

JavaVM * JNI::jvm = NULL;
jobject JNI::gClassLoader = NULL;
jmethodID JNI::gLoadClassMethod = NULL;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;

    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    JNI::init(jvm);


    jclass clientClass = env->FindClass("com/ttphoto/resource/watch/sdk/client/AppWatchClient");
    jclass classClass = env->GetObjectClass(clientClass);
    jmethodID getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
                                                      "()Ljava/lang/ClassLoader;");
    jobject localClassLoader = env->CallObjectMethod(clientClass, getClassLoaderMethod);
    JNI::gClassLoader = env->NewGlobalRef(localClassLoader);

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    JNI::gLoadClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                             "(Ljava/lang/String;)Ljava/lang/Class;");

    result = JNI_VERSION_1_6;
    return result;
}

} // extern "C"

jclass JNI::FindClass(JNIEnv *env, const char *classname) {

    if (!env)
        return NULL;

    if (JNI::gClassLoader && JNI::gLoadClassMethod) {
        jclass  clz = (jclass) env->CallObjectMethod(JNI::gClassLoader, JNI::gLoadClassMethod, env->NewStringUTF(classname));
        return clz;
    }

    return NULL;
}


char* JNI::convert_jstring_to_char(JNIEnv* env, jstring jstr)
{
    char* rtn = NULL;
    if (NULL == jstr) {
        return rtn;
    }

    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char*) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    env->DeleteLocalRef(barr);
    env->DeleteLocalRef(clsstring);
    env->DeleteLocalRef(strencode);

    return rtn;
}

bool JNI::get_thread_env(JNIEnv ** env)
{
    if (jvm  != NULL ) {
        int ret = jvm->GetEnv((void **)env, JNI_VERSION_1_6);
        if ( ret == JNI_OK ) {
            return true;
        } else if ( ret == JNI_EDETACHED ) {
            jvm->AttachCurrentThread(env, 0);
            if ( (*env) != NULL ) {
                return false;
            } else {
                return true;
            }
        }
        else {
            (*env) = NULL;
            return true;
        }
    } else {
        return true;
    }
}

void JNI::get_java_callstack(std::string &callstack) {

    if (jvm == NULL) {
        return;
    }

    JNIEnv *env = NULL;
    bool is_java_thread = get_thread_env(&env);
    if (!is_java_thread) {
        jvm->DetachCurrentThread();
        return;
    }

    if (env == NULL) {
        return;
    }

    JClass threadClass(env, "java/lang/Thread");
    JClass stackTraceElementClass(env, "java/lang/StackTraceElement");

    if (!threadClass.get() || !stackTraceElementClass.get())
        return;

    jmethodID currentThreadMethod = env->GetStaticMethodID(threadClass.get(), "currentThread", "()Ljava/lang/Thread;");
    jmethodID getStackTrace = env->GetMethodID(threadClass.get(), "getStackTrace", "()[Ljava/lang/StackTraceElement;");
    jmethodID getClassName = env->GetMethodID(stackTraceElementClass.get(), "getClassName", "()Ljava/lang/String;");
    jmethodID getMethodName = env->GetMethodID(stackTraceElementClass.get(), "getMethodName", "()Ljava/lang/String;");
    jmethodID getFileName = env->GetMethodID(stackTraceElementClass.get(), "getFileName", "()Ljava/lang/String;");
    jmethodID getLineNumber = env->GetMethodID(stackTraceElementClass.get(), "getLineNumber", "()I");

    if (!currentThreadMethod || !getStackTrace || !getClassName || !getMethodName || !getFileName || !getLineNumber)
        return;

    JObject currentThread(env, env->CallStaticObjectMethod(threadClass.get(), currentThreadMethod));

    if (!currentThread.get())
        return;

    JObject stackTrace(env, env->CallObjectMethod(currentThread.get(), getStackTrace));
    if (!stackTrace.get())
        return;

    jobjectArray array = (jobjectArray) stackTrace.get();
    int s = env->GetArrayLength(array);
    int c = 0;
    char buf[10];
    for (int i = 0; i < s; i++) {

        JObject element(env, env->GetObjectArrayElement(array, i));

        if (element.get()) {

            JObject className(env, env->CallObjectMethod(element.get(), getClassName));
            JObject methodName(env, env->CallObjectMethod(element.get(), getMethodName));
            JObject fileName(env, env->CallObjectMethod(element.get(), getFileName));
            jint lineNumberStr = env->CallIntMethod(element.get(), getLineNumber);

            if (className.get() && methodName.get() && fileName.get()) {

                const char * methodNameStr = convert_jstring_to_char(env, (jstring) methodName.get());
                const char * classNameStr = convert_jstring_to_char(env, (jstring) className.get());

                { // 忽略java.lang.Thread.getStackTrace、dalvik.system.VMStack.getThreadStackTrace方法

                    if (classNameStr &&
                        methodNameStr &&
                        strncmp(classNameStr, "java.lang.Thread", 16) == 0 &&
                        strncmp(methodNameStr, "getStackTrace", 13) == 0) {
                        free((void *) classNameStr);
                        free((void *) methodNameStr);
                        continue;
                    }

                    if (classNameStr &&
                        methodNameStr &&
                        strncmp(classNameStr, "dalvik.system.VMStack", 21) == 0 &&
                        strncmp(methodNameStr, "getThreadStackTrace", 13) == 0) {
                        free((void *) classNameStr);
                        free((void *) methodNameStr);
                        continue;
                    }
                }


                if (c > 0) {
                    callstack += "\n";
                }

                callstack += "\t";

                if (classNameStr) {
                    callstack += classNameStr;
                    free((void *) classNameStr);
                }

                if (methodNameStr) {
                    callstack += ".";
                    callstack += methodNameStr;
                    free((void *) methodNameStr);
                }

                const char * fileNameStr = convert_jstring_to_char(env, (jstring) fileName.get());
                if (fileNameStr) {
                    callstack += " ";
                    callstack += fileNameStr;
                    free((void *) fileNameStr);
                }

                sprintf(buf, ":%d", lineNumberStr);
                callstack += buf;

                c++;
            }
        }
    }
}