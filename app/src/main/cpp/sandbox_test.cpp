#include <jni.h>
#include <cstring>
#include <cerrno>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <android/log.h>
#include <sched.h>
#include <pthread.h>
#include <cstdlib>
#include <vector>

#define LOG_TAG "SandboxTest"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef PR_SET_NO_NEW_PRIVS
#define PR_SET_NO_NEW_PRIVS 38
#endif

#ifndef PR_SET_SECCOMP
#define PR_SET_SECCOMP 22
#endif

#ifndef SECCOMP_MODE_FILTER
#define SECCOMP_MODE_FILTER 2
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

static int install_execve_blocking_filter() {
    if (AUDIT_ARCH_CURRENT == 0) {
        LOGE("Architecture not supported");
        return -3;
    }

    struct sock_filter filter[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_CURRENT, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
        
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_EXECVE, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM),

        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };

    struct sock_fprog prog = {
        .len = sizeof(filter) / sizeof(filter[0]),
        .filter = filter,
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %d", errno);
        return -1;
    }

    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) == -1) {
        LOGE("prctl(PR_SET_SECCOMP) failed: %d", errno);
        return -2;
    }

    return 0;
}

static int install_clone_blocking_filter(bool block_thread, bool block_process) {
    if (AUDIT_ARCH_CURRENT == 0) {
        LOGE("Architecture not supported");
        return -3;
    }

    std::vector<struct sock_filter> filter;
    
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)));
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_CURRENT, 1, 0));
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)));
    
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_CLONE, 0, 6));
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, args[0])));
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JSET | BPF_K, CLONE_VM, 1, 0));
    
    if (block_process) {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM));
    } else {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    }
    
    if (block_thread) {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM));
    } else {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    }
    
    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_FORK, 0, 1));
    if (block_process) {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM));
    } else {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    }

    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_VFORK, 0, 1));
    if (block_process) {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM));
    } else {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    }

    filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    struct sock_fprog prog = {
        .len = static_cast<unsigned short>(filter.size()),
        .filter = filter.data(),
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %d", errno);
        return -1;
    }

    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) == -1) {
        LOGE("prctl(PR_SET_SECCOMP) failed: %d", errno);
        return -2;
    }

    return 0;
}

extern "C" {

JNIEXPORT jobject JNICALL
Java_com_justnothing_testmodule_command_functions_tests_SandboxTestMain_testSeccompNative(
        JNIEnv *env, jobject thiz) {

    jclass resultClass = env->FindClass("com/justnothing/testmodule/command/functions/tests/SandboxTestMain$SeccompTestResult");
    if (resultClass == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "()V");
    if (constructor == nullptr) {
        return nullptr;
    }

    jobject result = env->NewObject(resultClass, constructor);
    if (result == nullptr) {
        return nullptr;
    }

    jfieldID prctlSuccessField = env->GetFieldID(resultClass, "prctlSuccess", "Z");
    jfieldID prctlErrorField = env->GetFieldID(resultClass, "prctlError", "I");
    jfieldID seccompSuccessField = env->GetFieldID(resultClass, "seccompSuccess", "Z");
    jfieldID seccompErrorField = env->GetFieldID(resultClass, "seccompError", "I");
    jfieldID execveBlockedField = env->GetFieldID(resultClass, "execveBlocked", "Z");

    if (prctlSuccessField == nullptr || prctlErrorField == nullptr ||
        seccompSuccessField == nullptr || seccompErrorField == nullptr ||
        execveBlockedField == nullptr) {
        return nullptr;
    }

    int saved_errno = 0;

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == 0) {
        env->SetBooleanField(result, prctlSuccessField, JNI_TRUE);
        LOGI("prctl(PR_SET_NO_NEW_PRIVS) succeeded");
    } else {
        saved_errno = errno;
        env->SetBooleanField(result, prctlSuccessField, JNI_FALSE);
        env->SetIntField(result, prctlErrorField, saved_errno);
        LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %d", saved_errno);
        return result;
    }

    if (AUDIT_ARCH_CURRENT == 0 || SYS_EXECVE < 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        env->SetIntField(result, seccompErrorField, ENOTSUP);
        env->SetBooleanField(result, execveBlockedField, JNI_FALSE);
        LOGE("Architecture not supported for seccomp testing");
        return result;
    }

    struct sock_filter test_filter[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_CURRENT, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };

    struct sock_fprog test_prog = {
        .len = sizeof(test_filter) / sizeof(test_filter[0]),
        .filter = test_filter,
    };

    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &test_prog) == 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_TRUE);
        LOGI("prctl(PR_SET_SECCOMP) succeeded");
    } else {
        saved_errno = errno;
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        env->SetIntField(result, seccompErrorField, saved_errno);
        LOGE("prctl(PR_SET_SECCOMP) failed: %d", saved_errno);
        return result;
    }

    bool blocked = false;
    pid_t test_pid = fork();
    
    if (test_pid == -1) {
        LOGE("fork for seccomp test failed: %d", errno);
        blocked = false;
    } else if (test_pid == 0) {
        if (install_execve_blocking_filter() == 0) {
            char *argv[] = { (char*)"/system/bin/echo", (char*)"test", nullptr };
            char *envp[] = { nullptr };
            execve("/system/bin/echo", argv, envp);
            int err = errno;
            if (err == EPERM) {
                _exit(42);
            }
            _exit(err);
        }
        _exit(1);
    } else {
        int status;
        waitpid(test_pid, &status, 0);
        
        if (WIFEXITED(status)) {
            int exit_code = WEXITSTATUS(status);
            if (exit_code == 42) {
                blocked = true;
                LOGI("execve was blocked by seccomp (exit code 42)");
            } else {
                LOGI("execve was not blocked (exit code %d)", exit_code);
            }
        } else {
            LOGE("Child process did not exit normally");
        }
    }

    env->SetBooleanField(result, execveBlockedField, blocked ? JNI_TRUE : JNI_FALSE);

    return result;
}

