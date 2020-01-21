# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(XHOOK_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../xHook/libxhook/jni")
include_directories(BEFORE
                    "/wd_1t/work/github/xHook/libxhook/jni"
        )

link_directories(
        BEFORE
        "/out/host/linux-x86/lib64/")

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
             support/trace_hook.cpp)

find_library( log-lib log)

target_link_libraries( appWatch

                       ${log-lib}
        )