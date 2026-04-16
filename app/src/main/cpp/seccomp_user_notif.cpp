#include <jni.h>
#include <cstring>
#include <cerrno>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <android/log.h>
#include <pthread.h>
#include <cstdlib>
#include <vector>
#include <atomic>
#include <mutex>
#include <map>
#include <thread>

#define LOG_TAG "SeccompNotif"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#ifndef PR_SET_NO_NEW_PRIVS
#define PR_SET_NO_NEW_PRIVS 38
#endif

#ifndef PR_SET_SECCOMP
#define PR_SET_SECCOMP 22
#endif

#ifndef SECCOMP_MODE_FILTER
#define SECCOMP_MODE_FILTER 2
#endif

#ifndef SECCOMP_FILTER_FLAG_NEW_LISTENER
#define SECCOMP_FILTER_FLAG_NEW_LISTENER (1UL << 3)
#endif

#ifndef SECCOMP_RET_USER_NOTIF
#define SECCOMP_RET_USER_NOTIF 0x7fc00000U
#endif

#ifndef AUDIT_ARCH_X86_64
#define AUDIT_ARCH_X86_64 0xC000003E
#endif

#ifndef AUDIT_ARCH_I386
#define AUDIT_ARCH_I386 0x40000003
#endif

#ifndef AUDIT_ARCH_AARCH64
#define AUDIT_ARCH_AARCH64 0xC00000B7
#endif

#if defined(__x86_64__)
#define AUDIT_ARCH_CURRENT AUDIT_ARCH_X86_64
#define SYS_EXECVE 59
#define SYS_FORK 57
#define SYS_CLONE 56
#define SYS_VFORK 58
#elif defined(__i386__)
#define AUDIT_ARCH_CURRENT AUDIT_ARCH_I386
#define SYS_EXECVE 11
#define SYS_FORK 2
#define SYS_CLONE 120
#define SYS_VFORK 190
#elif defined(__aarch64__)
#define AUDIT_ARCH_CURRENT AUDIT_ARCH_AARCH64
#define SYS_EXECVE 221
#define SYS_FORK 220
#define SYS_CLONE 220
#define SYS_VFORK 220
#elif defined(__arm__)
#define AUDIT_ARCH_CURRENT 0x40000028
#define SYS_EXECVE 11
#define SYS_FORK 2
#define SYS_CLONE 120
#define SYS_VFORK 190
#else
#define AUDIT_ARCH_CURRENT 0
#define SYS_EXECVE 0xFFFFFFFF
#define SYS_FORK 0xFFFFFFFF
#define SYS_CLONE 0xFFFFFFFF
#define SYS_VFORK 0xFFFFFFFF
#endif

#ifndef CLONE_VM
#define CLONE_VM 0x00000100
#endif

static std::atomic<bool> g_handler_running(false);
static std::atomic<int> g_seccomp_fd(-1);
static std::thread g_notif_thread;
static JavaVM* g_jvm = nullptr;
static jclass g_handler_class = nullptr;

static std::map<int, bool> g_thread_permissions;
static std::mutex g_permissions_mutex;

static bool install_user_notif_filter() {
    if (AUDIT_ARCH_CURRENT == 0) {
        LOGE("Architecture not supported");
        return false;
    }

    std::vector<struct sock_filter> filter;
    
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)));
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_CURRENT, 1, 0));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)));
    
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_CLONE, 0, 2));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_USER_NOTIF));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_FORK, 0, 2));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_USER_NOTIF));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_VFORK, 0, 2));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_USER_NOTIF));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_EXECVE, 0, 2));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_USER_NOTIF));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    struct sock_fprog prog = {
        .len = static_cast<unsigned short>(filter.size()),
        .filter = filter.data(),
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %d", errno);
        return false;
    }

    int fd = syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_NEW_LISTENER, &prog);
    if (fd == -1) {
        LOGE("seccomp(SECCOMP_SET_MODE_FILTER, NEW_LISTENER) failed: %d", errno);
        return false;
    }

    g_seccomp_fd.store(fd);
    LOGI("USER_NOTIF filter installed, fd=%d", fd);
    return true;
}

