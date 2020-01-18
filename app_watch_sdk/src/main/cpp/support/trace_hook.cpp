#include "trace_hook.h"
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include "xhook.h"
#include <android/log.h>

char trace_file[PATH_MAX];
int (*origin_open)(const char* pathname,int flags,mode_t mode);

int my_open(const char* pathname,int flags,mode_t mode) {

    if (strcmp("/data/anr/traces.txt", pathname) == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, "TRACE_HOOK", "dump trace --> to %s", trace_file);
        return origin_open(trace_file, flags, mode);
    } else {
        return origin_open(pathname, flags, mode);
    }
}

void installHook(int sdkVersion) {

    int err;
    if (sdkVersion >= 21) {
        err = xhook_register(".*\\libart.so$", "open", (void *) my_open, (void **) &origin_open);
    } else {
        err = xhook_register(".*\\.so$",  "open", (void *) my_open,  (void **)&origin_open);
    }

    xhook_refresh(true);
    __android_log_print(ANDROID_LOG_DEBUG, "TRACE_HOOK", "install result: %d", err);
}

void setTraceFile(const char *path) {
    strncpy(trace_file, path, PATH_MAX - 1);
}

