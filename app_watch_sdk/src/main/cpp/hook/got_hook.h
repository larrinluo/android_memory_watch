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
// Created by larrin luo on 2020-01-25.
//

#ifndef ANDROID_MEMORY_WATCH_GOT_HOOK_H
#define ANDROID_MEMORY_WATCH_GOT_HOOK_H

#include <sys/types.h>
#include <sys/socket.h>
#include <vector>
#include <string>

typedef int (*OPEN_METHOD)(const char* pathname, int flags, mode_t mode);
typedef ssize_t (*WRITE_METHOD)(int fd, const void *buf, size_t count);
typedef int (*CLOSE_METHOD)(int fd);
typedef int (*CONNECT_METHOD)(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
typedef ssize_t (*RECVMSG_METHOD)(int sockfd, struct msghdr *msg, int flags);

/**
 * Open Hook
 */
struct OpenMethodContext {

    // method parameters
    const char *pathname;
    int flags;
    mode_t mode;

    // context parameters
    OPEN_METHOD origin_open;
    int retVal;
};

/**
 * Write hook
 */
struct WriteMethodContext {

    // method parameters
    int fd;
    const void *buf;
    size_t count;

    // context parameters
    WRITE_METHOD origin_write;
    ssize_t retVal;
};

/**
 * Close hook
 */
struct CloseMethodContext {

    // method parameters
    int fd;

    // context parameters
    CLOSE_METHOD origin_close;
    int retVal;
};

/**
 * Connect hook
 */
struct ConnectMethodContext {

    // method parameters
    int sockfd;
    const struct sockaddr *addr;
    socklen_t addrlen;

    // context parameters
    CONNECT_METHOD origin_connect;
    int retVal;
};

struct RecvMsgMethodContext {

    // method parameters
    int sockfd;
    struct msghdr *msg;
    int flags;

    // context parameters
    RECVMSG_METHOD origin_recvmsg;
    ssize_t retVal;
};

typedef int (*OpenHookMethod)(OpenMethodContext &);
typedef int (*WriteHookMethod)(WriteMethodContext &);
typedef int (*CloseMethod)(CloseMethodContext&);
typedef int (*ConnectMethod)(ConnectMethodContext &);
typedef int (*RecvMsgMethod)(RecvMsgMethodContext &);

class GotHook {

    // open hooks
    static std::string targetSo_open;
    static std::vector<OpenHookMethod> open_hook_list;
    static int open_hook_entry(const char* pathname, int flags, mode_t mode);

    // write hooks
    static std::string targetSo_write;
    static std::vector<WriteHookMethod> write_hook_list;
    static ssize_t write_hook_entry(int fd, const void *buf, size_t count);

    // close hooks
    static std::string targetSo_close;
    static std::vector<CloseMethod> close_hook_list;
    static int close_hook_entry(int fd);

    // connect hooks
    static std::string targetSo_connect;
    static std::vector<ConnectMethod> connect_hook_list;
    static int connect_hook_entry(int sockfd, const struct sockaddr *addr, socklen_t addrlen);

    // recvms hooks
    static std::string targetSo_recvmsg;
    static std::vector<RecvMsgMethod> recvmsg_hook_list;
    static ssize_t recvmsg_hook_entry(int sockfd, struct msghdr *msg, int flags);

public:

    static OPEN_METHOD origin_open;
    static WRITE_METHOD  origin_write;
    static CLOSE_METHOD origin_close;
    static CONNECT_METHOD origin_connect;
    static RECVMSG_METHOD  origin_recvmsg;

    static void add_open_hook(const char *targetSo, OpenHookMethod hookMethod);
    static void add_write_hook(const char *targetSo, WriteHookMethod hookMethod);
    static void add_close_hook(const char *targetSo, CloseMethod hookMethod);
    static void add_connect_hook(const char *targetSo, ConnectMethod hookMethod);
    static void add_recvmsg_hook(const char *targetSo, RecvMsgMethod hookMethod);

    static bool installHooks();

};



#endif //ANDROID_MEMORY_WATCH_GOT_HOOK_H
