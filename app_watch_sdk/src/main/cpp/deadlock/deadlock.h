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
// Created by larrin luo on 2020-01-23.
//

#ifndef ANDROID_MEMORY_WATCH_DEADLOCK_H
#define ANDROID_MEMORY_WATCH_DEADLOCK_H

#include "../hook/got_hook.h"
#include <map>
#include <vector>


#define READ_LOCK_CACHE_SIZE 32

struct MUTEX_TYPE {

    enum {
        UNKNOWN,
        MUTEX,
        RW_LOCK,
    };
};

struct RdLockInfo;
class DeadLock;

struct BlockedMutex {
    void * mutex;           // mutex
    int type;               // mutex类型
    int owner_thread;       // 锁定持有者线程
    int blocked_thread;     // 等待的线程

    std::string reportMsg;  // 上报消息

    bool dumped;            // 发生死锁时，blockedThread是否dump了死锁信息，每个死锁线程dump一次

    BlockedMutex():mutex(NULL),
                   type(MUTEX_TYPE::UNKNOWN),
                   owner_thread(0),
                   blocked_thread(0),
                   dumped(false)
    {
    }

    BlockedMutex(const BlockedMutex &other):
            mutex(other.mutex),
            type(other.type),
            owner_thread(other.owner_thread),
            blocked_thread(other.blocked_thread),
            dumped(other.dumped)
    {
    }
};

struct LockInfo {
    void *lock;
    int type;
    int owner;
    int deep;
    int recursive;
    long enter_time;

    LockInfo():owner(0), deep(0), recursive(0), enter_time(0) {
    }

    LockInfo(const LockInfo &rhs) {
        lock = rhs.lock;
        type = rhs.type;
        owner = rhs.owner;
        deep = rhs.deep;
        recursive = rhs.recursive;
        enter_time = rhs.enter_time;
    }
};

class RWLockInfo: public LockInfo {

    static pthread_mutex_t sCacheLock;
    static std::vector<RdLockInfo *> sCache;

    static RdLockInfo *obtainReadLock();
    static void releaseReadLock(RdLockInfo *lock);

    friend class DeadLock;

    std::atomic_flag m_readlocks_flag;

    struct FlagLocker
    {
        std::atomic_flag& m_flag;
        FlagLocker(std::atomic_flag& flag) : m_flag(flag)
        {
            while (m_flag.test_and_set()) {
                sched_yield();
            }
        }

        ~FlagLocker()
        {
            m_flag.clear();
        }
    };

public:

    std::vector<RdLockInfo *> readLocks;

    RdLockInfo * getReadLock(int tid);

    RWLockInfo(): LockInfo(), m_readlocks_flag(ATOMIC_FLAG_INIT) {
        type = MUTEX_TYPE::RW_LOCK;
    }

    ~RWLockInfo();

    void collect_block_items(int tid, std::vector<BlockedMutex> &items);
};

struct RdLockInfo: public LockInfo {
    int for_thread;
    RWLockInfo *parent;
    RdLockInfo(): LockInfo()  {
        type = MUTEX_TYPE::RW_LOCK;
    }
};


typedef std::map<void *, LockInfo *> LOCK_MAP;

class DeadLock {

    static pthread_mutex_t sBlockedMutexLock;
    static std::vector<BlockedMutex> sBlockedMutexes;

    static pthread_mutex_t sLock;
    static LOCK_MAP sLockMap;
    static int sdkVersion;

    // hooks
    static int my_pthread_mutex_lock(PthreadMutexLockContext &context);
    static int my_pthread_mutex_unlock(PthreadMutexUnlockContext &context);
    static int my_pthread_mutex_init(PthreadMutexInitContext &context);
    static int my_pthread_mutex_destroy(PthreadMutexDestroyContext &context);

    static int my_pthread_rwlock_init(PthreadRwLockInitContext &context);
    static int my_pthread_rwlock_destroy(PthreadRwLockDestroyContext &context);
    static int my_pthread_rwlock_rdlock(PthreadRWLockRDLockContext &context);
    static int my_pthread_rwlock_wrlock(PthreadRWLockWRLockContext &context);
    static int my_pthread_rwlock_unlock(PthreadRWLockUnlockContext &context);

    static inline LockInfo * getLock(void * _lock) {
        LockInfo *pLock;
        GotHook::origin_pthread_mutex_lock(&sLock);
        LOCK_MAP::iterator it = sLockMap.find(_lock);
        if (it != sLockMap.end()) {
            pLock =  it->second;
        } else {
            pLock = NULL;
        }
        GotHook::origin_pthread_mutex_unlock(&sLock);
        return pLock;
    }

    static int try_lock(LockInfo &lock, bool readLock = false);
    static int unlock(LockInfo &lock);

    // inner
    static int timed_lock(LockInfo& lock, int timeout, bool readLock);

    static void add_blocked_item(int blocked_thread, void *lock, int type, int owner_thread);
    static void remove_blocked_item(int blocked_thread);

    static void collect_blocked_items(int blocked_thread, LockInfo *lock, bool readLock);

    /**
     *
     * blockedByDeadLockLoop == true, 表示线程被block在一个死循环loop上，导致该线程不可能在恢复了。
     *
     * @param tid
     * @param deadlock_links
     * @param force
     * @param blockedByDeadLockThread
     */
    static void find_dead_locks(int tid, std::vector<std::vector<BlockedMutex>> &deadlock_links,
            bool force, bool &blockedByDeadLockLoop);

    static BlockedMutex* find_next_jump(BlockedMutex *from_mutex);

    friend class RWLockInfo;

public:

    /**
     * 选择监控指定so
     * 1. 出于性能考虑：   pthread_**方法使用过于频繁
     * 2. 出于稳定性考虑： hook方法不能嵌套，但由于pthread_**方法使用太广，很多工具方法都会用到
     *
     * @param sdkVersion
     * @param targetSo
     * @param path
     */
    static void registerHooks(int sdkVersion, const char *targetSo, const char *path);
    static void checkHooks();

};

#endif //ANDROID_MEMORY_WATCH_DEADLOCK_H
