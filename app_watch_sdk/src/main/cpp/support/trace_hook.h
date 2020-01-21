#ifndef APP_WARTCH_SDK__ANDROID_UTIL__H
#define APP_WARTCH_SDK__ANDROID_UTIL__H

#include <climits>

enum ANR_OUTPUT_MODE {
    ANR_REDIRECT,
    ANR_COPY
};

void installHook(int sdkVersion, const char *path, int output_mode);
void reportAnr();


#endif // APP_WARTCH_SDK__ANDROID_UTIL__H
