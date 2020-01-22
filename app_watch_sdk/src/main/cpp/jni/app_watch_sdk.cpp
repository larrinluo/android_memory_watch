#include <jni.h>
#include <string>
#include "app_watch_sdk.h"
#include "../anr/trace_hook.h"

extern "C" {

bool installed = false;

JavaVM * JNI::jvm = NULL;
jobject JNI::gClassLoader = NULL;
jmethodID JNI::gLoadClassMethod = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;

    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    JNI::init(jvm);


    jclass clientClass = env->FindClass("com/ttphoto/resource/watch/sdk/client/AppWatchClient");
    jclass classClass = env->GetObjectClass(clientClass);
    jmethodID getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject localClassLoader = env->CallObjectMethod(clientClass, getClassLoaderMethod);
    JNI::gClassLoader = env->NewGlobalRef(localClassLoader);

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    JNI::gLoadClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                             "(Ljava/lang/String;)Ljava/lang/Class;");

    result = JNI_VERSION_1_6;
    return result;
}

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_AnrWartch_installHooks(JNIEnv *env, jobject thiz,
        jint sdkVersion, jstring outputPath, jint outputMode) {
    if (!installed) {
        installed = true;

        const char *path = env->GetStringUTFChars(outputPath, 0);
        if (path != NULL) {
            installHook(sdkVersion, path, outputMode);
        }

        env->ReleaseStringUTFChars(outputPath, path);
    }
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
