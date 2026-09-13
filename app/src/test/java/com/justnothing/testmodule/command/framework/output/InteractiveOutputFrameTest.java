package com.justnothing.testmodule.command.framework.output;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.justnothing.testmodule.command.framework.output.InteractiveOutputHandler.OutputSegment;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 校验手工拼的 cmd.output 帧：合法 JSON、转义完整、批量结构正确。
 *
 * <p>输出文本是命令产生的任意内容（引号 / 反斜杠 / 换行 / 控制字符 / 中文 / 表情），
 * 一旦转义漏项，整个 RPC 帧就会坏掉且很难定位，所以这里用"拼出来再解析回去"的方式钉死。</p>
 */
public class InteractiveOutputFrameTest {

    @Test
    public void singleSegmentKeepsLegacyShape() {
        List<OutputSegment> batch = List.of(new OutputSegment(Colors.GREEN, true, "X"));

        JsonObject params = parseParams(batch);
        assertFalse("单段不应带 segments（保持旧格式向后兼容）", params.has("segments"));
        assertEquals("X", params.get("data").getAsString());
        assertEquals((int) Colors.GREEN, (int) params.get("color").getAsByte());
    }

    @Test
    public void singleSegmentWithoutColorOmitsColorField() {
        List<OutputSegment> batch = List.of(new OutputSegment(Colors.DEFAULT, false, "plain"));

        JsonObject params = parseParams(batch);
        assertEquals("plain", params.get("data").getAsString());
        assertFalse("无颜色时不应输出 color 字段", params.has("color"));
    }

    @Test
    public void batchKeepsSegmentsInOrderWithColors() {
        List<OutputSegment> batch = new ArrayList<>();
        batch.add(new OutputSegment(Colors.RED, true, "a"));
        batch.add(new OutputSegment(Colors.DEFAULT, false, "b"));
        batch.add(new OutputSegment(Colors.GREEN, true, "c"));

        JsonObject params = parseParams(batch);
        JsonArray segments = params.getAsJsonArray("segments");
        assertEquals(3, segments.size());
        assertEquals("a", segments.get(0).getAsJsonObject().get("data").getAsString());
        assertEquals((int) Colors.RED, (int) segments.get(0).getAsJsonObject().get("color").getAsByte());
        assertFalse("无颜色的片段不应输出 color 字段", segments.get(1).getAsJsonObject().has("color"));
        assertEquals("c", segments.get(2).getAsJsonObject().get("data").getAsString());
        assertEquals((int) Colors.GREEN, (int) segments.get(2).getAsJsonObject().get("color").getAsByte());
    }

    /**
     * 转义的正确性：任意文本拼进去，解析回来必须一模一样。
     * （JsonParser 对非法 JSON 会抛异常，所以这条同时校验了"没有拼坏"。）
     */
    @Test
    public void trickyTextRoundTrips() {
        String tricky = "引号\" 反斜杠\\ 换行\n 回车\r 制表\t 退格\b 换页\f "
                + "控制\u0001\u001f 中文 表情\uD83D\uDE00 "
                + "行分隔" + (char) 0x2028 + " 段分隔" + (char) 0x2029;
        List<OutputSegment> batch = List.of(new OutputSegment(Colors.DEFAULT, true, tricky));

        JsonObject params = parseParams(batch);
        assertEquals("转义后必须原样还原", tricky, params.get("data").getAsString());
    }

    private static JsonObject parseParams(List<OutputSegment> batch) {
        String frame = InteractiveOutputHandler.buildOutputFrame(batch);
        JsonObject root = JsonParser.parseString(frame).getAsJsonObject();
        assertEquals("2.0", root.get("jsonrpc").getAsString());
        assertEquals(ProtocolMethods.CMD_OUTPUT, root.get("method").getAsString());
        assertTrue("帧里必须有 params", root.has("params"));
        return root.getAsJsonObject("params");
    }
}
