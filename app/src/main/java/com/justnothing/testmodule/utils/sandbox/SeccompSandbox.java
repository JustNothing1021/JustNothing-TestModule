package com.justnothing.testmodule.utils.sandbox;

import android.util.Log;

/**
 * seccomp 过滤器：把"不许建进程 / 不许建线程 / 不许执行命令"下沉到系统调用层。
 *
 * <p>三条规则彼此独立，全部来自 {@link com.justnothing.engine.security.SandboxConfig}
 * 的对应位；三条全关时不会装载任何过滤器（零开销）。
 *
 * <h3>为什么需要它</h3>
 * {@code Runtime.exec("reboot")} 这类调用在 Java 层可以有无数种写法（直接调、反射调、
 * 从别的类里调），但在内核里只有一条路：{@code clone}/{@code fork} + {@code execve}。
 * 打在这里，脚本写法的花样就没意义了。
 *
 * <h3>装载位置很关键</h3>
 * 过滤器是<b>按线程</b>生效的（装在谁身上就跟着谁），并且<b>装上就不能卸载</b>，
 * 只能继续叠加。所以：
 * <ul>
 *   <li>绝不能装在池化线程（线程池 worker）上 —— 那个 worker 之后永远不能再建进程/线程；</li>
 *   <li>由调用方保证这条线程是一次性的，见 {@link BlockGuardSandbox#execute}；</li>
 *   <li>脚本自己 fork/clone 出来的后代<b>会继承</b>过滤器，因此"任务"整体受限。</li>
 * </ul>
 *
 * <p>{@code SECCOMP_RET_USER_NOTIF}（动态决策版本）要求内核 5.0+，在 Android 8.1
 * （内核 4.9）上不可用，因此这里只用 ERRNO 模式。errno 统一返回 {@code EPERM}。
 */
public final class SeccompSandbox {

    private static final String TAG = "SeccompSandbox";

    /** 安装成功 */
    public static final int OK = 0;
    /** 本地库没加载上（ABI 缺失或加载失败） */
    public static final int ERR_NO_LIBRARY = -1;

    // ===== 规则位（与 native 的 seccomp_policy::Bits 必须一致）=====
    /** 拦截进程创建（clone 无 CLONE_VM / fork / vfork） */
    public static final int BLOCK_PROCESS_CREATE = 1;
    /** 拦截线程创建（clone 带 CLONE_VM / clone3） */
    public static final int BLOCK_THREAD_CREATE = 1 << 1;
    /** 拦截执行命令（execve / execveat） */
    public static final int BLOCK_EXEC = 1 << 2;
    /** 拦截新建/修改文件（带写标志的 open/openat、creat、truncate、rename、mkdir、chmod…） */
    public static final int BLOCK_FILE_WRITE = 1 << 3;
    /** 拦截删除文件（unlink / unlinkat / rmdir） */
    public static final int BLOCK_FILE_DELETE = 1 << 4;
    /** 拦截联网（AF_INET/AF_INET6 的 socket、connect；AF_UNIX 本地通信放行） */
    public static final int BLOCK_NETWORK = 1 << 5;

    private static final boolean LIBRARY_LOADED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("sandbox_seccomp");
            loaded = true;
            Log.i(TAG, "seccomp 本地库已加载");
        } catch (UnsatisfiedLinkError e) {
            loaded = false;
            Log.e(TAG, "seccomp 本地库加载失败: " + e.getMessage());
        }
        LIBRARY_LOADED = loaded;
    }

    private SeccompSandbox() {
    }

    public static boolean isLibraryLoaded() {
        return LIBRARY_LOADED;
    }

    /**
     * 把过滤器装到<b>当前线程</b>。
     *
     * <p>注意 {@link #BLOCK_FILE_WRITE} / {@link #BLOCK_FILE_DELETE} / {@link #BLOCK_NETWORK}
     * 这三条<b>没有 bypass 通道</b>：一旦装上，本线程（及其后代）里所有代码都会撞上 EPERM，
     * 包括你们自己的 Logger 写盘。要用它们就得先把内部写盘改成缓冲/异步，或者接受失败。
     * 只是"不想让脚本碰文件"的话，交给 Java 层（BlockGuard / hook）更合适 —— 那边可以绕过。
     *
     * @param flags {@link #BLOCK_PROCESS_CREATE} 等规则位的按位或；0 表示不装任何过滤器
     * @return {@link #OK}，或 errno，或 {@link #ERR_NO_LIBRARY}
     */
    public static int install(int flags) {
        if (flags == 0) {
            return OK;
        }
        if (!LIBRARY_LOADED) {
            return ERR_NO_LIBRARY;
        }
        return nativeInstall(flags);
    }

    /** errno → 可读文本。 */
    public static String describe(int code) {
        if (code == OK) return "成功";
        if (code == ERR_NO_LIBRARY) return "本地库未加载";
        return LIBRARY_LOADED ? nativeStrerror(code) : ("errno=" + code);
    }

    private static native int nativeInstall(int flags);

    private static native String nativeStrerror(int errno);
}
