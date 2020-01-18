#include <jni.h>
#include <string>
#include "../support/trace_hook.h"

extern "C" {

bool installed = false;

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_TraceDumper_installHooks(JNIEnv *env, jobject thiz, jint sdkVersion) {
    if (!installed) {
        installed = true;
        installHook(sdkVersion);
    }
}

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_TraceDumper_setTracePath(JNIEnv *env, jobject thiz,
                                                                    jstring trace_file) {

    const char *path = env->GetStringUTFChars(trace_file, 0);
    if (path != NULL) {
        setTraceFile(path);
    }

    env->ReleaseStringUTFChars(trace_file, path);

}

} // extern "C"

