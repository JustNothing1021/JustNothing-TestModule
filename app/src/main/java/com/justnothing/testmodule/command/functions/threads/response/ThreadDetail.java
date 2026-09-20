package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个线程的明细，供 GUI 结构化展示（命令行侧本来就已把同样内容打印出来）。
 */
public class ThreadDetail {

    @Expose @SerializedName("threadId")
    private long threadId;
    @Expose @SerializedName("name")
    private String name;
    @Expose @SerializedName("state")
    private String state;
    @Expose @SerializedName("priority")
    private int priority;
    @Expose @SerializedName("daemon")
    private boolean daemon;
    @Expose @SerializedName("interrupted")
    private boolean interrupted;
    @Expose @SerializedName("alive")
    private boolean alive;
    @Expose @SerializedName("stackTrace")
    private List<String> stackTrace = new ArrayList<>();

    public ThreadDetail() {
    }

    /**
     * 从运行时线程对象构造明细。
     *
     * @param includeStackTrace 为 false 时只带状态信息（用于 GUI 高频刷新，避免传输整棵堆栈）
     */
    public static ThreadDetail of(Thread thread, StackTraceElement[] stack, boolean includeStackTrace) {
        ThreadDetail detail = new ThreadDetail();
        detail.threadId = thread.getId();
        detail.name = thread.getName();
        detail.state = thread.getState().name();
        detail.priority = thread.getPriority();
        detail.daemon = thread.isDaemon();
        detail.interrupted = thread.isInterrupted();
        detail.alive = thread.isAlive();
        if (includeStackTrace && stack != null) {
            for (StackTraceElement element : stack) {
                detail.stackTrace.add(element.toString());
            }
        }
        return detail;
    }

    public long getThreadId() { return threadId; }
    public void setThreadId(long threadId) { this.threadId = threadId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isDaemon() { return daemon; }
    public void setDaemon(boolean daemon) { this.daemon = daemon; }

    public boolean isInterrupted() { return interrupted; }
    public void setInterrupted(boolean interrupted) { this.interrupted = interrupted; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public List<String> getStackTrace() { return stackTrace; }
    public void setStackTrace(List<String> stackTrace) { this.stackTrace = stackTrace; }
}
