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

#include "got_hook.h"
#include "xhook.h"
#include <android/log.h>

#define TAG "GOT_HOOK"

std::string GotHook::targetSo_open;
OPEN_METHOD GotHook::origin_open = NULL;
std::vector<OpenHookMethod> GotHook::open_hook_list;

std::string GotHook::targetSo_write;
WRITE_METHOD GotHook::origin_write = NULL;
std::vector<WriteHookMethod> GotHook::write_hook_list;

std::string GotHook::targetSo_close;
CLOSE_METHOD GotHook::origin_close = NULL;
std::vector<CloseMethod> GotHook::close_hook_list;

std::string GotHook::targetSo_connect;
CONNECT_METHOD GotHook::origin_connect = NULL;
std::vector<ConnectMethod> GotHook::connect_hook_list;

std::string GotHook::targetSo_recvmsg;
RECVMSG_METHOD GotHook::origin_recvmsg = NULL;
std::vector<RecvMsgMethod> GotHook::recvmsg_hook_list;

int GotHook::open_hook_entry(const char* pathname, int flags, mode_t mode) {
    OpenMethodContext context;
    context.origin_open = GotHook::origin_open;
    context.pathname = pathname;
    context.flags = flags;
    context.mode = mode;

    for (auto hook: open_hook_list) {
        if (hook(context) != 0) {
            return context.retVal;
        }
    }

    return origin_open(context.pathname, context.flags, context.mode);
}

ssize_t GotHook::write_hook_entry(int fd, const void *buf, size_t count) {
    WriteMethodContext context;
    context.origin_write = GotHook::origin_write;
    context.fd = fd;
    context.buf = buf;
    context.count = count;

    for (auto hook: write_hook_list) {
        if (hook(context)) {
            return context.retVal;
        }
    }

    return origin_write(context.fd, context.buf, context.count);
}

int GotHook::close_hook_entry(int fd) {
    CloseMethodContext context;
    context.origin_close = GotHook::origin_close;
    context.fd = fd;

    for (auto hook: close_hook_list) {
        if (hook(context)) {
            return context.retVal;
        }
    }

    return origin_close(context.fd);
}

int GotHook::connect_hook_entry(int sockfd, const struct sockaddr *addr, socklen_t addrlen) {
    ConnectMethodContext context;
    context.origin_connect = GotHook::origin_connect;
    context.sockfd = sockfd;
    context.addr = addr;
    context.addrlen = addrlen;

    for (auto hook: connect_hook_list) {
        if (hook(context)) {
            return context.retVal;
        }
    }

    return origin_connect(context.sockfd, context.addr, context.addrlen);
}

ssize_t GotHook::recvmsg_hook_entry(int sockfd, struct msghdr *msg, int flags) {
    RecvMsgMethodContext context;
    context.origin_recvmsg = GotHook::origin_recvmsg;
    context.sockfd = sockfd;
    context.msg = msg;
    context.flags = flags;

    for (auto hook: recvmsg_hook_list) {
        if (hook(context)) {
            return context.retVal;
        }
    }

    return origin_recvmsg(context.sockfd, context.msg, context.flags);
}

void GotHook::add_open_hook(const char *targetSo, OpenHookMethod hookMethod) {
    if (targetSo_open.length() == 0) {
        targetSo_open = targetSo;
    } else if (targetSo_open.compare(targetSo) != 0) {
        targetSo_open = ".*\\.so$";
    }

    open_hook_list.push_back(hookMethod);
}

void GotHook::add_write_hook(const char *targetSo, WriteHookMethod hookMethod) {

    if (targetSo_write.length() == 0) {
        targetSo_write = targetSo;
    } else if (targetSo_write.compare(targetSo) != 0) {
        targetSo_write = ".*\\.so$";
    }

    write_hook_list.push_back(hookMethod);
}

void GotHook::add_close_hook(const char *targetSo, CloseMethod hookMethod) {

    if (targetSo_close.length() == 0) {
        targetSo_close = targetSo;
    } else if (targetSo_close.compare(targetSo) != 0) {
        targetSo_close = ".*\\.so$";
    }

    close_hook_list.push_back(hookMethod);
}

void GotHook::add_connect_hook(const char *targetSo, ConnectMethod hookMethod) {

    if (targetSo_connect.length() == 0) {
        targetSo_connect = targetSo;
    } else if (targetSo_connect.compare(targetSo) != 0) {
        targetSo_connect = ".*\\.so$";
    }

    connect_hook_list.push_back(hookMethod);
}

void GotHook::add_recvmsg_hook(const char *targetSo, RecvMsgMethod hookMethod) {

    if (targetSo_recvmsg.length() == 0) {
        targetSo_recvmsg = targetSo;
    } else if (targetSo_recvmsg.compare(targetSo) != 0) {
        targetSo_recvmsg = ".*\\.so$";
    }

    recvmsg_hook_list.push_back(hookMethod);
}

bool GotHook::installHooks() {

    if (targetSo_write.length() > 0) {
        xhook_register(targetSo_write.c_str(), "write", (void *) GotHook::write_hook_entry, (void **) &GotHook::origin_write);
    }

    if (targetSo_open.length() > 0) {
        xhook_register(targetSo_open.c_str(), "open", (void *) GotHook::open_hook_entry, (void **) &GotHook::origin_open);
    }

    if (targetSo_close.length() > 0) {
        xhook_register(targetSo_close.c_str(), "close", (void *) GotHook::close_hook_entry, (void **) &GotHook::origin_close);
    }

    if (targetSo_connect.length() > 0) {
        xhook_register(targetSo_connect.c_str(), "connect", (void *) GotHook::connect_hook_entry, (void **) &GotHook::origin_connect);
    }

    if (targetSo_recvmsg.length() > 0) {
        xhook_register(targetSo_recvmsg.c_str(), "recvmsg", (void *) GotHook::recvmsg_hook_entry, (void **) &GotHook::origin_recvmsg);
    }

    xhook_refresh(false);

    bool ret = true;

    if (targetSo_write.length() > 0 && GotHook::origin_write == NULL) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "install write hook failed!!");
        ret = false;
    }

    if (targetSo_open.length() > 0 && GotHook::origin_open == NULL) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "install open hook failed!!");
        ret = false;
    }

    if (targetSo_close.length() > 0 && GotHook::origin_close == NULL) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "install close hook failed!!");
        ret = false;
    }

    if (targetSo_connect.length() > 0 && GotHook::origin_connect) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "install connect hook failed!!");
        ret = false;
    }

    if (targetSo_recvmsg.length() > 0 && GotHook::origin_recvmsg) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "install recvmsg hook failed!!");
        ret = false;
    }

    if (!ret) {
        //xhook_clear();
        //TODO: restore all hooked functions
    }

    return ret;
}