#pragma once

// 进程 / 线程 / 执行 / 文件写 / 文件删 / 网络 六类动作的 seccomp 过滤器。
//
// 设计要点：
//   1. 六条规则彼此独立，由调用方按 SandboxConfig 逐位决定。
//   2. filter 由 prctl(PR_SET_SECCOMP) 装在**当前线程**上：只影响本线程及其后代，
//      不能撤销、不能卸载，因此调用方必须把装载动作放在一次性的线程里。
//   3. 只做"值级"判断（syscall 号 + 寄存器里的立即数），因为 BPF 不能解引用指针，
//      也就是**看不到路径字符串**。所以：
//        - 能拦：execve/execveat、clone 系、open/openat 的 flags、unlink/rename/mkdir 等、
//                socket 的 family、connect；
//        - 拦不了：按路径判断的读写、openat2（flags 在指针里，且内核 5.6+ 才有）。
//          这些交给 Java 层的 hook（libcore.io.BlockGuardOs）。
//   4. execve 与 execveat 都要拦：内核 3.19 起就有 execveat，只拦 execve 会留下真实绕过口。

#include <cerrno>
#include <cstddef>
#include <fcntl.h>
#include <vector>

#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <sched.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <sys/syscall.h>

#ifndef PR_SET_NO_NEW_PRIVS
#define PR_SET_NO_NEW_PRIVS 38
#endif

#ifndef CLONE_VM
#define CLONE_VM 0x00000100
#endif

#ifndef SECCOMP_RET_ERRNO
#define SECCOMP_RET_ERRNO 0x00050000U
#endif

// NDK 的 <linux/audit.h> 在不同架构上暴露的宏名不一致（x86 只有 AUDIT_ARCH_I386，
// 内核头里叫 AUDIT_ARCH_IA32），这里统一补齐。
#ifndef AUDIT_ARCH_ARM
#define AUDIT_ARCH_ARM 0x40000028
#endif

#ifndef AUDIT_ARCH_AARCH64
#define AUDIT_ARCH_AARCH64 0xC00000B7
#endif

#ifndef AUDIT_ARCH_IA32
#define AUDIT_ARCH_IA32 0x40000003
#endif

#ifndef AUDIT_ARCH_X86_64
#define AUDIT_ARCH_X86_64 0xC000003E
#endif

// syscall 号优先取 NDK 的 <sys/syscall.h>，取不到时回退到架构常量。
#ifndef __NR_clone3
#define __NR_clone3 435
#endif

#ifndef __NR_execveat
#if defined(__arm__)
#define __NR_execveat 387
#elif defined(__aarch64__)
#define __NR_execveat 281
#elif defined(__i386__)
#define __NR_execveat 358
#elif defined(__x86_64__)
#define __NR_execveat 322
#endif
#endif

namespace seccomp_policy {

    struct Policy {
        /** 拦截进程创建：clone（不带 CLONE_VM）、fork、vfork。 */
        bool blockProcessCreate = false;
        /** 拦截线程创建：clone（带 CLONE_VM）、clone3。 */
        bool blockThreadCreate = false;
        /** 拦截执行：execve、execveat。 */
        bool blockExec = false;
        /** 拦截新建/修改文件：带写标志的 open/openat、creat、truncate、rename、mkdir、chmod… */
        bool blockFileWrite = false;
        /** 拦截删除文件：unlink、unlinkat、rmdir。 */
        bool blockFileDelete = false;
        /** 拦截联网：AF_INET/AF_INET6 的 socket，以及 connect（含 Unix socket 的连接建立）。 */
        bool blockNetwork = false;

        bool any() const {
            return blockProcessCreate || blockThreadCreate || blockExec
                   || blockFileWrite || blockFileDelete || blockNetwork;
        }
    };

