# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(XHOOK_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../xHook/libxhook/jni")
include_directories(BEFORE ${XHOOK_SRC})
link_directories(BEFORE "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../xHook/libxhook/libs/${ANDROID_ABI}")

add_library( appWatch
             SHARED

             ${XHOOK_SRC}/xh_core.c
             ${XHOOK_SRC}/xh_elf.c
             ${XHOOK_SRC}/xh_jni.c
             ${XHOOK_SRC}/xh_log.c
             ${XHOOK_SRC}/xhook.c
             ${XHOOK_SRC}/xh_util.c
             ${XHOOK_SRC}/xh_version.c

             jni/app_watch_sdk.cpp
             jni/jni_utils.cpp
             hook/got_hook.cpp
             anr/trace_hook.cpp
             deadlock/deadlock.cpp
             utils/utils.cpp
             utils/utils_android.cpp
        )

find_library( log-lib log)

target_link_libraries( appWatch

#                       xhook
                       ${log-lib}
        )