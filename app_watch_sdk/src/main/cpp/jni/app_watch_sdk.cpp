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
// Created by larrin on 20-1-22.
//

#include <jni.h>
#include <string>
#include <unistd.h>
#include "app_watch_sdk.h"
#include "../anr/trace_hook.h"
#include "../deadlock/deadlock.h"
#include <android/log.h>
#include <mutex>


pthread_mutex_t lock1;
pthread_mutex_t lock2;

pthread_rwlock_t rwLock1;
pthread_rwlock_t rwLock2;

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t  cond  = PTHREAD_COND_INITIALIZER;

void *deadlock_thread1(void *arg)
{
    time_t t;
    srand((unsigned) time(&t));
    while (1) {
        sleep(rand() % 2);
        pthread_mutex_lock(&lock2);
        sleep(2);
        pthread_mutex_lock(&lock1);
        __android_log_print(ANDROID_LOG_DEBUG, "TESTA", "thread 1 in lock section");

//        pthread_mutex_unlock(&lock1);
//        pthread_mutex_unlock(&lock2);
    }
}

void *deadlock_thread2(void *arg)
{
    time_t t;
    srand((unsigned) time(&t));

    while (1) {
        sleep(rand() % 2);
        pthread_mutex_lock(&lock1);
        sleep(2);
        pthread_mutex_lock(&lock2);
        __android_log_print(ANDROID_LOG_DEBUG, "TESTA", "thread 2 in lock section");
        sleep(2);
//        pthread_mutex_unlock(&lock2);
//        pthread_mutex_unlock(&lock1);
    }
}

void *deadlock_thread3(void *arg)
{
    time_t t;
    srand((unsigned) time(&t));
    while (1) {
        sleep(10);
        pthread_rwlock_wrlock(&rwLock1);
    }
}

void *deadlock_thread4(void *arg)
{
    time_t t;
    srand((unsigned) time(&t));

    while (1) {
        //sleep(rand() % 2)
        pthread_rwlock_rdlock(&rwLock1);
        sleep(2);
        pthread_rwlock_wrlock(&rwLock2);
        sleep(2);
        __android_log_print(ANDROID_LOG_DEBUG, "TESTA", "thread 4 in lock section");
        sleep(2);
//        pthread_mutex_unlock(&lock2);
//        pthread_mutex_unlock(&lock1);
    }
}

void *deadlock_thread5(void *arg)
{
    time_t t;
    srand((unsigned) time(&t));

    while (1) {
        pthread_rwlock_rdlock(&rwLock2);
        sleep(1);
        pthread_rwlock_wrlock(&rwLock1);
        sleep(2);
        __android_log_print(ANDROID_LOG_DEBUG, "TESTA", "thread 5 in lock section");
        sleep(2);
//        pthread_mutex_unlock(&lock2);
//        pthread_mutex_unlock(&lock1);
    }
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_WatchSDK_enableANRWatch(JNIEnv *env, jclass clazz,
                                                                   jint sdk_version,
                                                                   jstring output_path,
                                                                   jint output_mode) {

    const char *path = env->GetStringUTFChars(output_path, 0);
    if (path != NULL) {
        ANR::registerHooks(sdk_version, path, output_mode);
    }

    if (path)
        env->ReleaseStringUTFChars(output_path, path);

}

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_WatchSDK_enableDeadLockWatch(JNIEnv *env, jclass clazz,
                                                                        jint sdk_version,
                                                                        jstring target_so,
                                                                        jstring output_path) {
    const char *path = env->GetStringUTFChars(output_path, 0);
    const char *targetSo = env->GetStringUTFChars(target_so, 0);

    if (path != NULL && targetSo != NULL) {
        DeadLock::registerHooks(sdk_version, targetSo, path);
    }

    if (path)
        env->ReleaseStringUTFChars(output_path, path);

    if (targetSo)
        env->ReleaseStringUTFChars(target_so, targetSo);
}

JNIEXPORT void JNICALL
Java_com_ttphoto_resource_watch_sdk_client_WatchSDK_startWatch(JNIEnv *env, jclass clazz) {
    GotHook::installHooks();
    ANR::checkHooks();
    DeadLock::checkHooks();

    // dead lock test
    pthread_t tid;

    {
        std::mutex mutex;
        mutex.lock();
        mutex.unlock();
    }

    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutex_init(&lock1, &attr);
    pthread_mutex_init(&lock2, &attr);

    pthread_rwlock_init(&rwLock1, NULL);
    pthread_rwlock_init(&rwLock2, NULL);

//    pthread_mutex_lock(&lock1);
//    pthread_cond_wait(&cond, &lock1, &tout);

    pthread_create(&tid, NULL, deadlock_thread1, NULL);
    pthread_create(&tid, NULL, deadlock_thread2, NULL);

    pthread_create(&tid, NULL, deadlock_thread3, NULL);
    pthread_create(&tid, NULL, deadlock_thread4, NULL);
    pthread_create(&tid, NULL, deadlock_thread5, NULL);
}

} // extern "C"

