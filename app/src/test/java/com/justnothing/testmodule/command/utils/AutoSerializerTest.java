package com.justnothing.testmodule.command.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AutoSerializerTest {

    private AutoHookRequest request;
    private AutoHookResult result;

    @Before
    public void setUp() {
        request = new AutoHookRequest();
        request.setTargetClass("com.example.MainActivity");
        request.setTargetMethod("onCreate");
        request.setHookType("before");
        request.setOutputPath("/sdcard/hook.log");
        request.setDebugMode(true);
        request.setThreadSafe(true);
        request.setTimeout(5000);
        request.setMaxRetries(3);
        request.setDescription("Test hook for lifecycle monitoring");

        result = new AutoHookResult();
        result.setSuccess(true);
        result.setMessage("Hook installed successfully");
        result.setHookId("hook-auto-001");
        result.setHookedClasses(Arrays.asList(
            "com.example.Activity",
            "com.example.Fragment"
        ));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalHooks", 5);
        stats.put("activeHooks", 3);
        stats.put("memoryUsage", "2.5MB");
        result.setStatistics(stats);
    }

    // ============================================
    // Section 1: Request 自动序列化测试
    // ============================================

    @Test
    public void testAutoSerialize_Request_BasicFields() {
        String json = AutoSerializer.toJson(request);

        System.out.println("=== Auto Serialized Request ===");
        System.out.println(json);

        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain targetClass", json.contains("\"targetClass\""));
        assertTrue("JSON should contain targetMethod", json.contains("\"targetMethod\""));
        assertTrue("JSON should contain hookType", json.contains("\"hookType\""));
    }

    @Test
    public void testAutoDeserialize_Request_BasicFields() {
        String json = AutoSerializer.toJson(request);
        
        AutoHookRequest restored = AutoSerializer.fromJson(json, AutoHookRequest.class);

        System.out.println("=== Auto Deserialized Request ===");
        System.out.println("Original targetClass: " + request.getTargetClass());
        System.out.println("Restored targetClass: " + restored.getTargetClass());

        assertEquals("targetClass should match", 
                     request.getTargetClass(), restored.getTargetClass());
        assertEquals("targetMethod should match", 
                     request.getTargetMethod(), restored.getTargetMethod());
        assertEquals("hookType should match", 
                     request.getHookType(), restored.getHookType());
        assertEquals("outputPath should match", 
                     request.getOutputPath(), restored.getOutputPath());
    }

    @Test
    public void testAutoRoundTrip_Request_BooleanAndIntegerFields() {
        String json = AutoSerializer.toJson(request);
        AutoHookRequest restored = AutoSerializer.fromJson(json, AutoHookRequest.class);

        assertTrue("debugMode should survive auto round-trip", restored.isDebugMode());
        assertTrue("threadSafe should survive auto round-trip", restored.isThreadSafe());
        assertEquals("timeout should survive auto round-trip", 5000, restored.getTimeout());
        assertEquals("maxRetries should survive auto round-trip", 3, restored.getMaxRetries());
    }

    @Test
    public void testAutoRoundTrip_Request_CompleteObject() {
        String json = AutoSerializer.toJson(request);
        AutoHookRequest restored = AutoSerializer.fromJson(json, AutoHookRequest.class);

        assertEquals("Complete object should match after round-trip", request, restored);
    }

    // ============================================
    // Section 2: Result 自动序列化测试
    // ============================================

    @Test
    public void testAutoSerialize_Result_Basic() {
        String json = AutoSerializer.toJson(result);

        System.out.println("\n=== Auto Serialized Result ===");
        System.out.println(json);

        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain hookId", json.contains("\"hookId\""));
        assertTrue("JSON should contain hookedClasses", json.contains("\"hookedClasses\""));
        assertTrue("JSON should contain statistics", json.contains("\"statistics\""));
    }

    @Test
    public void testAutoDeserialize_Result_WithList() {
        String json = AutoSerializer.toJson(result);
        AutoHookResult restored = AutoSerializer.fromJson(json, AutoHookResult.class);

        assertNotNull("hookedClasses should not be null after deserialization", restored.getHookedClasses());
        assertEquals("hookedClasses size should match", 
                     2, restored.getHookedClasses().size());
        assertTrue("Should contain first class", 
                   restored.getHookedClasses().contains("com.example.Activity"));
        assertTrue("Should contain second class", 
                   restored.getHookedClasses().contains("com.example.Fragment"));
    }

    @Test
    public void testAutoDeserialize_Result_WithMap() {
        String json = AutoSerializer.toJson(result);
        AutoHookResult restored = AutoSerializer.fromJson(json, AutoHookResult.class);

        assertNotNull("statistics should not be null after deserialization", restored.getStatistics());
        assertNotNull("totalHooks should exist in statistics", restored.getStatistics().get("totalHooks"));
        assertNotNull("activeHooks should exist in statistics", restored.getStatistics().get("activeHooks"));
        assertNotNull("memoryUsage should exist in statistics", restored.getStatistics().get("memoryUsage"));
    }

    @Test
    public void testAutoRoundTrip_Result_Complete() {
        String json = AutoSerializer.toJson(result);
        AutoHookResult restored = AutoSerializer.fromJson(json, AutoHookResult.class);

        assertTrue("success should survive auto round-trip", restored.isSuccess());
        assertEquals("message should match", result.getMessage(), restored.getMessage());
        assertEquals("hookId should match", result.getHookId(), restored.getHookId());
        assertNotNull("hookedClasses should survive", restored.getHookedClasses());
        assertNotNull("statistics should survive", restored.getStatistics());
    }

    // ============================================
    // Section 3: 使用便捷 API 测试
    // ============================================

    @Test
    public void testConvenientAPI_ToJson() {
        String json = request.autoToJson();

        assertNotNull("autoToJson() should work", json);
        assertTrue("autoToJson() should produce valid JSON", json.contains("targetClass"));
    }

    @Test
    public void testConvenientAPI_FromJson() {
        String json = request.autoToJson();

        AutoHookRequest restored = request.autoFromJson(json);

        assertEquals("autoFromJson() should restore object correctly", 
                     request.getTargetClass(), restored.getTargetClass());
    }

    // ============================================
    // Section 4: 端到端集成测试 (CLI → Auto Request → Auto Result → JSON)
    // ============================================

    @Test
    public void testEndToEnd_AutoSerialization() throws Exception {
        // Step 1: Parse CLI arguments using ParamParser
        String[] cliArgs = {"-d", "--thread-safe", "com.example.App", "main", "--timeout=3000"};
        ComplexHookRequest parsedRequest = ParamParser.parse(ComplexHookRequest.class, cliArgs);

        assertTrue(parsedRequest.isDebugMode());
        assertTrue(parsedRequest.isThreadSafe());
        assertEquals("com.example.App", parsedRequest.getTargetClass());
        assertEquals("main", parsedRequest.getTargetMethod());
        assertEquals(3000, parsedRequest.getTimeout());

        // Step 2: Convert to AutoHookRequest for auto-serialization
        AutoHookRequest autoRequest = new AutoHookRequest();
        autoRequest.setTargetClass(parsedRequest.getTargetClass());
        autoRequest.setTargetMethod(parsedRequest.getTargetMethod());
        autoRequest.setDebugMode(parsedRequest.isDebugMode());
        autoRequest.setThreadSafe(parsedRequest.isThreadSafe());
        autoRequest.setTimeout(parsedRequest.getTimeout());

        // Step 3: Auto-serialize to JSON
        String requestJson = AutoSerializer.toJson(autoRequest);
        System.out.println("\n=== End-to-End Auto Serialization Test ===");
        System.out.println("CLI args: " + Arrays.toString(cliArgs));
        System.out.println("Auto-serialized JSON:\n" + requestJson);

        // Step 3: Auto-deserialize from JSON
        AutoHookRequest serverRequest = AutoSerializer.fromJson(requestJson, AutoHookRequest.class);
        assertEquals("Server should receive correct targetClass", 
                     "com.example.App", serverRequest.getTargetClass());
        assertTrue("Server should see debugMode", serverRequest.isDebugMode());

        // Step 4: Create Result and auto-serialize
        AutoHookResult hookResult = new AutoHookResult();
        hookResult.setSuccess(true);
        hookResult.setHookId("auto-e2e-" + System.currentTimeMillis());
        hookResult.setMessage("Auto-hook installed on " + serverRequest.getTargetClass());
        hookResult.setHookedClasses(Arrays.asList(serverRequest.getTargetClass()));

        String resultJson = AutoSerializer.toJson(hookResult);
        System.out.println("Auto-serialized Result JSON:\n" + resultJson);

        // Step 5: Auto-deserialize Result on client side
        AutoHookResult clientResult = AutoSerializer.fromJson(resultJson, AutoHookResult.class);

        assertTrue("Client should see success", clientResult.isSuccess());
        assertNotNull("Client should have hookId", clientResult.getHookId());
        assertNotNull("Client should have message", clientResult.getMessage());
        assertNotNull("Client should have hookedClasses", clientResult.getHookedClasses());
        assertEquals("Client should see correct class in list", 
                     1, clientResult.getHookedClasses().size());
    }

    // ============================================
    // Section 5: 边界条件测试
    // ============================================

    @Test
    public void testAutoSerialize_NullFields() {
        AutoHookRequest emptyRequest = new AutoHookRequest();

        String json = AutoSerializer.toJson(emptyRequest);
        AutoHookRequest restored = AutoSerializer.fromJson(json, AutoHookRequest.class);

        assertNull("null fields should stay null", restored.getTargetClass());
        assertNull("null fields should stay null", restored.getDescription());
        assertFalse("default boolean should be false", restored.isDebugMode());
        assertEquals("default int should be 0", 0, restored.getTimeout());
    }

    @Test
    public void testAutoDeserialize_EmptyJson() {
        String emptyJson = "{}";

        AutoHookRequest restored = AutoSerializer.fromJson(emptyJson, AutoHookRequest.class);

        assertNotNull("Should create instance from empty JSON", restored);
        assertNull("All fields should be null/empty", restored.getTargetClass());
    }

    @Test
    public void testAutoDeserialize_NullJson() {
        AutoHookRequest restored = AutoSerializer.fromJson(null, AutoHookRequest.class);

        assertNotNull("Should create instance from null JSON", restored);
    }

    @Test
    public void testPerformance_LargeDataset() {
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longDesc.append("Line ").append(i).append(" of description. ");
        }
        request.setDescription(longDesc.toString());

        List<String> manyClasses = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manyClasses.add("com.example.package" + i + ".Class" + i);
        }
        result.setHookedClasses(manyClasses);

        Map<String, Object> bigStats = new java.util.HashMap<>();
        for (int i = 0; i < 20; i++) {
            bigStats.put("metric_" + i, i * 1000);
        }
        result.setStatistics(bigStats);

        long startReq = System.currentTimeMillis();
        String reqJson = AutoSerializer.toJson(request);
        long reqTime = System.currentTimeMillis() - startReq;

        long startRes = System.currentTimeMillis();
        String resJson = AutoSerializer.toJson(result);
        long resTime = System.currentTimeMillis() - startRes;

        System.out.println("\n=== Performance Test (Auto Serializer) ===");
        System.out.println("Request serialization time: " + reqTime + "ms");
        System.out.println("Result serialization time: " + resTime + "ms");
        System.out.println("Request JSON length: " + reqJson.length() + " chars");
        System.out.println("Result JSON length: " + resJson.length() + " chars");

        assertTrue("Request serialization should complete within 200ms", reqTime < 200);
        assertTrue("Result serialization should complete within 200ms", resTime < 200);
    }
}
