package com.justnothing.methodsclient.renderer;

/**
 * JSON 渲染器：丢弃流式输出，仅捕获 cmd.done 携带的结构化结果。
 *
 * <p>供 GUI（UiClient）/ 文件消费端使用——它们只关心最终结果 JSON。</p>
 */
public class JsonRenderer implements OutputRenderer {

    private volatile String resultJson;

    @Override
    public void onOutput(byte color, String text) {
        // JSON 消费端不关心流式输出
    }

    @Override
    public void onResult(String resultJson) {
        this.resultJson = resultJson;
    }

    @Override
    public void onDone() {
    }

    /** 获取最终结构化结果（未收到时返回 null） */
    public String getResultJson() {
        return resultJson;
    }
}
