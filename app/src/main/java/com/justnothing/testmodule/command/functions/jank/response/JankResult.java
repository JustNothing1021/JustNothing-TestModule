package com.justnothing.testmodule.command.functions.jank.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.util.List;

/**
 * jank 采样的结果。
 *
 * <p>{@code output} 是给人看的文本总结（终端里直接就是它）；其余字段是同一份结论的结构化形式，
 * 方便以后写进报告或者被别的命令消费。</p>
 */
public class JankResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;

    /** 纯文本总结，客户端渲染的就是它。 */
    @Expose @SerializedName("output")
    private String output;

    /** 实际采到的帧数。 */
    @Expose @SerializedName("samples")
    private int samples;

    @Expose @SerializedName("avgBusyPct")
    private double avgBusyPct;

    @Expose @SerializedName("peakBusyPct")
    private double peakBusyPct;

    /** 占用 ≥ 85% 的帧数。 */
    @Expose @SerializedName("hotFrames")
    private int hotFrames;

    /** 每核排队的调度实体数的峰值。 */
    @Expose @SerializedName("peakRunQueuePerCore")
    private double peakRunQueuePerCore;

    @Expose @SerializedName("avgMemUsedPct")
    private double avgMemUsedPct;

    /** 看得见的进程数（取最后一帧）。 */
    @Expose @SerializedName("procCount")
    private int procCount;

    @Expose @SerializedName("threadCount")
    private int threadCount;

    /** D 状态进程数的峰值。 */
    @Expose @SerializedName("peakDState")
    private int peakDState;

    /** 单帧采集耗时的平均值（毫秒）。 */
    @Expose @SerializedName("avgSampleCostMs")
    private long avgSampleCostMs;

    /** 上过 CPU TOP 榜单的进程，按上榜次数排序，形如 {@code system_server ×14}。 */
    @Expose @SerializedName("suspects")
    private List<String> suspects;

    /** 采样期间新增/消失的进程，形如 {@code +4211 surfaceflinger}。 */
    @Expose @SerializedName("churn")
    private List<String> churn;

    public JankResult() {
        super();
    }

    public JankResult(String requestId) {
        super(requestId);
    }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public int getSamples() { return samples; }
    public void setSamples(int samples) { this.samples = samples; }

    public double getAvgBusyPct() { return avgBusyPct; }
    public void setAvgBusyPct(double avgBusyPct) { this.avgBusyPct = avgBusyPct; }

    public double getPeakBusyPct() { return peakBusyPct; }
    public void setPeakBusyPct(double peakBusyPct) { this.peakBusyPct = peakBusyPct; }

    public int getHotFrames() { return hotFrames; }
    public void setHotFrames(int hotFrames) { this.hotFrames = hotFrames; }

    public double getPeakRunQueuePerCore() { return peakRunQueuePerCore; }
    public void setPeakRunQueuePerCore(double peakRunQueuePerCore) {
        this.peakRunQueuePerCore = peakRunQueuePerCore;
    }

    public double getAvgMemUsedPct() { return avgMemUsedPct; }
    public void setAvgMemUsedPct(double avgMemUsedPct) { this.avgMemUsedPct = avgMemUsedPct; }

    public int getProcCount() { return procCount; }
    public void setProcCount(int procCount) { this.procCount = procCount; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getPeakDState() { return peakDState; }
    public void setPeakDState(int peakDState) { this.peakDState = peakDState; }

    public long getAvgSampleCostMs() { return avgSampleCostMs; }
    public void setAvgSampleCostMs(long avgSampleCostMs) { this.avgSampleCostMs = avgSampleCostMs; }

    public List<String> getSuspects() { return suspects; }
    public void setSuspects(List<String> suspects) { this.suspects = suspects; }

    public List<String> getChurn() { return churn; }
    public void setChurn(List<String> churn) { this.churn = churn; }
}
