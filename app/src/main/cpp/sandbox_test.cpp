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
#include <csignal>

#define LOG_TAG "JustNothing[SandboxTest]"
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
    
    // clone: check CLONE_VM flag in args[0]
    // On 32-bit, args[0] low word is at offset args[0], high word at offset args[0]+4
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SYS_CLONE, 0, 5));
    filter.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, args[0])));
    filter.push_back(BPF_JUMP(BPF_JMP | BPF_JSET | BPF_K, CLONE_VM, 1, 0));
    
    // No CLONE_VM = process creation (fork-like)
    if (block_process) {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM));
    } else {
        filter.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
    }
    
    // CLONE_VM = thread creation
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

    if (AUDIT_ARCH_CURRENT == 0 || SYS_EXECVE < 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        env->SetIntField(result, seccompErrorField, ENOTSUP);
        env->SetBooleanField(result, execveBlockedField, JNI_FALSE);
        LOGE("Architecture not supported for seccomp testing");
        return result;
    }

    int pipefd[2];
    if (pipe(pipefd) == -1) {
        LOGE("pipe() failed: %d", errno);
        return result;
    }

    pid_t test_pid = fork();
    
    if (test_pid == -1) {
        LOGE("fork for seccomp test failed: %d", errno);
        close(pipefd[0]);
        close(pipefd[1]);
        return result;
    } else if (test_pid == 0) {
        close(pipefd[0]);
        
        int result_data[3] = {0, 0, 0};
        
        if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == 0) {
            result_data[0] = 1;
            LOGI("prctl(PR_SET_NO_NEW_PRIVS) succeeded");
        } else {
            result_data[1] = errno;
            LOGE("prctl(PR_SET_NO_NEW_PRIVS) failed: %d", errno);
            write(pipefd[1], result_data, sizeof(result_data));
            close(pipefd[1]);
            _exit(1);
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
            result_data[2] = 1;
            LOGI("prctl(PR_SET_SECCOMP) succeeded");
        } else {
            result_data[1] = errno;
            LOGE("prctl(PR_SET_SECCOMP) failed: %d", errno);
            write(pipefd[1], result_data, sizeof(result_data));
            close(pipefd[1]);
            _exit(2);
        }

        write(pipefd[1], result_data, sizeof(result_data));
        close(pipefd[1]);

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
        close(pipefd[1]);
        
        int result_data[3] = {0, 0, 0};
        read(pipefd[0], result_data, sizeof(result_data));
        close(pipefd[0]);
        
        env->SetBooleanField(result, prctlSuccessField, result_data[0] ? JNI_TRUE : JNI_FALSE);
        env->SetIntField(result, prctlErrorField, result_data[1]);
        env->SetBooleanField(result, seccompSuccessField, result_data[2] ? JNI_TRUE : JNI_FALSE);
        if (!result_data[2]) {
            env->SetIntField(result, seccompErrorField, result_data[1]);
        }

        int status;
        waitpid(test_pid, &status, 0);
        
        bool blocked = false;
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

        env->SetBooleanField(result, execveBlockedField, blocked ? JNI_TRUE : JNI_FALSE);
    }

    return result;
}

struct CloneForkChildResult {
    int step;
    int filter_error;
    int fork_blocked;
    int fork_errno;
    int thread_blocked;
    int thread_error;
};