    /**
     * 位标志 —— Java 侧 {@code SeccompSandbox} 里有一份同样的常量，两边必须保持一致。
     *
     * <p>用位而不是一串 boolean：规则还会继续加，签名不用跟着改，也不会出现
     * {@code install(true, false, true, ...)} 这种看不出第几个是什么的调用。
     */
    enum Bits {
        BIT_PROCESS_CREATE = 1 << 0,
        BIT_THREAD_CREATE = 1 << 1,
        BIT_EXEC = 1 << 2,
        BIT_FILE_WRITE = 1 << 3,
        BIT_FILE_DELETE = 1 << 4,
        BIT_NETWORK = 1 << 5,
    };

    inline Policy policyFromBits(int bits) {
        Policy p;
        p.blockProcessCreate = (bits & BIT_PROCESS_CREATE) != 0;
        p.blockThreadCreate = (bits & BIT_THREAD_CREATE) != 0;
        p.blockExec = (bits & BIT_EXEC) != 0;
        p.blockFileWrite = (bits & BIT_FILE_WRITE) != 0;
        p.blockFileDelete = (bits & BIT_FILE_DELETE) != 0;
        p.blockNetwork = (bits & BIT_NETWORK) != 0;
        return p;
    }

    /** 本机架构的 AUDIT_ARCH 值，0 表示不支持。 */
    inline unsigned int currentAuditArch() {
#if defined(__aarch64__)
        return AUDIT_ARCH_AARCH64;
#elif defined(__arm__)
        return AUDIT_ARCH_ARM;
#elif defined(__x86_64__)
        return AUDIT_ARCH_X86_64;
#elif defined(__i386__)
        return AUDIT_ARCH_IA32;
#else
        return 0;
#endif
    }

    namespace detail {

        inline sock_filter stmt(unsigned short code, unsigned int k) {
            sock_filter f;
            f.code = code;
            f.jt = 0;
            f.jf = 0;
            f.k = k;
            return f;
        }

        inline sock_filter jump(unsigned short code, unsigned int k, unsigned char jt, unsigned char jf) {
            sock_filter f;
            f.code = code;
            f.jt = jt;
            f.jf = jf;
            f.k = k;
            return f;
        }

        inline sock_filter retAction(unsigned int action, int err) {
            return stmt(BPF_RET | BPF_K,
                        action | (static_cast<unsigned int>(err) & SECCOMP_RET_DATA));
        }

        inline sock_filter allow() {
            return stmt(BPF_RET | BPF_K, SECCOMP_RET_ALLOW);
        }

        /**
         * 发一条 {@code syscall == nr ? 走 block : 跳过 block} 的守卫。
         *
         * <p>BPF 只能往前跳、偏移是手工填的字节数 —— 直接写数字极容易错（而且一错就是
         * 静默放行或内核拒绝加载）。这里先占位、等 block 发完再回填，把它变成结构化代码。
         *
         * <p>约定：block 内部的"放行/继续判断"分支直接落到 block 末尾即可，
         * 那里正好是下一个守卫的入口。
         */
        template<typename BlockEmitter>
        inline void guard(std::vector<sock_filter> *f, long nr, BlockEmitter emitBlock) {
            size_t jeqAt = f->size();
            f->push_back(jump(BPF_JMP | BPF_JEQ | BPF_K, static_cast<unsigned int>(nr), 0, 0));
            size_t blockStart = f->size();
            emitBlock();
            (*f)[jeqAt].jf = static_cast<unsigned char>(f->size() - blockStart);
        }

        /** 无条件拦截某个 syscall。 */
        inline void guardBlock(std::vector<sock_filter> *f, long nr, int err) {
            guard(f, nr, [&]() { f->push_back(retAction(SECCOMP_RET_ERRNO, err)); });
        }

        /** open/openat 的写标志：命中任意一位就算"要写文件"。O_RDONLY == 0 天然不命中。 */
        inline unsigned int writeOpenFlags() {
            return O_WRONLY | O_RDWR | O_CREAT | O_TRUNC | O_APPEND;
        }

