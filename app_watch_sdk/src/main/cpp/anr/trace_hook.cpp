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
// Created by larrin luo on 2020-01-18.
//

#include "trace_hook.h"
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include "xhook.h"
#include "../jni/app_watch_sdk.h"
#include <android/log.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sstream>
#include <jni.h>

#define TOMBSTONE_CLIENT_SOCKET "/dev/socket/tombstoned_java_trace"
#define ANR_TRACE_FILE "/data/anr/traces.txt"
#define TAG "TRACE_HOOK"

char ANR::trace_file[PATH_MAX];
int ANR::outputMode = ANR::REDIRECT;

int ANR::tombstone_client_socket = -1;
int ANR::tombstone_fd = -1;
int ANR::my_trace_fd = -1;

/**
 * hook open方法:
 *
 *   当/data/anr/traces.txt被打开是，表明发生ANR。
 *   例外： 通过adb执行kill -3 pid，不一定是ANR，但是仍然出发输出trace
 *
 *   这是可以通过origin_open打开我们的trace文件， 并返回句柄来将trace信息重定向到我们的输出文件，
 *   或者记录origin_open返回的句柄， 并hook write方法收集trace数据，最后写到我们的输出文件，实现复制
 *
 * @param pathname
 * @param flags
 * @param mode
 * @return
 */
int ANR::my_open(OpenMethodContext &context) {
    if (strcmp(ANR_TRACE_FILE, context.pathname) == 0) {
        if (outputMode == ANR::REDIRECT) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "redirect trace to :%s", trace_file);
            context.pathname = trace_file; // change target
        } else {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "lately, copy trace to: %s", trace_file);
            context.retVal =  context.origin_open(context.pathname, context.flags, context.mode);
            tombstone_fd = context.retVal;
            return 1;
        }
    }

    return 0;
}

/**
 * hook connect方法:
 *
 *      当通过local socket连接/dev/socket/tombstoned_java_trace时，表明准备输出trace
 *      例外： 通过adb执行kill -3 pid，不一定是ANR，但是仍然出发输出trace
 *
 *      高版本android加强了tombstone信息的保护， 不直接通过open的方式打开trace文件输出trace,
 *      而是通过tombstone_client方式， 用local socket请求system_server打开trace文件，并
 *      返回本进程可用的句柄(应该是类似binder的机制， 在内核中直接为本进程复制文件句柄)。
 *
 *      这样就不能通过open方法来获取trace文件句柄了。
 *
 *      我们的方法是：
 *      1. hook connect, 检测tombsonr连接, 并获取得到的tombsone socket
 *      2. hook recvmsg, 过滤tombsone socket收到的应答，并从中解析trace fd
 *
 *      得到trace fd后， 根据ANR_OUPUT模式
 *      1. 如果是ANR_RECIRECT, 则open我们的输出trace文件，并用其句柄替换cmsg，
 *         这样trace将输出到我们的trace输出文件，而不是系统的trace文件
 *      2. 如果是ANR_COPY模式， 记录tombsone fd, 并通过hook write将trace复制我们的
 *         trace输出文件。
 *
 * @param sockfd
 * @param addr
 * @param addrlen
 * @return
 */
int ANR::my_connect(ConnectMethodContext &context) {
    if (context.addr->sa_family == AF_LOCAL) { // 如果是tombstone连接, 在recvmsg中获取tombstne的fd
        struct sockaddr_un *p_addr = (struct sockaddr_un *)context.addr;
        if (strncmp(TOMBSTONE_CLIENT_SOCKET, p_addr->sun_path, strlen(TOMBSTONE_CLIENT_SOCKET)) == 0) {
            tombstone_client_socket = context.sockfd;
        }
    }

    return 0;
}

/**
 * 两种方案:
 * 方案 1. 链接write方法， 从中过滤出anr数据
 *         优点： 我们是复制一份anr数据， 系统收集的数据不受影响
 *         缺点： 需要hook更多方法，性能、稳定性受一定影响
 * 方案 2. 直接修改msg数据 -> 打开我们的anr文件， 将cmsg_fds[0]直接修改为我们的fd
 *         优点： hook更少的方法， 更高效、稳定
 *         缺点： anr将被我们拦截， 系统将不会收集到我们的数据
 * @param sockfd
 * @param msg
 * @param flags
 * @return
 */