static void notification_handler_loop() {
    LOGI("Notification handler thread started");
    
    JNIEnv* env = nullptr;
    bool need_detach = false;
    
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to attach thread to JVM");
            return;
        }
        need_detach = true;
    }

    int fd = g_seccomp_fd.load();
    if (fd < 0) {
        LOGE("Invalid seccomp fd");
        if (need_detach) g_jvm->DetachCurrentThread();
        return;
    }

    while (g_handler_running.load()) {
        struct seccomp_notif req = {};
        struct seccomp_notif_resp resp = {};
        
        ssize_t ret = ioctl(fd, SECCOMP_IOCTL_NOTIF_RECV, &req);
        if (ret < 0) {
            if (errno == EINTR) continue;
            LOGE("ioctl(NOTIF_RECV) failed: %d", errno);
            break;
        }

        LOGD("Received notification: id=%llu, pid=%d, nr=%d", 
             (unsigned long long)req.id, req.pid, req.data.nr);

        bool should_deny = false;
        
        {
            std::lock_guard<std::mutex> lock(g_permissions_mutex);
            auto it = g_thread_permissions.find(req.pid);
            if (it != g_thread_permissions.end()) {
                bool allowed = it->second;
                should_deny = !allowed;
                LOGD("Thread %d permission: allowed=%d, will deny=%d", 
                     req.pid, allowed, should_deny);
            } else {
                LOGD("Thread %d not in permission map, allowing by default", req.pid);
            }
        }

        resp.id = req.id;
        resp.error = should_deny ? EPERM : 0;
        resp.val = 0;
        resp.flags = 0;

        ret = ioctl(fd, SECCOMP_IOCTL_NOTIF_SEND, &resp);
        if (ret < 0) {
            LOGE("ioctl(NOTIF_SEND) failed: %d", errno);
        }
    }

    if (need_detach) {
        g_jvm->DetachCurrentThread();
    }
    
    LOGI("Notification handler thread stopped");
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeInit(
        JNIEnv *env, jclass clazz) {
    
    if (g_handler_running.load()) {
        LOGI("Handler already running");
        return JNI_TRUE;
    }

    env->GetJavaVM(&g_jvm);
    g_handler_class = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));

    if (!install_user_notif_filter()) {
        return JNI_FALSE;
    }

    g_handler_running.store(true);
    g_notif_thread = std::thread(notification_handler_loop);
    g_notif_thread.detach();

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeShutdown(
        JNIEnv *env, jclass clazz) {
    
    g_handler_running.store(false);
    
    int fd = g_seccomp_fd.exchange(-1);
    if (fd >= 0) {
        close(fd);
    }

    if (g_handler_class) {
        env->DeleteGlobalRef(g_handler_class);
        g_handler_class = nullptr;
    }

    LOGI("Handler shutdown complete");
}

JNIEXPORT jint JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeGetTid(
        JNIEnv *env, jclass clazz) {
    return gettid();
}

JNIEXPORT void JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeRegisterThread(
        JNIEnv *env, jclass clazz, jint tid, jboolean allowed) {
    std::lock_guard<std::mutex> lock(g_permissions_mutex);
    g_thread_permissions[tid] = allowed;
    LOGI("Registered thread %d: allowed=%d", tid, allowed);
}

JNIEXPORT void JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeUnregisterThread(
        JNIEnv *env, jclass clazz, jint tid) {
    std::lock_guard<std::mutex> lock(g_permissions_mutex);
    g_thread_permissions.erase(tid);
    LOGI("Unregistered thread %d", tid);
}

JNIEXPORT jboolean JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeInstallFilter(
        JNIEnv *env, jclass clazz) {
    return install_user_notif_filter() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeIsSupported(
        JNIEnv *env, jclass clazz) {
    if (AUDIT_ARCH_CURRENT == 0) {
        return JNI_FALSE;
    }

    struct sock_filter test_filter[] = {
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };

    struct sock_fprog test_prog = {
        .len = 1,
        .filter = test_filter,
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) not supported: %d", errno);
        return JNI_FALSE;
    }

    int fd = syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_NEW_LISTENER, &test_prog);
    if (fd == -1) {
        LOGE("SECCOMP_FILTER_FLAG_NEW_LISTENER not supported: %d", errno);
        return JNI_FALSE;
    }

    close(fd);
    LOGI("USER_NOTIF is supported");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompNotifHandler_nativeStrerror(
        JNIEnv *env, jclass clazz, jint errnum) {
    char buf[1024];
    const char* msg = strerror_r(errnum, buf, sizeof(buf));
    return env->NewStringUTF(msg);
}

}
