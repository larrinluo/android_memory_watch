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

#include <pthread.h>

typedef int (*OPEN_METHOD)(const char* pathname, int flags, mode_t mode);
typedef ssize_t (*WRITE_METHOD)(int fd, const void *buf, size_t count);
typedef int (*CLOSE_METHOD)(int fd);
typedef int (*CONNECT_METHOD)(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
typedef ssize_t (*RECVMSG_METHOD)(int sockfd, struct msghdr *msg, int flags);

typedef int (*PTHREAD_MUTEX_INIT)(pthread_mutex_t* __mutex, const pthread_mutexattr_t* __attr);
typedef int (*PTHREAD_MUTEX_DESTROY)(pthread_mutex_t* __mutex);
typedef int (*PTHREAD_MUTEX_LOCK)(pthread_mutex_t *mutex);
typedef int (*PTHREAD_MUTEX_UNLOCK)(pthread_mutex_t* __mutex);

typedef int (*PTHREAD_RWLOCK_INIT)(pthread_rwlock_t* __rwlock, const pthread_rwlockattr_t* __attr);
typedef int (*PTHREAD_RWOCK_DESTROY)(pthread_rwlock_t* __rwlock);
typedef int (*PTHREAD_RWLOCK_RDLOCK)(pthread_rwlock_t* __rwlock);
typedef int (*PTHREAD_RWLOCK_WRLOCK)(pthread_rwlock_t* __rwlock);
typedef int (*PTHREAD_RWLOCK_UNLOCK)(pthread_rwlock_t* __rwlock);

#if __ANDROID_API__ >= __ANDROID_API_N__
typedef int (*PTHREAD_SPIN_DESTROY)(pthread_spinlock_t* __spinlock);
typedef int (*PTHREAD_SPIN_INIT)(pthread_spinlock_t* __spinlock, int __shared);
typedef int (*PTHREAD_SPIN_LOCK)(pthread_spinlock_t* __spinlock);
typedef int (*PTHREAD_SPIN_UNLOCK)(pthread_spinlock_t* __spinlock);
#endif

/**
 * Open Hook
 */
struct OpenMethodContext {

    // method parameters
    const char *pathname;
    int flags;
    mode_t mode;

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

    ssize_t retVal;
};

/**
 * Close hook
 */
struct CloseMethodContext {

    // method parameters
    int fd;

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

    int retVal;
};

struct RecvMsgMethodContext {

    // method parameters
    int sockfd;
    struct msghdr *msg;
    int flags;

    ssize_t retVal;
};

struct PthreadMutexLockContext {

    // method parameters
    pthread_mutex_t * mutex;

    int retVal;
};

struct PthreadMutexUnlockContext {
    // method parameters
    pthread_mutex_t * mutex;

    int retVal;
};

struct PthreadMutexInitContext {
    // method parameters
    pthread_mutex_t * mutex;
    const pthread_mutexattr_t  *attr;

    int retVal;
};

struct PthreadMutexDestroyContext {
    // method parameters
    pthread_mutex_t * mutex;

    int retVal;
};

struct PthreadRwLockInitContext {
    pthread_rwlock_t* rwlock;
    const pthread_rwlockattr_t* attr;
    int retVal;
};

struct PthreadRwLockDestroyContext {
    pthread_rwlock_t* rwlock;
    int retVal;
};

struct PthreadRWLockRDLockContext {
    pthread_rwlock_t* rwlock;
    int retVal;
};

struct PthreadRWLockWRLockContext {
    pthread_rwlock_t* rwlock;
    int retVal;
};

struct PthreadRWLockUnlockContext {
    pthread_rwlock_t* rwlock;
    int retVal;
};

typedef int (*OpenHookMethod)(OpenMethodContext &);
typedef int (*WriteHookMethod)(WriteMethodContext &);
typedef int (*CloseMethod)(CloseMethodContext&);
typedef int (*ConnectMethod)(ConnectMethodContext &);
typedef int (*RecvMsgMethod)(RecvMsgMethodContext &);

typedef int (*PthreadMutexInitMethod)(PthreadMutexInitContext &);
typedef int (*PthreadMutexDestroyMethod)(PthreadMutexDestroyContext &);
typedef int (*PthreadMutexLockMethod)(PthreadMutexLockContext &);
typedef int (*PthreadMutexUnlockMethod)(PthreadMutexUnlockContext &);

typedef int (*PthreadRWLockInitMethod)(PthreadRwLockInitContext &);
typedef int (*PthreadRWLockDestoryMethod)(PthreadRwLockDestroyContext &);
typedef int (*PthreadRWLockRdlockMethod)(PthreadRWLockRDLockContext &);
typedef int (*PthreadRWLockWrLockMethod)(PthreadRWLockWRLockContext &);
typedef int (*PthreadRWLockUnlockMethod)(PthreadRWLockUnlockContext &);


typedef int (*PTHREAD_MUTEX_INIT)(pthread_mutex_t* __mutex, const pthread_mutexattr_t* __attr);
typedef int (*PTHREAD_MUTEX_DESTROY)(pthread_mutex_t* __mutex);

class GotHook {

    static std::string sDeadLock_targetSo;

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

    // pthread_mutex_lock hooks
    static std::vector<PthreadMutexLockMethod > pthread_mutex_lock_hook_list;
    static int pthread_mutex_lock_hook_entry(pthread_mutex_t* __mutex);

    // pthread_mutex_unlock hooks
    static std::vector<PthreadMutexUnlockMethod > pthread_mutex_unlock_hook_list;
    static int pthread_mutex_unlock_hook_entry(pthread_mutex_t* __mutex);

    // pthread_mutex_init hooks
    static std::vector<PthreadMutexInitMethod> pthread_mutex_init_hook_list;
    static int pthread_mutex_init_hook_entry(pthread_mutex_t* __mutex, const pthread_mutexattr_t* __attr);

    // pthread_mutext_destroy hooks
    static std::vector<PthreadMutexDestroyMethod> pthread_mutex_destroy_hook_list;
    static int pthread_mutex_destroy_hook_entry(pthread_mutex_t *mutex);

    // pthread_mutex_timedlock
    static int pthread_mutex_timedlock_hook_entry(pthread_mutex_t* __mutex, const struct timespec* __timeout);

    // ptread_rwlock_init hooks
    static std::vector<PthreadRWLockInitMethod > pthread_rwlock_init_hook_list;
    static int pthread_rwlock_init_hook_entry(pthread_rwlock_t* __rwlock, const pthread_rwlockattr_t* __attr);

    // pthread_rwlock_destroy hooks
    static std::vector<PthreadRWLockDestoryMethod > pthread_rwlock_destroy_hook_list;
    static int pthread_rwlock_destroy_hook_entry(pthread_rwlock_t* __rwlock);

    // pthread_rwlock_rdlock hooks
    static std::vector<PthreadRWLockRdlockMethod > pthread_rwlock_rdlock_hook_list;
    static int pthread_rwlock_rdlock_hook_entry(pthread_rwlock_t* __rwlock);

    // pthread_rwlock_wdlock hooks
    static std::vector<PthreadRWLockWrLockMethod > pthread_rwlock_wrlock_hook_list;
    static int pthread_rwlock_wrlock_hook_entry(pthread_rwlock_t *__rwlock);

    // pthread_rwlock_unlock hooks
    static std::vector<PthreadRWLockUnlockMethod > pthread_rwlock_unlock_hook_list;
    static int pthread_rwlock_unlock_hook_entry(pthread_rwlock_t* __rwlock);

public:

    static OPEN_METHOD origin_open;
    static WRITE_METHOD  origin_write;
    static CLOSE_METHOD origin_close;
    static CONNECT_METHOD origin_connect;
    static RECVMSG_METHOD  origin_recvmsg;

    static PTHREAD_MUTEX_INIT origin_pthread_mutex_init;
    static PTHREAD_MUTEX_DESTROY origin_pthread_mutex_destroy;
    static PTHREAD_MUTEX_LOCK origin_pthread_mutex_lock;
    static PTHREAD_MUTEX_UNLOCK origin_pthread_mutex_unlock;

    static PTHREAD_RWLOCK_INIT origin_pthread_rwlock_init;
    static PTHREAD_RWOCK_DESTROY origin_pthread_rwlock_destroy;
    static PTHREAD_RWLOCK_RDLOCK origin_pthread_rwlock_rdlock;
    static PTHREAD_RWLOCK_WRLOCK origin_pthread_rwlock_wrlock;
    static PTHREAD_RWLOCK_UNLOCK origin_pthread_rwlock_unlock;

    static void add_open_hook(const char *targetSo, OpenHookMethod hookMethod);
    static void add_write_hook(const char *targetSo, WriteHookMethod hookMethod);
    static void add_close_hook(const char *targetSo, CloseMethod hookMethod);
    static void add_connect_hook(const char *targetSo, ConnectMethod hookMethod);
    static void add_recvmsg_hook(const char *targetSo, RecvMsgMethod hookMethod);

    static void add_pthread_mutex_lock_hook(PthreadMutexLockMethod);
    static void add_pthread_mutex_unlock_hook(PthreadMutexUnlockMethod);
    static void add_pthread_mutex_init_hook(PthreadMutexInitMethod);
    static void add_pthread_mutex_destroy_hook(PthreadMutexDestroyMethod);


    static void add_pthread_rwlock_init_hook(PthreadRWLockInitMethod);
    static void add_pthread_rwlock_destory_hook(PthreadRWLockDestoryMethod);
    static void add_pthread_rwlock_rdlock_hook(PthreadRWLockRdlockMethod);
    static void add_pthread_rwlock_wdlock_hook(PthreadRWLockWrLockMethod);
    static void add_pthread_rwlock_unlock_hook(PthreadRWLockUnlockMethod);

    static void setDeadLockTargetSo(const char *so) {
        sDeadLock_targetSo = so;
    }

    static bool installHooks();

};



#endif //ANDROID_MEMORY_WATCH_GOT_HOOK_H
