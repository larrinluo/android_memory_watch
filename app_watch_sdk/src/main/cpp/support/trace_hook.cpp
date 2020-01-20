#include "trace_hook.h"
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include "xhook.h"
#include <android/log.h>
#include <sys/socket.h>
#include <sys/un.h>

#define TOMBSTONE_CLIENT_SOCKET "/dev/socket/tombstoned_java_trace"
#define ANR_TRACE_FILE "/data/anr/traces.txt"

char trace_file[PATH_MAX];

// hook methcods
int (*origin_open)(const char* pathname,int flags,mode_t mode);
ssize_t (*origin_write)(int fd, const void *buf, size_t count);
int (*origin_close)(int fd);
int (*origin_connect)(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
ssize_t (*origin_recvmsg)(int sockfd, struct msghdr *msg, int flags);

int tombstone_client_socket = -1;
int tombstone_fd = -1;

int my_open(const char* pathname,int flags,mode_t mode) {

    if (strcmp(ANR_TRACE_FILE, pathname) == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, "TRACE_HOOK", "dump trace --> to %s", trace_file);
        return origin_open(trace_file, flags, mode);
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "TRACE_HOOK", "open file : %s", pathname);
        return origin_open(pathname, flags, mode);
    }
}

int my_connect(int sockfd, const struct sockaddr *addr, socklen_t addrlen) {
    __android_log_print(ANDROID_LOG_DEBUG, "SOCKET_HOOK", "connect hooked --> %d", addr->sa_family);

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
                tombstone_fd = cmsg_fds[0];
                __android_log_print(ANDROID_LOG_DEBUG, "SOCKET_HOOK", "got tombstones fd from server %d", tombstone_fd);
            }
        }

    }
    return s;
}

ssize_t my_write(int fd, const void *buf, size_t count) {

    if (fd == tombstone_fd) {
        __android_log_print(ANDROID_LOG_DEBUG, "SOCKET_HOOK", "write tombstone%d", tombstone_fd);
    }

    return origin_write(fd, buf, count);
}

int my_close(int fd) {
    if (fd == tombstone_fd) {
        __android_log_print(ANDROID_LOG_DEBUG, "SOCKET_HOOK", "tombstones file closed %d", tombstone_fd);
    }

    return origin_close(fd);
}

void installHook(int sdkVersion) {

    int err;

    if (sdkVersion >= 21) {
        xhook_register(".*\\libart.so$", "open", (void *) my_open, (void **) &origin_open);
        xhook_register(".*\\.so$", "connect", (void *) my_connect, (void **) &origin_connect);
        xhook_register(".*\\.so$", "recvmsg", (void *) my_recvmsg, (void **) &origin_recvmsg);
        xhook_register(".*\\.so$", "write", (void *) my_write, (void **) &origin_write);
        xhook_register(".*\\.so$", "close", (void *) my_close, (void **) &origin_close);

        xhook_refresh(false);
        err = origin_open != NULL;
    } else {
        xhook_register(".*\\.so$",  "open", (void *) my_open,  (void **)&origin_open);
        xhook_refresh(false);
        err = origin_open != NULL ;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "TRACE_HOOK", "install result: %d", err);
}

void setTraceFile(const char *path) {
    strncpy(trace_file, path, PATH_MAX - 1);
}

