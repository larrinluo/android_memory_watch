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

char trace_file[PATH_MAX];
int outputMode = ANR_REDIRECT;

// hook methcods
int (*origin_open)(const char* pathname,int flags,mode_t mode);
ssize_t (*origin_write)(int fd, const void *buf, size_t count);
int (*origin_close)(int fd);
int (*origin_connect)(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
ssize_t (*origin_recvmsg)(int sockfd, struct msghdr *msg, int flags);

int tombstone_client_socket = -1;
int tombstone_fd = -1;
int my_trace_fd = -1;

#define TAG "TRACE_HOOK"

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
int my_open(const char* pathname,int flags,mode_t mode) {

    if (strcmp(ANR_TRACE_FILE, pathname) == 0) {
        if (outputMode == ANR_REDIRECT) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "redirect trace to :%s", trace_file);
            return origin_open(trace_file, flags, mode);
        } else {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "lately, copy trace to: %s", trace_file);
            tombstone_fd =  origin_open(pathname, flags, mode);
            return tombstone_fd;
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "open file : %s", pathname);
    return origin_open(pathname, flags, mode);
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
int my_connect(int sockfd, const struct sockaddr *addr, socklen_t addrlen) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "connect hooked --> %d", addr->sa_family);

    if (addr->sa_family == AF_LOCAL) {
        // 检查是否tombstone连接
        struct sockaddr_un *p_addr = (struct sockaddr_un *)addr;
        if (strncmp(TOMBSTONE_CLIENT_SOCKET, p_addr->sun_path, strlen(TOMBSTONE_CLIENT_SOCKET)) == 0) {
            tombstone_client_socket = sockfd;
        }
    }

    return origin_connect(sockfd, addr, addrlen);
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
ssize_t my_recvmsg(int sockfd, struct msghdr *msg, int flags) {
    ssize_t s = origin_recvmsg(sockfd, msg, flags);
    if (sockfd == tombstone_client_socket) {\
        struct cmsghdr* cmsg;
        for (cmsg = CMSG_FIRSTHDR(msg); cmsg != nullptr; cmsg = CMSG_NXTHDR(msg, cmsg)) {
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

                my_trace_fd = open(trace_file, O_APPEND | O_CREAT | O_WRONLY, 0666);
                if (my_trace_fd != -1) {
                    if (outputMode == ANR_REDIRECT) {
                        close(cmsg_fds[0]);
                        cmsg_fds[0] = my_trace_fd;
                        my_trace_fd = -1;
                        reportAnr();
                    } else {
                        tombstone_fd = cmsg_fds[0];
                        __android_log_print(ANDROID_LOG_DEBUG, TAG, "lately, copy trace to: %s", trace_file);
                    }
                }
            }
        }

    }
    return s;
}

ssize_t my_write(int fd, const void *buf, size_t count) {

    int bytes = origin_write(fd, buf, count);

    if (fd == tombstone_fd && my_trace_fd != -1) {
        int c = 0;
        while (c < bytes) {
            int b = origin_write(my_trace_fd, buf, bytes - c);
            if (b <= 0) {
                __android_log_print(ANDROID_LOG_WARN, TAG, "copy trace file failed: %s", trace_file);
                origin_close(my_trace_fd);
                my_trace_fd = -1;
                break;
            }
            c += b;
        }
    }

    return bytes;
}

int my_close(int fd) {
    if (fd == tombstone_fd && my_trace_fd != -1) {
        origin_close(my_trace_fd);
        my_trace_fd = -1;
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "copy trace file  done: %s", trace_file);
        reportAnr();
    }

    return origin_close(fd);
}

void installHook(int sdkVersion, const char *path, int output_mode) {

    strncpy(trace_file, path, PATH_MAX - 1);
    outputMode = output_mode;
    if (outputMode != ANR_REDIRECT && outputMode != ANR_COPY) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "invalide anr output mode %d, keep ANR_REDIRECT mode", output_mode);
        outputMode = ANR_REDIRECT;
    }

    if (sdkVersion >= 27) { // android 8.1以上版本
        xhook_register(".*\\.so$", "connect", (void *) my_connect, (void **) &origin_connect);
        xhook_register(".*\\.so$", "recvmsg", (void *) my_recvmsg, (void **) &origin_recvmsg);
    } else { // android 8.1以下
        xhook_register(".*\\libart.so$", "open", (void *) my_open, (void **) &origin_open);
    }

    if (outputMode == ANR_COPY) {
        xhook_register(".*\\libart.so$", "write", (void *) my_write, (void **) &origin_write);
        xhook_register(".*\\libart.so$", "close", (void *) my_close, (void **) &origin_close);
    }

    xhook_refresh(false);

    int err = 0;
    if (outputMode == ANR_COPY) {
        err = (origin_write != NULL && origin_close != NULL) ? 0 : 1;
    }

    if (err == 0) {
        if (sdkVersion >= 28) {
            err = (origin_connect != NULL && origin_recvmsg != NULL) ? 0 : 1;
        } else {
            err = (origin_open != NULL) ? 0 : 1;
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "install result: %d", err);
}

void* reportAnrThread(void *param) {
    JNIEnv *env = NULL;//sEnv;
    bool isJavaThread = true;

    if (env == NULL) {
        isJavaThread = false;
        JNI::Jvm()->AttachCurrentThread(&env, NULL);
    }

    if (env == NULL)
        return NULL;

    if (JNI::gClassLoader && JNI::gLoadClassMethod) {
        jclass  clientClz = (jclass) env->CallObjectMethod(JNI::gClassLoader, JNI::gLoadClassMethod, env->NewStringUTF("com/ttphoto/resource/watch/sdk/client/AppWatchClient"));
        if (clientClz) {
            jmethodID onAnrMethod = env->GetStaticMethodID(clientClz, "onAnr", "()V");
            if (onAnrMethod) {
                env->CallStaticVoidMethod(clientClz, onAnrMethod);
            }

//            env->DeleteLocalRef(clientClz);
        }
    }

    if (!isJavaThread) {
        JNI::Jvm()->DetachCurrentThread();
    }

    return NULL;
}

void reportAnr() {
    pthread_t tid;
    pthread_create(&tid, NULL, reportAnrThread, NULL);
}