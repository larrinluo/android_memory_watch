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
// Created by larrin luo on 2020-01-30.
//

#include "utils.h"
#include <string>
#include <unwind.h>
#include <dlfcn.h>
#include <iomanip>
#include "../jni/jni_utils.h"

//for test android
struct BacktraceState
{
    void** current;
    void** end;
};

static _Unwind_Reason_Code unwind_callback(
        struct _Unwind_Context* context, void* arg)
{
    BacktraceState* state = static_cast<BacktraceState*>(arg);
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc) {
        if (state->current == state->end) {
            return _URC_END_OF_STACK;
        } else {
            *state->current++ = reinterpret_cast<void*>(pc);
        }
    }
    return _URC_NO_REASON;
}

static int capture_backtrace(void** buffer, int max)
{
    BacktraceState state = {buffer, buffer + max};
    _Unwind_Backtrace(unwind_callback, &state);

    return state.current - buffer;
}

static std::string dump_backtrace(void** buffer, size_t depth)
{
    std::string backStr;
    char buf[32];
    for (size_t idx = 0; idx < depth; ++idx) {
        const void* addr = buffer[idx];
        const char* symbol = "";

        Dl_info info;
        if (dladdr(addr, &info) && info.dli_sname) {
            symbol = info.dli_sname;
        }

        // 忽略get_full_callstack、get_native_callstack方法
        if (strstr(symbol, "get_full_callstack") ||
//            strstr(symbol, "my_pthread_") ||
            (strstr(symbol, "pthread") &&  strstr(symbol, "hook_entry")) ||
            (strstr(symbol, "DeadLock") &&  strstr(symbol, "try_lock")) ||
            strstr(symbol, "get_native_callstack")) {
            continue;
        }

        sprintf(buf, "  [0x%08X] ", addr);
        backStr += buf;
        if (strlen(symbol) > 0)
            backStr += symbol;
        else
            backStr += "-";

        backStr += "\n";
    }
    return backStr;
}

// android backtrace methods --- end


void get_native_callstack(std::string &callstack, int level) {

    std::string funcBt;
    int nptrs = 0;
    void* buffer[level];
    nptrs = capture_backtrace(buffer, level);
    callstack = dump_backtrace(buffer, nptrs);
}

void get_full_callstack(std::string &callstack, int level) {

    // 减少调用栈深度，直接获取native堆栈，不调用get_native_callstack
    std::string funcBt;
    int nptrs = 0;
    void* buffer[level];
    nptrs = capture_backtrace(buffer, level);
    callstack = dump_backtrace(buffer, nptrs);

    JNI::get_java_callstack(callstack);
}
