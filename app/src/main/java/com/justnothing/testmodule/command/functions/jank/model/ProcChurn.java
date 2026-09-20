package com.justnothing.testmodule.command.functions.jank.model;

/**
 * 进程流水里的一条：进程出现（{@code +}）或消失（{@code -}）。
 *
 * <p>名字在进程消失之后就读不到了（{@code /proc/<pid>} 已经没了），所以消失事件里的
 * 名字是上一帧从 stat 里带出来的 —— 这也是为什么 {@link ProcessEntry#name} 即使
 * 当前没打算展示也要一直解析着。</p>
 */
public class ProcChurn {

    /** true = 本帧新出现，false = 上一帧还在、本帧没了。 */
    public final boolean added;
    public final int pid;
    public final String name;

    public ProcChurn(boolean added, int pid, String name) {
        this.added = added;
        this.pid = pid;
        this.name = name;
    }

    /** 形如 {@code +4211 surfaceflinger}。 */
    @Override
    public String toString() {
        return (added ? "+" : "-") + pid + " " + name;
    }
}