JNIEXPORT jobject JNICALL
Java_com_justnothing_testmodule_command_functions_tests_SandboxTestMain_testCloneForkBlocking(
        JNIEnv *env, jobject thiz, jboolean blockThread, jboolean blockProcess) {

    jclass resultClass = env->FindClass("com/justnothing/testmodule/command/functions/tests/SandboxTestMain$CloneForkTestResult");
    if (resultClass == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "()V");
    if (constructor == nullptr) {
        return nullptr;
    }

    jobject result = env->NewObject(resultClass, constructor);
    if (result == nullptr) {
        return nullptr;
    }

    jfieldID seccompSuccessField = env->GetFieldID(resultClass, "seccompSuccess", "Z");
    jfieldID forkBlockedField = env->GetFieldID(resultClass, "forkBlocked", "Z");
    jfieldID threadBlockedField = env->GetFieldID(resultClass, "threadBlocked", "Z");
    jfieldID errorMsgField = env->GetFieldID(resultClass, "errorMsg", "Ljava/lang/String;");

    if (seccompSuccessField == nullptr || forkBlockedField == nullptr ||
        threadBlockedField == nullptr || errorMsgField == nullptr) {
        return nullptr;
    }

    if (AUDIT_ARCH_CURRENT == 0 || SYS_CLONE < 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF("Architecture not supported"));
        return result;
    }

    pid_t test_pid = fork();
    
    if (test_pid == -1) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        char buf[256];
        snprintf(buf, sizeof(buf), "fork for test failed: %d", errno);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        return result;
    }
    
    if (test_pid == 0) {
        if (install_clone_blocking_filter(blockThread, blockProcess) != 0) {
            _exit(1);
        }
        
        bool fork_blocked = false;
        bool thread_blocked = false;
        
        pid_t fork_result = fork();
        if (fork_result == -1) {
            if (errno == EPERM) {
                fork_blocked = true;
                LOGI("fork was blocked by seccomp");
            } else {
                LOGE("fork failed with errno: %d", errno);
            }
        } else if (fork_result == 0) {
            _exit(0);
        } else {
            int status;
            waitpid(fork_result, &status, 0);
        }
        
        pthread_t thread;
        int thread_result = pthread_create(&thread, nullptr, [](void*) -> void* { return nullptr; }, nullptr);
        if (thread_result == EPERM) {
            thread_blocked = true;
            LOGI("pthread_create was blocked by seccomp");
        } else if (thread_result == 0) {
            pthread_join(thread, nullptr);
        } else {
            LOGE("pthread_create failed with error: %d", thread_result);
        }
        
        int exit_code = (fork_blocked ? 1 : 0) | (thread_blocked ? 2 : 0);
        _exit(exit_code);
    }
    
    int status;
    waitpid(test_pid, &status, 0);
    
    if (WIFEXITED(status)) {
        int exit_code = WEXITSTATUS(status);
        env->SetBooleanField(result, seccompSuccessField, JNI_TRUE);
        env->SetBooleanField(result, forkBlockedField, (exit_code & 1) ? JNI_TRUE : JNI_FALSE);
        env->SetBooleanField(result, threadBlockedField, (exit_code & 2) ? JNI_TRUE : JNI_FALSE);
        LOGI("Test completed: fork_blocked=%d, thread_blocked=%d", exit_code & 1, (exit_code >> 1) & 1);
    } else {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF("Child process did not exit normally"));
    }

    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_justnothing_testmodule_command_functions_tests_SandboxTestMain_testExecveBlocked(
        JNIEnv *env, jobject thiz) {
    
    pid_t pid = fork();
    
    if (pid == -1) {
        if (errno == EPERM || errno == EACCES) {
            return JNI_TRUE;
        }
        LOGE("fork failed: %d", errno);
        return JNI_FALSE;
    }
    
    if (pid == 0) {
        _exit(0);
    }
    
    int status;
    waitpid(pid, &status, 0);
    
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_justnothing_testmodule_command_functions_tests_SandboxTestMain_strerror(
        JNIEnv *env, jobject thiz, jint errnum) {
    char buf[1024];
    const char *msg = strerror_r(errnum, buf, sizeof(buf));
    return env->NewStringUTF(msg);
}

}
