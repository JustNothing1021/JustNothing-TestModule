package com.justnothing.methodsclient.renderer;

import com.justnothing.methodsclient.model.ColoredSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * 片段收集渲染器：把流式输出收集为彩色片段列表。
 *
 * <p>供脚本管理页等需要结构化片段（而非直接渲染）的消费端使用。</p>
 */
public class SegmentsRenderer implements OutputRenderer {

    private final List<ColoredSegment> segments = new ArrayList<>();

    @Override
    public void onOutput(byte color, String text) {
        if (text != null && !text.isEmpty()) {
            segments.add(new ColoredSegment(color, text));
        }
    }

    @Override
    public void onResult(String resultJson) {
        // 片段消费端只关心流式输出
    }

    @Override
    public void onDone() {
    }

    public List<ColoredSegment> getSegments() {
        return segments;
    }
}
