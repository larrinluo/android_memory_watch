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

#ifndef APP_WARTCH_SDK__ANDROID_UTIL__H
#define APP_WARTCH_SDK__ANDROID_UTIL__H

#include <climits>
#include "../hook/got_hook.h"

class ANR {

public:

    enum OUTPUT_MODE {
        REDIRECT,
        COPY
    };


private:

    static char trace_file[PATH_MAX];
    static int outputMode;

    static int tombstone_client_socket;
    static int tombstone_fd;
    static int my_trace_fd;

    static int my_open(OpenMethodContext &context);
    static int my_connect(ConnectMethodContext &context);
    static int my_recvmsg(RecvMsgMethodContext &context);
    static int my_write(WriteMethodContext& context);
    static int my_close(CloseMethodContext &context);

public:

    static void installHooks(int sdkVersion, const char *path, int output_mode);
};

#endif // APP_WARTCH_SDK__ANDROID_UTIL__H
