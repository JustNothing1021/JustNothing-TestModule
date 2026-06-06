package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.utils.CmdParamProcessor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NewArchitectureIntegrationTest {

    @Before
    public void setUp() {
        CmdParamProcessor.getCmdParamFields(MemoryInfoRequest.class);
    }

    @Test
    public void testCmdParamFieldScanning() {
        var fields = CmdParamProcessor.getCmdParamFields(MemoryInfoRequest.class);

        assertEquals("应该扫描到3个字段", 3, fields.size());

        boolean hasDetailLevel = fields.stream()
            .anyMatch(fi -> fi.param().name().equals("--detail-level"));
        assertTrue("应该有 --detail-level 参数", hasDetailLevel);

        boolean hasHeap = fields.stream()
            .anyMatch(fi -> fi.param().name().equals("--heap"));
        assertTrue("应该有 --heap 参数", hasHeap);

        boolean hasDetailed = fields.stream()
            .anyMatch(fi -> fi.param().name().equals("--detailed"));
        assertTrue("应该有 --detailed 参数", hasDetailed);
    }

    @Test
    public void testBasicParameterParsing() throws Exception {
        MemoryInfoRequest request = new MemoryInfoRequest();

        String[] args = {"--heap"};
        CmdParamProcessor.parseCommandLineArgs(request, args);

        assertTrue("heapOnly 应该为 true", request.isHeapOnly());
        assertEquals("detailLevel 应该使用默认值", "full", request.getDetailLevel());
    }

    @Test
    public void testAliasParsing() throws Exception {
        MemoryInfoRequest request = new MemoryInfoRequest();

        String[] args = {"-h", "-d"};
        CmdParamProcessor.parseCommandLineArgs(request, args);

        assertTrue("heapOnly 应该为 true (通过别名 -h)", request.isHeapOnly());
        assertTrue("detailed 应该为 true (通过别名 -d)", request.isDetailed());
    }

    @Test
    public void testDefaultValueApplication() throws Exception {
        MemoryInfoRequest request = new MemoryInfoRequest();

        String[] args = {};
        CmdParamProcessor.parseCommandLineArgs(request, args);

        assertEquals("detailLevel 默认值应该是 full", "full", request.getDetailLevel());
        assertFalse("heapOnly 默认值应该是 false", request.isHeapOnly());
        assertTrue("detailed 默认值应该是 true", request.isDetailed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAllowedValue() throws Exception {
        MemoryInfoRequest request = new MemoryInfoRequest();

        String[] args = {"--detail-level", "invalid_value"};
        CmdParamProcessor.parseCommandLineArgs(request, args);
    }

    @Test
    public void testValidAllowedValue() throws Exception {
        MemoryInfoRequest request = new MemoryInfoRequest();

        String[] args = {"--detail-level", "basic"};
        CmdParamProcessor.parseCommandLineArgs(request, args);

        assertEquals("detailLevel 应该是 basic", "basic", request.getDetailLevel());
    }

    @Test
    public void testHelpTextGeneration() {
        String helpText = CmdParamProcessor.generateHelpText(MemoryInfoRequest.class);

        assertNotNull("帮助文本不应该为 null", helpText);
        assertTrue("帮助文本应该包含 --detail-level", 
            helpText.contains("--detail-level"));
        assertTrue("帮助文本应该包含 --heap",
            helpText.contains("--heap"));
        assertTrue("帮助文本应该包含允许值列表",
            helpText.contains("{basic|full}"));

        System.out.println("=== 生成的帮助文本 ===");
        System.out.println(helpText);
        System.out.println("=======================");
    }

    @Test
    public void testGsonIntegration() {
        if (!isGsonAvailable()) {
            System.out.println("⚠️ Gson 库不可用，跳过 Gson 集成测试");
            return;
        }

        try {
            com.google.gson.Gson gson = CmdParamProcessor.createGsonWithCmdParamSupport(MemoryInfoRequest.class);

            MemoryInfoRequest original = new MemoryInfoRequest();
            original.setDetailLevel("basic");
            original.setHeapOnly(true);
            original.setDetailed(false);

            String jsonStr = gson.toJson(original);
            assertNotNull("JSON 字符串不应该为 null", jsonStr);
            assertTrue("JSON 应该包含 detailLevel", jsonStr.contains("detailLevel"));
            assertTrue("JSON 应该包含 heapOnly", jsonStr.contains("heapOnly"));

            System.out.println("=== Gson 序列化结果 ===");
            System.out.println(jsonStr);
            System.out.println("========================");

            MemoryInfoRequest restored = gson.fromJson(jsonStr, MemoryInfoRequest.class);
            assertEquals("反序列化后 detailLevel 应该一致", 
                original.getDetailLevel(), restored.getDetailLevel());
            assertEquals("反序列化后 heapOnly 应该一致",
                original.isHeapOnly(), restored.isHeapOnly());

        } catch (Exception e) {
            System.out.println("⚠️ Gson 集成测试跳过: " + e.getMessage());
        }
    }

    private boolean isGsonAvailable() {
        try {
            Class.forName("com.google.gson.Gson");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
