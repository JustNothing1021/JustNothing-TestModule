package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdvancedIntegrationTest {

    @Before
    public void setUp() {
    }

    // ============================================
    // Section 1: 复杂参数解析测试
    // ============================================

    @Test
    public void testComplexHook_AllPositionalParams() throws IllegalCommandLineArgumentException {
        String[] args = {"com.example.MainActivity", "onCreate", "before", "/sdcard/hook_output.txt"};
        
        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, args);
        
        assertNotNull("Request should not be null", request);
        assertEquals("com.example.MainActivity", request.getTargetClass());
        assertEquals("onCreate", request.getTargetMethod());
        assertEquals("before", request.getHookType());
        assertEquals("/sdcard/hook_output.txt", request.getOutputPath());
    }

    @Test
    public void testComplexHook_MixedFlagsAndKeywords() throws IllegalCommandLineArgumentException {
        String[] args = {
            "-d",
            "--thread-safe",
            "com.example.Activity",
            "onResume",
            "after",
            "--timeout=5000",
            "--max-retries=3",
            "--description=Monitor lifecycle events",
            "/sdcard/output.log"
        };
        
        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, args);
        
        assertTrue("debugMode should be true", request.isDebugMode());
        assertTrue("threadSafe should be true", request.isThreadSafe());
        assertEquals("com.example.Activity", request.getTargetClass());
        assertEquals("onResume", request.getTargetMethod());
        assertEquals("after", request.getHookType());
        assertEquals(5000, request.getTimeout());
        assertEquals(3, request.getMaxRetries());
        assertEquals("Monitor lifecycle events", request.getDescription());
        assertEquals("/sdcard/output.log", request.getOutputPath());
    }

    @Test
    public void testComplexHook_FlagsAtVariousPositions() throws IllegalCommandLineArgumentException {
        String[] args = {
            "com.example.Service",
            "-d",
            "onStartCommand",
            "--timeout=10000",
            "--thread-safe",
            "before",
            "--tags=lifecycle,network"
        };
        
        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, args);
        
        assertEquals("com.example.Service", request.getTargetClass());
        assertTrue("debug mode should work in middle", request.isDebugMode());
        assertEquals("onStartCommand", request.getTargetMethod());
        assertEquals(10000, request.getTimeout());
        assertTrue("thread-safe flag should work anywhere", request.isThreadSafe());
        assertEquals("before", request.getHookType());
        assertEquals("lifecycle,network", request.getTags());
    }

    @Test
    public void testComplexHook_DefaultValues() throws IllegalCommandLineArgumentException {
        String[] args = {"com.example.Class", "method"};
        
        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, args);
        
        assertFalse("debugMode should default to false", request.isDebugMode());
        assertFalse("threadSafe should default to false", request.isThreadSafe());
        assertEquals("hookType should default to 'before'", "before", request.getHookType());
        assertEquals("timeout should default to 0", 0, request.getTimeout());
        assertNull("outputPath should default to null", request.getOutputPath());
    }

    @Test
    public void testComplexHook_VarArgsWithSpaces() throws IllegalCommandLineArgumentException {
        String[] args = {
            "com.example.App",
            "main",
            "--description=This is a very long description with spaces",
            "/sdcard/path with spaces/file.txt"
        };
        
        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, args);
        
        assertEquals("com.example.App", request.getTargetClass());
        assertEquals("main", request.getTargetMethod());
        assertEquals("This is a very long description with spaces", request.getDescription());
        assertEquals("/sdcard/path with spaces/file.txt", request.getOutputPath());
    }

    @Test(expected = IllegalCommandLineArgumentException.class)
    public void testComplexHook_MissingRequiredParams() throws IllegalCommandLineArgumentException {
        String[] args = {"onlyOneParam"};
        
        ParamParser.parse(ComplexHookRequest.class, args);
    }

    // ============================================
    // Section 2: JSON 序列化/反序列化往返测试
    // ============================================

    @Test
    public void testJsonRoundTrip_BasicFields() throws JSONException {
        ComplexHookRequest original = new ComplexHookRequest();
        original.setTargetClass("com.example.Test");
        original.setTargetMethod("testMethod");
        original.setHookType("after");
        original.setOutputPath("/sdcard/test.log");
        original.setRequestId("req-12345");

        String jsonStr = original.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());

        ComplexHookRequest restored = new ComplexHookRequest();
        restored.fromJsonString(jsonStr);

        assertNotNull("targetClass should survive round-trip", restored.getTargetClass());
        assertEquals("targetClass should survive round-trip",
                     original.getTargetClass(), restored.getTargetClass());
        assertNotNull("targetMethod should survive round-trip", restored.getTargetMethod());
        assertEquals("targetMethod should survive round-trip",
                     original.getTargetMethod(), restored.getTargetMethod());
        assertNotNull("hookType should survive round-trip", restored.getHookType());
        assertEquals("hookType should survive round-trip",
                     original.getHookType(), restored.getHookType());
        assertNotNull("requestId should survive round-trip", restored.getRequestId());
        assertEquals("requestId should survive round-trip",
                     original.getRequestId(), restored.getRequestId());
    }

    @Test
    public void testJsonRoundTrip_BooleanAndIntegerFields() throws JSONException {
        ComplexHookRequest original = new ComplexHookRequest();
        original.setTargetClass("cls");
        original.setTargetMethod("meth");
        original.setDebugMode(true);
        original.setThreadSafe(true);
        original.setTimeout(9999);
        original.setMaxRetries(42);

        String jsonStr = original.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("Boolean/Int JSON: " + jsonStr);

        ComplexHookRequest restored = new ComplexHookRequest();
        restored.fromJsonString(jsonStr);

        assertTrue("debugMode boolean should survive JSON", restored.isDebugMode());
        assertTrue("threadSafe boolean should survive JSON", restored.isThreadSafe());
        assertEquals("timeout int should survive JSON", 9999, restored.getTimeout());
        assertEquals("maxRetries int should survive JSON", 42, restored.getMaxRetries());
    }

    @Test
    public void testJsonRoundTrip_NullFields() throws JSONException {
        ComplexHookRequest original = new ComplexHookRequest();
        original.setTargetClass("TestClass");
        original.setTargetMethod("testM");
        original.setDescription(null);
        original.setTags(null);

        JSONObject json = original.toJson();
        ComplexHookRequest restored = new ComplexHookRequest();
        restored.fromJson(json);

        assertNull("null description should stay null after round-trip", restored.getDescription());
        assertNull("null tags should stay null after round-trip", restored.getTags());
    }

    @Test
    public void testJsonRoundTrip_ComplexStringWithSpecialChars() throws JSONException {
        ComplexHookRequest original = new ComplexHookRequest();
        original.setTargetClass("com.example.Class$Inner");
        original.setTargetMethod("<init>");
        original.setDescription("Test with \"quotes\" and 'apostrophes' & <html> tags");
        original.setTags("tag1,tag2,tag with spaces,tag=equals");

        String jsonStr = original.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("JSON output: " + jsonStr);

        ComplexHookRequest restored = new ComplexHookRequest();
        restored.fromJsonString(jsonStr);

        assertNotNull("targetClass should not be null after round-trip", restored.getTargetClass());
        assertEquals("Special chars in targetClass should survive",
                     original.getTargetClass(), restored.getTargetClass());
        assertNotNull("targetMethod should not be null after round-trip", restored.getTargetMethod());
        assertEquals("Special chars in method name should survive",
                     original.getTargetMethod(), restored.getTargetMethod());
        assertNotNull("description should not be null after round-trip", restored.getDescription());
        assertEquals("Special chars in description should survive",
                     original.getDescription(), restored.getDescription());
        assertNotNull("tags should not be null after round-trip", restored.getTags());
        assertEquals("Special chars in tags should survive",
                     original.getTags(), restored.getTags());
    }

    // ============================================
    // Section 3: Result 序列化测试
    // ============================================

    @Test
    public void testResultSerialization_Basic() throws JSONException {
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setHookId("hook-001");
        result.setMessage("Hook installed successfully");
        result.setRequestId("req-001");

        String jsonStr = result.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("Basic Result JSON: " + jsonStr);

        assertTrue("JSON should contain success field", jsonStr.contains("\"success\":true"));
        assertTrue("JSON should contain hookId", jsonStr.contains("\"hookId\":\"hook-001\""));
        assertTrue("JSON should contain message", jsonStr.contains("\"message\":\"Hook installed successfully\""));
    }

    @Test
    public void testResultSerialization_WithList() throws JSONException {
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setHookedClasses(Arrays.asList(
            "com.example.Activity",
            "com.example.Fragment",
            "com.example.Service"
        ));

        String jsonStr = result.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("List Result JSON: " + jsonStr);

        assertTrue("JSON should contain hookedClasses array", jsonStr.contains("\"hookedClasses\""));
        assertTrue("JSON should contain first class", jsonStr.contains("com.example.Activity"));
        assertTrue("JSON should contain second class", jsonStr.contains("com.example.Fragment"));
        assertTrue("JSON should contain third class", jsonStr.contains("com.example.Service"));
    }

    @Test
    public void testResultSerialization_WithMap() throws JSONException {
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalHooks", 5);
        stats.put("activeHooks", 3);
        stats.put("memoryUsage", "2.5MB");
        stats.put("avgLatency", 15.7);
        result.setStatistics(stats);

        String jsonStr = result.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("Map Result JSON: " + jsonStr);

        assertTrue("JSON should contain statistics object", jsonStr.contains("\"statistics\""));
        assertTrue("JSON should contain totalHooks", jsonStr.contains("\"totalHooks\":5"));
        assertTrue("JSON should contain activeHooks", jsonStr.contains("\"activeHooks\":3"));
        assertTrue("JSON should contain memoryUsage", jsonStr.contains("\"memoryUsage\":\"2.5MB\""));
        assertTrue("JSON should contain avgLatency", jsonStr.contains("\"avgLatency\":15.7"));
    }

    @Test
    public void testResultDeserialization_CompleteRoundTrip() throws JSONException {
        ComplexHookResult original = new ComplexHookResult();
        original.setSuccess(true);
        original.setHookId("hook-rt-001");
        original.setMessage("Round-trip test");
        original.setRequestId("req-rt-001");
        original.setHookedClasses(Arrays.asList("class1", "class2"));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("count", 10);
        original.setStatistics(stats);

        String jsonStr = original.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("toJsonString() should not be empty", jsonStr.trim().isEmpty());
        System.out.println("Result JSON: " + jsonStr);

        ComplexHookResult restored = new ComplexHookResult();
        restored.fromJsonString(jsonStr);

        assertTrue("success should survive round-trip", restored.isSuccess());
        assertNotNull("hookId should not be null after round-trip", restored.getHookId());
        assertEquals("hookId should survive round-trip", original.getHookId(), restored.getHookId());
        assertEquals("message should survive round-trip", original.getMessage(), restored.getMessage());
        assertNotNull("hookedClasses should not be null after round-trip", restored.getHookedClasses());
        assertEquals("hookedClasses size should match",
                     original.getHookedClasses().size(), restored.getHookedClasses().size());
        assertNotNull("statistics should not be null after round-trip", restored.getStatistics());
        assertEquals("statistics count should match", 10, ((Number) restored.getStatistics().get("count")).intValue());
    }

    // ============================================
    // Section 4: 端到端集成测试 (CLI → Request → Result → JSON)
    // ============================================

    @Test
    public void testEndToEnd_SimpleScenario() throws Exception {
        // Step 1: Parse CLI arguments into Request
        String[] cliArgs = {"com.example.App", "onCreate"};

        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, cliArgs);

        assertEquals("CLI parsing - targetClass", "com.example.App", request.getTargetClass());
        assertEquals("CLI parsing - targetMethod", "onCreate", request.getTargetMethod());

        // Step 2: Serialize Request to JSON
        String requestJsonStr = request.toJsonString();
        assertNotNull("Request toJsonString() should not return null", requestJsonStr);
        assertFalse("Request toJsonString() should not be empty", requestJsonStr.trim().isEmpty());
        System.out.println("Request JSON: " + requestJsonStr);

        // Step 3: Deserialize Request from JSON (simulating server receive)
        ComplexHookRequest serverRequest = new ComplexHookRequest();
        serverRequest.fromJsonString(requestJsonStr);

        assertNotNull("Server deserialization - targetClass should not be null", serverRequest.getTargetClass());
        assertEquals("Server deserialization - targetClass",
                     request.getTargetClass(), serverRequest.getTargetClass());

        // Step 4: Create Result and serialize to JSON
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setHookId("auto-gen-" + System.currentTimeMillis());
        result.setMessage("Successfully hooked " + serverRequest.getTargetClass() + "." + serverRequest.getTargetMethod());
        result.setOriginalRequest(serverRequest);

        String resultJsonStr = result.toJsonString();
        assertNotNull("Result toJsonString() should not return null", resultJsonStr);
        assertFalse("Result toJsonString() should not be empty", resultJsonStr.trim().isEmpty());
        System.out.println("Result JSON: " + resultJsonStr);

        // Step 5: Deserialize Result from JSON (simulating client receive)
        ComplexHookResult clientResult = new ComplexHookResult();
        clientResult.fromJsonString(resultJsonStr);

        assertTrue("Client should see success", clientResult.isSuccess());
        assertNotNull("Client should have hookId", clientResult.getHookId());
        assertNotNull("Client should have message", clientResult.getMessage());
        assertNotNull("Client should have originalRequest", clientResult.getOriginalRequest());
        assertEquals("Client should see correct targetClass in originalRequest",
                     "com.example.App", clientResult.getOriginalRequest().getTargetClass());
    }

    @Test
    public void testEndToEnd_ComplexScenario() throws Exception {
        // Step 1: Parse CLI
        String[] cliArgs = {
            "-d",
            "--thread-safe",
            "com.example.network.HttpClient",
            "sendRequest",
            "around",
            "--timeout=30000",
            "--max-retries=5",
            "--description=Network monitoring hook for debugging API calls",
            "--tags=network,monitoring,debug",
            "/sdcard/network_hooks.log"
        };

        ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, cliArgs);
        
        assertTrue(request.isDebugMode());
        assertTrue(request.isThreadSafe());
        assertEquals("com.example.network.HttpClient", request.getTargetClass());
        assertEquals("sendRequest", request.getTargetMethod());
        assertEquals("around", request.getHookType());
        assertEquals(30000, request.getTimeout());
        assertEquals(5, request.getMaxRetries());
        assertEquals("Network monitoring hook for debugging API calls", request.getDescription());
        assertEquals("network,monitoring,debug", request.getTags());
        assertEquals("/sdcard/network_hooks.log", request.getOutputPath());

        // Step 2: Request -> JSON -> Server Request
        String reqJsonStr = request.toJsonString();
        ComplexHookRequest serverReq = new ComplexHookRequest();
        serverReq.fromJsonString(reqJsonStr);

        // Step 3: Create complex Result
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setHookId("hook-net-001");
        result.setMessage("Hook installed on network layer");
        result.setHookedClasses(Arrays.asList(
            "com.example.network.HttpClient",
            "com.example.network.ResponseHandler",
            "com.example.network.ConnectionPool"
        ));
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("hooksInstalled", 3);
        stats.put("bytesIntercepted", 1024000);
        stats.put("avgResponseTime", 145.5);
        result.setStatistics(stats);
        result.setOriginalRequest(serverReq);

        // Step 4: Result -> JSON -> Client Result
        String resJsonStr = result.toJsonString();
        assertNotNull("Result toJsonString() should not return null", resJsonStr);
        assertFalse("Request jsonStr should not be empty", reqJsonStr.trim().isEmpty());
        System.out.println("\n=== Complete End-to-End Test ===");
        System.out.println("Request (from CLI): " + Arrays.toString(cliArgs));
        System.out.println("Serialized Request JSON length: " + reqJsonStr.length());
        System.out.println("Serialized Result JSON:\n" + resJsonStr);

        ComplexHookResult clientRes = new ComplexHookResult();
        clientRes.fromJsonString(resJsonStr);

        // Assertions for complete data integrity
        assertTrue(clientRes.isSuccess());
        assertEquals("hook-net-001", clientRes.getHookId());
        assertNotNull(clientRes.getHookedClasses());
        assertEquals(3, clientRes.getHookedClasses().size());
        assertTrue(clientRes.getHookedClasses().contains("com.example.network.HttpClient"));
        assertNotNull(clientRes.getStatistics());
        assertEquals(3, ((Number) clientRes.getStatistics().get("hooksInstalled")).intValue());
        assertNotNull(clientRes.getOriginalRequest());
        assertEquals("com.example.network.HttpClient", clientRes.getOriginalRequest().getTargetClass());
        assertEquals("sendRequest", clientRes.getOriginalRequest().getTargetMethod());
        assertTrue(clientRes.getOriginalRequest().isDebugMode());
        assertTrue(clientRes.getOriginalRequest().isThreadSafe());
        assertEquals(30000, clientRes.getOriginalRequest().getTimeout());
        assertEquals("Network monitoring hook for debugging API calls", 
                    clientRes.getOriginalRequest().getDescription());
    }

    @Test
    public void testEndToEnd_ErrorScenario() throws Exception {
        // Simulate error case: invalid target class
        String[] cliArgs = {"Invalid.Class.Name", "nonExistentMethod"};

        try {
            ComplexHookRequest request = ParamParser.parse(ComplexHookRequest.class, cliArgs);

            // Even though parsing succeeds, the result indicates failure
            ComplexHookResult result = new ComplexHookResult();
            result.setSuccess(false);
            result.setMessage("Failed to hook: Class Invalid.Class.Name not found");
            result.setOriginalRequest(request);

            String resultJsonStr = result.toJsonString();
            assertNotNull("Error Result toJsonString() should not return null", resultJsonStr);
            assertFalse("Error Result jsonStr should not be empty", resultJsonStr.trim().isEmpty());
            System.out.println("Error Result JSON: " + resultJsonStr);

            ComplexHookResult clientResult = new ComplexHookResult();
            clientResult.fromJsonString(resultJsonStr);

            assertFalse("Error scenario: success should be false", clientResult.isSuccess());
            assertTrue("Error scenario: message should contain error info",
                      clientResult.getMessage().contains("Failed to hook"));
            assertNotNull("Error scenario: should still have originalRequest for debugging",
                         clientResult.getOriginalRequest());

        } catch (Exception e) {
            fail("Parsing should succeed even for invalid class names at this stage");
        }
    }

    @Test
    public void testPerformance_LargeDataset() throws Exception {
        // Create a large dataset to test performance
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longDescription.append("This is line ").append(i).append(" of the description. ");
        }
        
        List<String> manyClasses = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manyClasses.add("com.example.package" + i + ".Class" + i);
        }
        
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setMessage(longDescription.toString());
        result.setHookedClasses(manyClasses);
        
        Map<String, Object> bigStats = new java.util.HashMap<>();
        for (int i = 0; i < 20; i++) {
            bigStats.put("metric_" + i, i * 1000);
        }
        result.setStatistics(bigStats);
        
        long startTime = System.currentTimeMillis();
        String jsonStr = result.toJsonString();
        assertNotNull("toJsonString() should not return null", jsonStr);
        assertFalse("jsonStr should not be empty", jsonStr.trim().isEmpty());
        long serializationTime = System.currentTimeMillis() - startTime;

        System.out.println("\n=== Performance Test ===");
        System.out.println("Dataset size:");
        System.out.println("  - Description length: " + longDescription.length() + " chars");
        System.out.println("  - Hooked classes: " + manyClasses.size());
        System.out.println("  - Statistics entries: " + bigStats.size());
        System.out.println("Serialization time: " + serializationTime + "ms");
        System.out.println("JSON string length: " + jsonStr.length() + " chars");
        
        startTime = System.currentTimeMillis();
        ComplexHookResult restored = new ComplexHookResult();
        restored.fromJsonString(jsonStr);
        long deserializationTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Deserialization time: " + deserializationTime + "ms");
        
        // Verify data integrity
        assertTrue(restored.isSuccess());
        assertEquals(longDescription.toString(), restored.getMessage());
        assertNotNull(restored.getHookedClasses());
        assertEquals(manyClasses.size(), restored.getHookedClasses().size());
        assertNotNull(restored.getStatistics());
        assertEquals(bigStats.size(), restored.getStatistics().size());
        
        // Performance assertions (should complete quickly)
        assertTrue("Serialization should complete within 100ms", serializationTime < 100);
        assertTrue("Deserialization should complete within 100ms", deserializationTime < 100);
    }
}
