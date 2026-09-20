package com.justnothing.testmodule.command.functions.bytecode.impl;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link AbstractBytecodeCommand#splitIntoChunks} 的单测。
 *
 * <p>分片是为了把高亮渲染的峰值内存从 O(代码总长) 压到 O(单片)，但切点必须在
 * "安全位置" —— RichConsole 的 lexer 处理块注释/字符串的状态是每片重新开始的，
 * 在块注释中间切开会让后一片把注释正文当成代码上色。</p>
 */
public class ChunkSplitTest {

    @Test
    public void splitsByLineCount() {
        List<String> chunks = AbstractBytecodeCommand.splitIntoChunks("a\nb\nc\nd\ne\n", 2);

        assertEquals(3, chunks.size());
        assertEquals("a\nb\n", chunks.get(0));
        assertEquals("c\nd\n", chunks.get(1));
        assertEquals("e\n", chunks.get(2));
    }

    @Test
    public void keepsBlockCommentInOneChunk() {
        StringBuilder code = new StringBuilder("/*\n");
        for (int i = 0; i < 60; i++) {
            code.append(" * comment line\n");
        }
        code.append(" */\nint x = 1;\n");

        List<String> chunks = AbstractBytecodeCommand.splitIntoChunks(code.toString(), 40);

        // 第一片必须把整个块注释包进去 —— 不能出现"半截注释"
        String first = chunks.get(0);
        assertTrue("第一片应包含完整的块注释结尾", first.contains("*/"));
        assertTrue("块注释之后还应剩下一片", chunks.size() >= 2);
    }

    @Test
    public void ignoresSlashStarInsideString() {
        // 字符串里的 "/*" 不该被当成注释开头，否则后面的切分全乱
        String code = "String a = \"/*\";\nString b = \"x\";\nString c = \"*/\";\nString d = \"y\";\n";

        assertEquals(2, AbstractBytecodeCommand.splitIntoChunks(code, 2).size());
    }

    @Test
    public void ignoresSlashStarInsideLineComment() {
        String code = "int a = 1; // /*\nint b = 2;\nint c = 3;\nint d = 4;\n";

        assertEquals(2, AbstractBytecodeCommand.splitIntoChunks(code, 2).size());
    }

    @Test
    public void keepsEscapedQuoteInsideString() {
        // \" 不该提前结束字符串状态
        String code = "String a = \"say \\\"hi\\\"\";\nint b = 1;\nint c = 2;\n";

        assertEquals(2, AbstractBytecodeCommand.splitIntoChunks(code, 2).size());
    }

    @Test
    public void emptyInputProducesNoChunks() {
        assertTrue(AbstractBytecodeCommand.splitIntoChunks(null, 10).isEmpty());
        assertTrue(AbstractBytecodeCommand.splitIntoChunks("", 10).isEmpty());
        assertTrue(AbstractBytecodeCommand.splitIntoChunks("a\nb\n", 0).isEmpty());
    }
}
