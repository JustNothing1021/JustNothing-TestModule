#include <jni.h>

#include <cerrno>
#include <cstring>

#include "seccomp_policy.h"

// JNI 薄封装：真正的过滤器构造在 seccomp_policy.h 里，那份代码同时被真机探测器使用，
// 保证"测过的"和"跑起来的"是同一份逻辑。
extern "C" {

/**
 * 把策略装到当前线程（含其后代）。
 *
 * @param bits 规则位，取值见 {@code SeccompSandbox} 里的 BLOCK_* 常量
 * @return 0 成功；否则为 errno
 */
JNIEXPORT jint JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompSandbox_nativeInstall(
        JNIEnv *, jclass, jint bits) {
    return seccomp_policy::installErrnoFilter(seccomp_policy::policyFromBits(bits), EPERM);
}

/** errno → 可读文本。 */
JNIEXPORT jstring JNICALL
Java_com_justnothing_testmodule_utils_sandbox_SeccompSandbox_nativeStrerror(
        JNIEnv *env, jclass, jint err) {
    char buf[256];
    const char *msg = strerror_r(err, buf, sizeof(buf));
    return env->NewStringUTF(msg != nullptr ? msg : "unknown errno");
}

} // extern "C"