        /** 新建/修改文件类的 syscall（无条件拦；按路径区分的读不在其中）。 */
        inline std::vector<long> fileWriteSyscalls() {
            std::vector<long> v;
#ifdef __NR_creat
            v.push_back(__NR_creat);
#endif
#ifdef __NR_truncate
            v.push_back(__NR_truncate);
#endif
#ifdef __NR_ftruncate
            v.push_back(__NR_ftruncate);
#endif
#ifdef __NR_rename
            v.push_back(__NR_rename);
#endif
#ifdef __NR_renameat
            v.push_back(__NR_renameat);
#endif
#ifdef __NR_renameat2
            v.push_back(__NR_renameat2);
#endif
#ifdef __NR_mkdir
            v.push_back(__NR_mkdir);
#endif
#ifdef __NR_mkdirat
            v.push_back(__NR_mkdirat);
#endif
#ifdef __NR_chmod
            v.push_back(__NR_chmod);
#endif
#ifdef __NR_fchmod
            v.push_back(__NR_fchmod);
#endif
#ifdef __NR_fchmodat
            v.push_back(__NR_fchmodat);
#endif
#ifdef __NR_symlink
            v.push_back(__NR_symlink);
#endif
#ifdef __NR_symlinkat
            v.push_back(__NR_symlinkat);
#endif
#ifdef __NR_link
            v.push_back(__NR_link);
#endif
#ifdef __NR_linkat
            v.push_back(__NR_linkat);
#endif
#ifdef __NR_mknod
            v.push_back(__NR_mknod);
#endif
#ifdef __NR_mknodat
            v.push_back(__NR_mknodat);
#endif
#ifdef __NR_utimensat
            v.push_back(__NR_utimensat);
#endif
#ifdef __NR_utimes
            v.push_back(__NR_utimes);
#endif
#ifdef __NR_fallocate
            v.push_back(__NR_fallocate);
#endif
            return v;
        }

        /** 删除类 syscall。 */
        inline std::vector<long> fileDeleteSyscalls() {
            std::vector<long> v;
#ifdef __NR_unlink
            v.push_back(__NR_unlink);
#endif
#ifdef __NR_unlinkat
            v.push_back(__NR_unlinkat);
#endif
#ifdef __NR_rmdir
            v.push_back(__NR_rmdir);
#endif
            return v;
        }

        /** open/openat 的 flags 位置：open(path, flags, …) 是 args[1]；openat(dirfd, path, flags, …) 是 args[2]。 */
        inline void guardOpenFlags(std::vector<sock_filter> *f, long nr, int argIndex, int err) {
            // 偏移用算术算出来，不依赖 offsetof(struct, args[运行时下标]) 这种编译器扩展
            const unsigned int flagsOffset = static_cast<unsigned int>(
                    offsetof(seccomp_data, args[0]) + argIndex * sizeof(__u64));
            guard(f, nr, [&]() {
                f->push_back(stmt(BPF_LD | BPF_W | BPF_ABS, flagsOffset));
                // JSET: 命中写标志 → 落到下面的 RET；否则跳过 RET（1 条）
                f->push_back(jump(BPF_JMP | BPF_JSET | BPF_K, writeOpenFlags(), 0, 1));
                f->push_back(retAction(SECCOMP_RET_ERRNO, err));
            });
        }

        /** 只拦"上网"用的地址族，AF_UNIX / AF_NETLINK 等本地通信照常放行。 */
        inline void guardSocketFamily(std::vector<sock_filter> *f, long nr, int err) {
            guard(f, nr, [&]() {
                f->push_back(stmt(BPF_LD | BPF_W | BPF_ABS,
                                  static_cast<unsigned int>(offsetof(seccomp_data, args[0]))));
                f->push_back(jump(BPF_JMP | BPF_JEQ | BPF_K, AF_INET, 0, 1));
                f->push_back(retAction(SECCOMP_RET_ERRNO, err));
                f->push_back(jump(BPF_JMP | BPF_JEQ | BPF_K, AF_INET6, 0, 1));
                f->push_back(retAction(SECCOMP_RET_ERRNO, err));
            });
        }

    } // namespace detail