static void reset_signal_handlers() {
    signal(SIGSEGV, SIG_DFL);
    signal(SIGBUS, SIG_DFL);
    signal(SIGABRT, SIG_DFL);
    signal(SIGFPE, SIG_DFL);
    signal(SIGPIPE, SIG_DFL);
    signal(SIGTRAP, SIG_DFL);
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

    int pipefd[2];
    if (pipe(pipefd) == -1) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        char buf[256];
        snprintf(buf, sizeof(buf), "pipe() failed: %d", errno);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        return result;
    }

    pid_t test_pid = fork();

    if (test_pid == -1) {
        close(pipefd[0]);
        close(pipefd[1]);
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        char buf[256];
        snprintf(buf, sizeof(buf), "fork() for test failed: %d", errno);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        return result;
    }

    if (test_pid == 0) {
        close(pipefd[0]);

        reset_signal_handlers();

        CloneForkChildResult child_result = {};
        child_result.step = 0;
        child_result.filter_error = 0;
        child_result.fork_blocked = 0;
        child_result.fork_errno = 0;
        child_result.thread_blocked = 0;
        child_result.thread_error = 0;

        int filter_result = install_clone_blocking_filter(blockThread, blockProcess);
        child_result.step = 1;
        child_result.filter_error = filter_result;

        if (filter_result != 0) {
            write(pipefd[1], &child_result, sizeof(child_result));
            close(pipefd[1]);
            _exit(1);
        }

        write(pipefd[1], &child_result, sizeof(child_result));

        pid_t fork_result = fork();
        if (fork_result == -1) {
            if (errno == EPERM) {
                child_result.fork_blocked = 1;
                LOGI("fork was blocked by seccomp");
            } else {
                child_result.fork_errno = errno;
                LOGE("fork failed with errno: %d", errno);
            }
        } else if (fork_result == 0) {
            close(pipefd[1]);
            _exit(0);
        } else {
            int status;
            waitpid(fork_result, &status, 0);
        }

        child_result.step = 2;
        write(pipefd[1], &child_result, sizeof(child_result));

        pthread_t thread;
        int thread_result = pthread_create(&thread, nullptr, [](void*) -> void* { return nullptr; }, nullptr);
        if (thread_result == EPERM) {
            child_result.thread_blocked = 1;
            LOGI("pthread_create was blocked by seccomp");
        } else if (thread_result == 0) {
            pthread_join(thread, nullptr);
        } else {
            child_result.thread_error = thread_result;
            LOGE("pthread_create failed with error: %d", thread_result);
        }

        child_result.step = 3;
        write(pipefd[1], &child_result, sizeof(child_result));

        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);

    CloneForkChildResult last_result = {};
    CloneForkChildResult current_result = {};
    ssize_t bytes_read;

    while ((bytes_read = read(pipefd[0], &current_result, sizeof(current_result))) > 0) {
        if (bytes_read == sizeof(current_result)) {
            memcpy(&last_result, &current_result, sizeof(current_result));
        }
    }

    close(pipefd[0]);

    int status;
    waitpid(test_pid, &status, 0);

    int step = last_result.step;

    if (step == 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        if (WIFSIGNALED(status)) {
            char buf[256];
            snprintf(buf, sizeof(buf), "子进程在安装过滤器前崩溃 (signal %d)", WTERMSIG(status));
            env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        } else {
            env->SetObjectField(result, errorMsgField, env->NewStringUTF("子进程在安装过滤器前退出"));
        }
        return result;
    }

    if (step == 1 && last_result.filter_error != 0) {
        env->SetBooleanField(result, seccompSuccessField, JNI_FALSE);
        char buf[256];
        snprintf(buf, sizeof(buf), "过滤器安装失败: error=%d", last_result.filter_error);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        return result;
    }

    if (step < 2) {
        env->SetBooleanField(result, seccompSuccessField, JNI_TRUE);
        if (WIFSIGNALED(status)) {
            char buf[256];
            snprintf(buf, sizeof(buf), "子进程在 fork 测试期间崩溃 (signal %d)", WTERMSIG(status));
            env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        } else {
            env->SetObjectField(result, errorMsgField, env->NewStringUTF("子进程在 fork 测试期间异常退出"));
        }
        return result;
    }

    if (step < 3) {
        env->SetBooleanField(result, seccompSuccessField, JNI_TRUE);
        env->SetBooleanField(result, forkBlockedField, last_result.fork_blocked ? JNI_TRUE : JNI_FALSE);
        if (WIFSIGNALED(status)) {
            char buf[256];
            snprintf(buf, sizeof(buf), "子进程在 pthread_create 测试期间崩溃 (signal %d)", WTERMSIG(status));
            env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
        } else {
            env->SetObjectField(result, errorMsgField, env->NewStringUTF("子进程在 pthread_create 测试期间异常退出"));
        }
        return result;
    }

    env->SetBooleanField(result, seccompSuccessField, JNI_TRUE);
    env->SetBooleanField(result, forkBlockedField, last_result.fork_blocked ? JNI_TRUE : JNI_FALSE);
    env->SetBooleanField(result, threadBlockedField, last_result.thread_blocked ? JNI_TRUE : JNI_FALSE);

    if (last_result.fork_errno != 0 || last_result.thread_error != 0) {
        char buf[256];
        snprintf(buf, sizeof(buf), "fork_errno=%d, thread_error=%d", last_result.fork_errno, last_result.thread_error);
        env->SetObjectField(result, errorMsgField, env->NewStringUTF(buf));
    }

    LOGI("Test completed: fork_blocked=%d, thread_blocked=%d", last_result.fork_blocked, last_result.thread_blocked);

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
