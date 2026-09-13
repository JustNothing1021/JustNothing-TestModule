package com.justnothing.methodsclient.renderer;

import com.justnothing.methodsclient.model.ColoredSegment;

import java.util.List;

/**
 * 命令输出的渲染抽象：消费端（CLI 终端 / GUI / 文件）各自实现渲染逻辑，与网络流解耦。
 *
 * <p>输出标准化协议：
 * <ul>
 *   <li>流式输出 → {@link #onOutput}（对应服务端 cmd.output 通知）</li>
 *   <li>批量流式输出 → {@link #onOutputBatch}（服务端把连续输出合并进同一帧时）</li>
 *   <li>结尾结构化结果 → {@link #onResult}（对应服务端 cmd.done 携带的 result）</li>
 *   <li>会话结束 → {@link #onDone}（cmd.done 到达）</li>
 * </ul>
 */
public interface OutputRenderer {

    /** 收到一段流式输出（可能带颜色，{@code color} 为 0 表示无颜色） */
    void onOutput(byte color, String text);

    /**
     * 收到一批流式输出（服务端把连续输出合并进同一帧）。
     *
     * <p>默认逐段回调 {@link #onOutput}，保持向后兼容；需要"整批只 flush 一次"的
     * 渲染器（如终端）可以覆盖此方法。</p>
     */
    default void onOutputBatch(List<ColoredSegment> segments) {
        for (ColoredSegment segment : segments) {
            onOutput(segment.color(), segment.text());
        }
    }

    /** 收到命令的最终结构化结果（JSON 字符串，来自 cmd.done 的 result 字段） */
    void onResult(String resultJson);

    /** 命令执行完成（cmd.done 到达） */
    void onDone();
}