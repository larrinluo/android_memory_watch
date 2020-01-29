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
// Created by larrin luo on 2020-01-27.
//

#ifndef ANDROID_MEMORY_WATCH_UTILS_H
#define ANDROID_MEMORY_WATCH_UTILS_H

#include <cstdint>
#include <linux/time.h>
#include <time.h>
#include <unistd.h>
#include <syscall.h>
#include <string>
#include <sys/prctl.h>
#include "utils.h"

static inline uint64_t get_relative_millisecond()
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    uint64_t usec = ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
    return usec;
}

static inline int get_tid()
{
#if defined(__linux__) || defined(__ANDROID__)
    return syscall(SYS_gettid);
#else
    return getpid();
#endif
}

static inline bool is_main_thread(pid_t tid)
{
    if (tid < 0) {
        return get_tid() == getpid();
    } else {
        return tid == getpid();
    }
}

static inline char* get_thread_name(char *name, pid_t tid) {
    if (is_main_thread(tid)) {
        snprintf(name, 16, "%s", "MainThread");
    } else {
        prctl(PR_GET_NAME, name);
    }
    return name;
}

void get_native_callstack(std::string &callstack, int level);
void get_full_callstack(std::string &callstack, int deepth);

#endif //ANDROID_MEMORY_WATCH_UTILS_H
