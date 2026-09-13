package com.justnothing.testmodule.command.framework.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link TerminalRpcChannel.RpcOutputStream} 输出合并的行为测试。
 *
 * <p>背景：JLine 的 FilteringOutputStream 几乎每次 write 之后都会 flush。若每次 flush 都发一帧，
 * 批量输出会退化成"每行一次跨进程帧"。RpcOutputStream 因此引入合并窗口：空闲时立即发送、
 * 连续输出时按窗口合并。这里锁定两点：</p>
 *
 * <ul>
 *   <li>合并必须无损——拼接所有 output 帧后能还原出完整且同序的输出文本；</li>
 *   <li>合并必须生效——突发 flush 产生的帧数远小于 flush 次数。</li>
 * </ul>
 */
public class TerminalRpcChannelOutputTest {

    @Test
    public void burstFlushIsCoalescedWithoutLosingData() throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        TerminalRpcChannel channel = new TerminalRpcChannel(sink, new Object());
        OutputStream output = channel.createOutputStream();

        int lineCount = 500;
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            String line = "line-" + i + "\n";
            expected.append(line);
            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.flush(); // 模拟 JLine 每次 write 后 flush
        }

        // 真实链路里 cmd.done 之前会同步排空，这里照做，保证断言不依赖调度时序
        channel.getRpcOutputStream().flushNow();

        List<String> chunks = decodeOutputChunks(sink.toByteArray());
        String restored = String.join("", chunks);

        assertTrue("合并后必须能无损还原全部输出（含顺序）",
                expected.toString().equals(restored));
        assertTrue("突发 flush 必须被合并，实际帧数=" + chunks.size(),
                chunks.size() < lineCount / 2);
    }

    @Test
    public void flushNowEmitsImmediately() throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        TerminalRpcChannel channel = new TerminalRpcChannel(sink, new Object());
        OutputStream output = channel.createOutputStream();

        output.write("hello".getBytes(StandardCharsets.UTF_8));
        output.flush();
        channel.getRpcOutputStream().flushNow();

        List<String> chunks = decodeOutputChunks(sink.toByteArray());
        assertEquals("flushNow 之后应立即产生且只产生 1 帧", 1, chunks.size());
        assertEquals("hello", chunks.get(0));
    }

    // ==================== 帧解析 ====================

    /** 从原始字节流解出所有 output 通知的 data（按出现顺序）。 */
    private static List<String> decodeOutputChunks(byte[] raw) {
        List<String> chunks = new ArrayList<>();
        int pos = 0;
        while (pos + 13 <= raw.length) {
            if (!matches(raw, pos, InteractiveProtocol.START_MARKER)) {
                pos++;
                continue;
            }
            byte type = raw[pos + 4];
            int length = ((raw[pos + 5] & 0xFF) << 24)
                    | ((raw[pos + 6] & 0xFF) << 16)
                    | ((raw[pos + 7] & 0xFF) << 8)
                    | (raw[pos + 8] & 0xFF);
            int dataStart = pos + 9;
            int dataEnd = dataStart + length;
            if (dataEnd + 4 > raw.length) break;

            if (type == InteractiveProtocol.TYPE_RPC) {
                String json = new String(raw, dataStart, length, StandardCharsets.UTF_8);
                JsonObject msg = JsonParser.parseString(json).getAsJsonObject();
                if (msg.has("method") && ProtocolMethods.TERM_OUTPUT.equals(msg.get("method").getAsString())) {
                    chunks.add(msg.getAsJsonObject("params").get("data").getAsString());
                }
            }
            pos = dataEnd + 4;
        }
        return chunks;
    }

    private static boolean matches(byte[] raw, int offset, byte[] marker) {
        for (int i = 0; i < marker.length; i++) {
            if (raw[offset + i] != marker[i]) return false;
        }
        return true;
    }
}