int ANR::my_recvmsg(RecvMsgMethodContext &context) {

    context.retVal = context.origin_recvmsg(context.sockfd, context.msg, context.flags);

    if (context.sockfd == tombstone_client_socket) { // tombstone应答，提取tombstone fd用于获取Trace
        struct cmsghdr* cmsg;
        for (cmsg = CMSG_FIRSTHDR(context.msg); cmsg != nullptr; cmsg = CMSG_NXTHDR(context.msg, cmsg)) {
            if (cmsg->cmsg_level != SOL_SOCKET || cmsg->cmsg_type != SCM_RIGHTS) {
                continue;
            }

            if (cmsg->cmsg_len % sizeof(int) != 0) {
                break;
            } else if (cmsg->cmsg_len <= CMSG_LEN(0)) {
                break;
            }

            int* cmsg_fds = reinterpret_cast<int*>(CMSG_DATA(cmsg));
            size_t cmsg_fdcount = static_cast<size_t>(cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int);
            if (cmsg_fdcount == 1) {

                if (GotHook::origin_open) {
                    my_trace_fd = GotHook::origin_open(trace_file, O_APPEND | O_CREAT | O_WRONLY, 0666);
                } else {
                    my_trace_fd = open(trace_file, O_APPEND | O_CREAT | O_WRONLY, 0666);
                }

                if (my_trace_fd != -1) {
                    if (outputMode == ANR::REDIRECT) {

                        if (GotHook::origin_close) {
                            GotHook::origin_close(cmsg_fds[0]);
                        } else {
                            close(cmsg_fds[0]);
                        }

                        cmsg_fds[0] = my_trace_fd;
                        my_trace_fd = -1;
                    } else {
                        tombstone_fd = cmsg_fds[0];
                        __android_log_print(ANDROID_LOG_DEBUG, TAG, "lately, copy trace to: %s", trace_file);
                    }
                }
            }
        }

    }
    return 1;
}

int ANR::my_write(WriteMethodContext& context) {

    int bytes = context.origin_write(context.fd, context.buf, context.count);
    context.retVal = bytes;

    if (context.fd == tombstone_fd && my_trace_fd != -1) {
        int c = 0;
        while (c < bytes) {
            int b = GotHook::origin_write(my_trace_fd, context.buf, bytes - c);
            if (b <= 0) {
                __android_log_print(ANDROID_LOG_WARN, TAG, "copy trace file failed: %s", trace_file);
                if (GotHook::origin_close)
                    GotHook::origin_close(my_trace_fd);
                else
                    close(my_trace_fd);

                my_trace_fd = -1;
                break;
            }
            c += b;
        }
    }

    return 1;
}

int ANR::my_close(CloseMethodContext &context) {
    if (context.fd == tombstone_fd && my_trace_fd != -1) {
        context.origin_close(my_trace_fd);
        my_trace_fd = -1;
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "copy trace file  done: %s", trace_file);
    }

    return 0;
}

void ANR::installHooks(int sdkVersion, const char *path, int output_mode) {

    strncpy(trace_file, path, PATH_MAX - 1);
    outputMode = output_mode;
    if (outputMode != ANR::REDIRECT && outputMode != ANR::COPY) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "invalide anr output mode %d, keep ANR_REDIRECT mode", output_mode);
        outputMode = ANR::REDIRECT;
    }

    if (sdkVersion >= 27) { // android 8.1以上版本
        GotHook::add_connect_hook(".*\\.so$", ANR::my_connect);
        GotHook::add_recvmsg_hook(".*\\.so$", ANR::my_recvmsg);
    } else { // android 8.1以下
        GotHook::add_open_hook(".*\\libart.so$", ANR::my_open);
    }

    if (outputMode == ANR::COPY) {
        GotHook::add_write_hook(".*\\libart.so$", ANR::my_write);
        GotHook::add_close_hook(".*\\libart.so$", ANR::my_close);
    }

    bool ret = GotHook::installHooks();

    if (!ret) {
        if (outputMode == ANR::COPY) {
            if (GotHook::origin_write == NULL || GotHook::origin_close == NULL) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "anr hook try to downgrade to REDIRECT mode");
                outputMode = ANR::REDIRECT; // downgrade
            }
        }

        if (outputMode == ANR::REDIRECT) {
            if (sdkVersion >= 27) {
                if (GotHook::origin_connect != NULL || GotHook::origin_recvmsg != NULL) {
                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "anr hook downgrade to REDIRECT mode OK 1");
                    ret = true;
                }
            } else {
                if (GotHook::origin_open != NULL) {
                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "anr hook downgrade to REDIRECT mode OK 2");
                    ret = true;
                }
            }
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "install result: %d", ret);
}