    /**
     * 构建 ERRNO 过滤器。
     *
     * @param p   策略
     * @param err 命中时返回给调用方的 errno（通常是 EPERM）
     * @param out 输出的 BPF 程序
     * @return 是否构建成功（架构不支持或策略全关时为 false）
     */
    inline bool buildErrnoFilter(const Policy &p, int err, std::vector<sock_filter> *out) {
        const unsigned int arch = currentAuditArch();
        if (arch == 0 || !p.any()) return false;

        using namespace detail;
        std::vector<sock_filter> &f = *out;
        f.clear();

        // 架构不匹配一律放行（本进程自己装的 filter，不存在跨架构执行）
        f.push_back(stmt(BPF_LD | BPF_W | BPF_ABS, offsetof(seccomp_data, arch)));
        f.push_back(jump(BPF_JMP | BPF_JEQ | BPF_K, arch, 1, 0));
        f.push_back(allow());
        f.push_back(stmt(BPF_LD | BPF_W | BPF_ABS, offsetof(seccomp_data, nr)));

        // ---------- 执行 ----------
        if (p.blockExec) {
            guardBlock(&f, __NR_execve, err);
            guardBlock(&f, __NR_execveat, err);
        }

        // ---------- 进程 / 线程 ----------
        if (p.blockProcessCreate || p.blockThreadCreate) {
            // clone 靠 CLONE_VM 区分：无 VM = 进程，有 VM = 线程
            guard(&f, __NR_clone, [&]() {
                f.push_back(stmt(BPF_LD | BPF_W | BPF_ABS, offsetof(seccomp_data, args[0])));
                f.push_back(jump(BPF_JMP | BPF_JSET | BPF_K, CLONE_VM, 1, 0));
                f.push_back(p.blockProcessCreate ? retAction(SECCOMP_RET_ERRNO, err) : allow());
                f.push_back(p.blockThreadCreate ? retAction(SECCOMP_RET_ERRNO, err) : allow());
            });
            // 注意：arm64 没有独立的老派 fork/vfork syscall（bionic 用 clone 实现），
            // 因此这两条必须按宏存在与否来装 —— 否则 arm64 上直接编不过。
#ifdef __NR_fork
            if (p.blockProcessCreate) guardBlock(&f, __NR_fork, err);
#endif
#ifdef __NR_vfork
            if (p.blockProcessCreate) guardBlock(&f, __NR_vfork, err);
#endif
            // clone3 的 flags 在指针里，BPF 读不到，无法区分线程/进程 → 只按"线程"语义处理
            if (p.blockThreadCreate) guardBlock(&f, __NR_clone3, err);
        }

        // ---------- 文件写 / 删 ----------
        if (p.blockFileWrite) {
#ifdef __NR_open
            guardOpenFlags(&f, __NR_open, 1, err);
#endif
#ifdef __NR_openat
            guardOpenFlags(&f, __NR_openat, 2, err);
#endif
            for (long nr : fileWriteSyscalls()) guardBlock(&f, nr, err);
        }
        if (p.blockFileDelete) {
            for (long nr : fileDeleteSyscalls()) guardBlock(&f, nr, err);
        }

        // ---------- 网络 ----------
        if (p.blockNetwork) {
            guardSocketFamily(&f, __NR_socket, err);
            // connect 的 sockaddr 是指针，看不到地址族，只能一律拦；
            // 代价是脚本线程里新建 Unix socket 连接也会被拦（正在用的连接是 write，不受影响）
            guardBlock(&f, __NR_connect, err);
        }

        f.push_back(allow());
        return true;
    }

    /**
     * 把过滤器装到**当前线程**（含其后代）。
     *
     * 注意：一旦装上就无法卸载，只能继续叠加。给的 errno 会原样返回给被拦的调用方。
     *
     * @return 0 成功；失败时为 errno
     */
    inline int installErrnoFilter(const Policy &p, int err) {
        if (!p.any()) return 0;

        std::vector<sock_filter> filter;
        if (!buildErrnoFilter(p, err, &filter)) {
            return ENOTSUP;
        }

        sock_fprog prog;
        prog.len = static_cast<unsigned short>(filter.size());
        prog.filter = filter.data();

        if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
            return errno;
        }
        if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) == -1) {
            return errno;
        }
        return 0;
    }

} // namespace seccomp_policy
