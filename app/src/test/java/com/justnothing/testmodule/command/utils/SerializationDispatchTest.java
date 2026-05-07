package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;

public class SerializationDispatchTest {

    private Gson gson;
    private Random random;

    @Before
    public void setUp() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        random = new Random(42);  // 固定种子保证可重复性
    }

    // ============================================
    // Section 1: Request 类型安全分配测试
    // ============================================

    @Test
    public void testRequestDispatch_BasicTypePreservation() throws IllegalCommandLineArgumentException {
        System.out.println("\n=== Test 1: Request Basic Type Preservation ===");

        ComplexHookRequest original = createComplexHookRequest();

        String json = AutoSerializer.toJson(original);
        System.out.println("Original type: " + original.getClass().getSimpleName());
        System.out.println("Serialized JSON length: " + json.length());

        ComplexHookRequest dispatched = AutoSerializer.fromJson(json, ComplexHookRequest.class);
        System.out.println("Dispatched type: " + dispatched.getClass().getSimpleName());

        assertSameType("ComplexHookRequest", original, dispatched);
        assertAllFieldsEqual(original, dispatched);
    }

    @Test
    public void testRequestDispatch_AutoSerializerVsGson() throws IllegalCommandLineArgumentException {
        System.out.println("\n=== Test 2: AutoSerializer vs Gson Consistency ===");

        ComplexHookRequest original = createComplexHookRequest();

        String autoJson = AutoSerializer.toJson(original);
        String gsonJson = gson.toJson(original);

        System.out.println("AutoSerializer JSON length: " + autoJson.length());
        System.out.println("Gson JSON length: " + gsonJson.length());

        ComplexHookRequest autoResult = AutoSerializer.fromJson(autoJson, ComplexHookRequest.class);
        ComplexHookRequest gsonResult = gson.fromJson(gsonJson, ComplexHookRequest.class);

        assertAllFieldsEqual(autoResult, gsonResult);
        System.out.println("✓ AutoSerializer and Gson produce identical results");
    }

    @Test
    public void testRequestDispatch_WithAllParamTypes() throws IllegalCommandLineArgumentException {
        System.out.println("\n=== Test 3: Request With All Parameter Types ===");

        String[] cliArgs = {
            "-d",
            "--thread-safe",
            "com.example.TestClass",
            "testMethod",
            "around",
            "--timeout=9999",
            "--max-retries=100",
            "--description=Full parameter coverage test with special chars: <>&\"'",
            "/sdcard/path/with spaces/output.txt"
        };

        ComplexHookRequest fromCli = ParamParser.parse(ComplexHookRequest.class, cliArgs);

        String json = AutoSerializer.toJson(fromCli);
        ComplexHookRequest dispatched = AutoSerializer.fromJson(json, ComplexHookRequest.class);

        assertSameType("ComplexHookRequest (from CLI)", fromCli, dispatched);
        assertEquals("targetClass", fromCli.getTargetClass(), dispatched.getTargetClass());
        assertEquals("targetMethod", fromCli.getTargetMethod(), dispatched.getTargetMethod());
        assertEquals("hookType", fromCli.getHookType(), dispatched.getHookType());
        assertTrue("debugMode", dispatched.isDebugMode());
        assertTrue("threadSafe", dispatched.isThreadSafe());
        assertEquals("timeout", 9999, dispatched.getTimeout());
        assertEquals("maxRetries", 100, dispatched.getMaxRetries());
        assertEquals("outputPath", fromCli.getOutputPath(), dispatched.getOutputPath());
        assertEquals("description", fromCli.getDescription(), dispatched.getDescription());

        System.out.println("✓ All parameter types preserved correctly");
    }

    // ============================================
    // Section 2: Result 类型安全分配测试
    // ============================================

    @Test
    public void testResultDispatch_BasicTypePreservation() {
        System.out.println("\n=== Test 4: Result Basic Type Preservation ===");

        ComplexHookResult original = createComplexHookResult();

        String json = AutoSerializer.toJson(original);
        System.out.println("Original type: " + original.getClass().getSimpleName());
        System.out.println("Serialized JSON:\n" + json);

        ComplexHookResult dispatched = AutoSerializer.fromJson(json, ComplexHookResult.class);
        System.out.println("Dispatched type: " + dispatched.getClass().getSimpleName());

        assertSameType("ComplexHookResult", original, dispatched);
        assertResultFieldsEqual(original, dispatched);
    }

    @Test
    public void testResultDispatch_WithNestedRequest() {
        System.out.println("\n=== Test 5: Result With Nested Request ===");

        ComplexHookRequest request = createComplexHookRequest();
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setMessage("Nested request test");
        result.setHookId("nested-001");
        result.setOriginalRequest(request);

        String json = AutoSerializer.toJson(result);
        System.out.println("Result with nested request JSON:\n" + json);

        ComplexHookResult dispatched = AutoSerializer.fromJson(json, ComplexHookResult.class);

        assertNotNull("originalRequest should not be null after dispatch", 
                     dispatched.getOriginalRequest());
        assertSameType("nested ComplexHookRequest", 
                      request, dispatched.getOriginalRequest());
        assertAllFieldsEqual(request, dispatched.getOriginalRequest());

        System.out.println("✓ Nested request type and data preserved");
    }

    @Test
    public void testResultDispatch_WithComplexCollections() {
        System.out.println("\n=== Test 6: Result With Complex Collections ===");

        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setMessage("Collection test");

        List<String> classes = Arrays.asList(
            "com.pkg.Class$Inner",
            "com.pkg.Class<Generic>",
            "com.pkg.Class'With'Quotes"
        );
        result.setHookedClasses(classes);

        Map<String, Object> stats = new HashMap<>();
        stats.put("stringVal", "test value");
        stats.put("intVal", 42);
        stats.put("doubleVal", 3.14159);
        stats.put("boolVal", true);
        stats.put("nullVal", null);
        result.setStatistics(stats);

        String json = AutoSerializer.toJson(result);
        ComplexHookResult dispatched = AutoSerializer.fromJson(json, ComplexHookResult.class);

        assertNotNull("hookedClasses should survive dispatch", dispatched.getHookedClasses());
        assertEquals("hookedClasses size", 3, dispatched.getHookedClasses().size());
        for (int i = 0; i < classes.size(); i++) {
            assertEquals("hookedClasses[" + i + "]",
                        classes.get(i), dispatched.getHookedClasses().get(i));
        }

        assertNotNull("statistics should survive dispatch", dispatched.getStatistics());
        assertEquals("stringVal", "test value", dispatched.getStatistics().get("stringVal"));
        assertEquals("intVal", 42.0, dispatched.getStatistics().get("intVal"));
        assertEquals("doubleVal", 3.14159, (Double) dispatched.getStatistics().get("doubleVal"), 0.0001);
        assertEquals("boolVal", true, dispatched.getStatistics().get("boolVal"));
        assertNull("nullVal should stay null", dispatched.getStatistics().get("nullVal"));

        System.out.println("✓ Complex collections (List + Map) fully preserved");
    }

    // ============================================
    // Section 3: 自动序列化对象分配测试
    // ============================================

    @Test
    public void testAutoSerializable_DispatchRoundTrip() {
        System.out.println("\n=== Test 7: AutoSerializable Dispatch Round-Trip ===");

        AutoHookRequest original = new AutoHookRequest();
        original.setTargetClass("com.auto.Test");
        original.setTargetMethod("autoMethod");
        original.setDebugMode(true);
        original.setTimeout(7777);

        String json = original.autoToJson();
        System.out.println("AutoHookRequest autoToJson() output:\n" + json);

        AutoHookRequest dispatched = original.autoFromJson(json);

        assertSameType("AutoHookRequest", original, dispatched);
        assertEquals("targetClass", original.getTargetClass(), dispatched.getTargetClass());
        assertEquals("targetMethod", original.getTargetMethod(), dispatched.getTargetMethod());
        assertTrue("debugMode", dispatched.isDebugMode());
        assertEquals("timeout", 7777, dispatched.getTimeout());

        System.out.println("✓ AutoSerializable round-trip successful");
    }

    @Test
    public void testAutoSerializable_MultipleDispatchCycles() {
        System.out.println("\n=== Test 8: Multiple Dispatch Cycles (Stability) ===");

        AutoHookResult original = new AutoHookResult();
        original.setSuccess(true);
        original.setMessage("Cycle test");
        original.setHookId("cycle-001");

        AutoHookResult current = original;
        for (int cycle = 1; cycle <= 5; cycle++) {
            String json = current.autoToJson();
            current = current.autoFromJson(json);

            assertTrue("Cycle " + cycle + ": success should persist", current.isSuccess());
            assertEquals("Cycle " + cycle + ": message should persist",
                        "Cycle test", current.getMessage());
            assertEquals("Cycle " + cycle + ": hookId should persist",
                        "cycle-001", current.getHookId());
        }

        System.out.println("✓ Survived 5 serialization/deserialization cycles without data loss");
    }

    // ============================================
    // Section 4: 端到端分配流程测试
    // ============================================

    @Test
    public void testEndToEnd_CompleteDispatchFlow() throws Exception {
        System.out.println("\n=== Test 9: Complete End-to-End Dispatch Flow ===");

        // Phase 1: CLI → Request (ParamParser)
        String[] cliArgs = {"-d", "com.example.App", "onCreate", "--timeout=5000"};
        ComplexHookRequest cliRequest = ParamParser.parse(ComplexHookRequest.class, cliArgs);

        // Phase 2: Request → JSON (AutoSerializer)
        String requestJson = AutoSerializer.toJson(cliRequest);
        System.out.println("Phase 2 - Request JSON ready for network transfer");

        // Phase 3: JSON → Server Request (Simulate server receive)
        ComplexHookRequest serverRequest = AutoSerializer.fromJson(requestJson, ComplexHookRequest.class);
        assertSameType("Server received correct type", cliRequest, serverRequest);

        // Phase 4: Server Request → Result (Business logic)
        ComplexHookResult serverResult = new ComplexHookResult();
        serverResult.setSuccess(true);
        serverResult.setHookId("e2e-dispatch-" + System.currentTimeMillis());
        serverResult.setMessage("Processed: " + serverRequest.getTargetClass() + "." + serverRequest.getTargetMethod());
        serverResult.setHookedClasses(Arrays.asList(serverRequest.getTargetClass()));
        serverResult.setOriginalRequest(serverRequest);

        // Phase 5: Result → Client JSON (AutoSerializer)
        String resultJson = AutoSerializer.toJson(serverResult);
        System.out.println("Phase 5 - Result JSON ready for client");

        // Phase 6: Client JSON → Client Result (Simulate client receive)
        ComplexHookResult clientResult = AutoSerializer.fromJson(resultJson, ComplexHookResult.class);
        assertSameType("Client received correct type", serverResult, clientResult);

        // Phase 7: Verify complete data integrity
        assertTrue(clientResult.isSuccess());
        assertNotNull(clientResult.getMessage());
        assertNotNull(clientResult.getHookId());
        assertNotNull(clientResult.getHookedClasses());
        assertEquals(1, clientResult.getHookedClasses().size());
        assertNotNull(clientResult.getOriginalRequest());
        assertSameType("Nested request in client result",
                      serverRequest, clientResult.getOriginalRequest());
        assertEquals("com.example.App", clientResult.getOriginalRequest().getTargetClass());
        assertTrue(clientResult.getOriginalRequest().isDebugMode());
        assertEquals(5000, clientResult.getOriginalRequest().getTimeout());

        System.out.println("✓ Complete end-to-end dispatch flow verified!");
        System.out.println("  CLI args → Request → JSON → Server → Result → JSON → Client");
        System.out.println("  All types preserved ✓ All data intact ✓");
    }

    @Test
    public void testCrossSerializer_Interoperability() throws Exception {
        System.out.println("\n=== Test 10: Cross-Serializer Interoperability ===");

        ComplexHookRequest original = createComplexHookRequest();

        // Path A: AutoSerializer serialize → Gson deserialize
        String autoJson = AutoSerializer.toJson(original);
        ComplexHookRequest fromAutoToGson = gson.fromJson(autoJson, ComplexHookRequest.class);

        // Path B: Gson serialize → AutoSerializer deserialize
        String gsonJson = gson.toJson(original);
        ComplexHookRequest fromGsonToAuto = AutoSerializer.fromJson(gsonJson, ComplexHookRequest.class);

        System.out.println("Path A (Auto→Gson): Types match? " +
                          (original.getClass() == fromAutoToGson.getClass()));
        System.out.println("Path B (Gson→Auto): Types match? " +
                          (original.getClass() == fromGsonToAuto.getClass()));

        // Both paths should produce equivalent objects
        assertEquals("Path A: targetClass", original.getTargetClass(), fromAutoToGson.getTargetClass());
        assertEquals("Path B: targetClass", original.getTargetClass(), fromGsonToAuto.getTargetClass());
        assertEquals("Path A: debugMode", original.isDebugMode(), fromAutoToGson.isDebugMode());
        assertEquals("Path B: debugMode", original.isDebugMode(), fromGsonToAuto.isDebugMode());
        assertEquals("Path A: timeout", original.getTimeout(), fromAutoToGson.getTimeout());
        assertEquals("Path B: timeout", original.getTimeout(), fromGsonToAuto.getTimeout());

        System.out.println("✓ Cross-serializer interoperability verified");
    }

    // ============================================
    // Section 5: 边界条件与压力测试
    // ============================================

    @Test
    public void testDispatch_LargeDataset_Integrity() {
        System.out.println("\n=== Test 11: Large Dataset Integrity ===");

        AutoHookRequest bigRequest = new AutoHookRequest();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longDesc.append("Line ").append(i).append(": ");
            for (int j = 0; j < 50; j++) {
                longDesc.append((char) ('A' + (j % 26)));
            }
            longDesc.append(". ");
        }
        bigRequest.setDescription(longDesc.toString());
        bigRequest.setTargetClass("com." + String.join(".", Collections.nCopies(20, "deep")) + ".Class");
        bigRequest.setOutputPath("/" + String.join("/", Arrays.asList("very", "deep", "path", "with", "many", "levels")) + "/file.txt");

        String json = AutoSerializer.toJson(bigRequest);
        System.out.println("Large dataset - Original desc length: " + longDesc.length());
        System.out.println("Large dataset - JSON length: " + json.length());

        AutoHookResult bigResult = new AutoHookResult();
        bigResult.setSuccess(true);
        bigResult.setMessage(longDesc.toString());

        List<String> manyClasses = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyClasses.add("com.package" + i + ".Class" + i + "$Inner" + i);
        }
        bigResult.setHookedClasses(manyClasses);

        Map<String, Object> bigStats = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            bigStats.put("metric_" + i, random.nextInt(10000));
        }
        bigResult.setStatistics(bigStats);

        String resultJson = AutoSerializer.toJson(bigResult);
        AutoHookResult dispatchedResult = AutoSerializer.fromJson(resultJson, AutoHookResult.class);

        assertEquals("Large description should survive",
                    longDesc.length(), dispatchedResult.getMessage().length());
        assertNotNull("Many classes should survive", dispatchedResult.getHookedClasses());
        assertEquals("100 classes should survive", 100, dispatchedResult.getHookedClasses().size());
        assertNotNull("Big statistics should survive", dispatchedResult.getStatistics());
        assertEquals("50 metrics should survive", 50, dispatchedResult.getStatistics().size());

        System.out.println("✓ Large dataset (200+ lines, 100 classes, 50 metrics) integrity verified");
    }

    @Test
    public void testDispatch_SpecialCharacters_Escaping() {
        System.out.println("\n=== Test 12: Special Characters Escaping ===");

        AutoHookRequest specialReq = new AutoHookRequest();
        specialReq.setTargetClass("com.example.Class$Inner<Generic>");
        specialReq.setTargetMethod("<init>(String, int[])");
        specialReq.setDescription("Special: \"quotes\" 'apostrophes' & <html> \\n\\t \u00E9\u00F1\u00FC");

        String json = AutoSerializer.toJson(specialReq);
        System.out.println("Special chars JSON:\n" + json);

        AutoHookResult specialRes = new AutoHookResult();
        specialRes.setSuccess(true);
        specialRes.setMessage(specialReq.getDescription());

        String resJson = AutoSerializer.toJson(specialRes);
        AutoHookResult dispatched = AutoSerializer.fromJson(resJson, AutoHookResult.class);

        assertEquals("Unicode and special chars should survive exactly",
                    specialReq.getDescription(), dispatched.getMessage());

        System.out.println("✓ Special characters (Unicode, HTML entities, quotes) correctly escaped/unescaped");
    }

    // ============================================
    // Helper Methods
    // ============================================

    private ComplexHookRequest createComplexHookRequest() {
        ComplexHookRequest req = new ComplexHookRequest();
        req.setTargetClass("com.example.MainActivity");
        req.setTargetMethod("onCreate(Bundle)");
        req.setHookType("before");
        req.setOutputPath("/sdcard/hooks/test.log");
        req.setDebugMode(true);
        req.setThreadSafe(false);
        req.setTimeout(3000);
        req.setMaxRetries(5);
        req.setDescription("Test hook for lifecycle monitoring with unicode: 中文日本語한국어");
        return req;
    }

    private ComplexHookResult createComplexHookResult() {
        ComplexHookResult result = new ComplexHookResult();
        result.setSuccess(true);
        result.setMessage("Hook installed successfully");
        result.setRequestId("req-dispatch-test-001");
        result.setHookId("hook-dispatch-001");
        result.setHookedClasses(Arrays.asList(
            "com.example.Activity",
            "com.example.Fragment",
            "com.example.Service"
        ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHooks", 5);
        stats.put("activeHooks", 3);
        stats.put("memoryUsage", "2.5MB");
        stats.put("avgLatency", 145.5);
        result.setStatistics(stats);

        return result;
    }

    private void assertSameType(String context, Object expected, Object actual) {
        assertNotNull(context + ": Expected object should not be null", expected);
        assertNotNull(context + ": Actual object should not be null", actual);
        assertEquals(context + ": Class type mismatch",
                    expected.getClass(), actual.getClass());
        System.out.println("  ✓ " + context + ": Type = " + actual.getClass().getSimpleName());
    }

    private void assertAllFieldsEqual(ComplexHookRequest expected, ComplexHookRequest actual) {
        assertEquals("targetClass", expected.getTargetClass(), actual.getTargetClass());
        assertEquals("targetMethod", expected.getTargetMethod(), actual.getTargetMethod());
        assertEquals("hookType", expected.getHookType(), actual.getHookType());
        assertEquals("outputPath", expected.getOutputPath(), actual.getOutputPath());
        assertEquals("debugMode", expected.isDebugMode(), actual.isDebugMode());
        assertEquals("threadSafe", expected.isThreadSafe(), actual.isThreadSafe());
        assertEquals("timeout", expected.getTimeout(), actual.getTimeout());
        assertEquals("maxRetries", expected.getMaxRetries(), actual.getMaxRetries());
        assertEquals("description", expected.getDescription(), actual.getDescription());
        System.out.println("  ✓ All ComplexHookRequest fields equal");
    }

    private void assertResultFieldsEqual(ComplexHookResult expected, ComplexHookResult actual) {
        assertEquals("success", expected.isSuccess(), actual.isSuccess());
        if (expected.getRequestId() != null) {
            assertEquals("requestId", expected.getRequestId(), actual.getRequestId());
        }
        assertEquals("hookId", expected.getHookId(), actual.getHookId());
        assertEquals("message", expected.getMessage(), actual.getMessage());

        if (expected.getHookedClasses() != null) {
            assertNotNull("hookedClasses not null", actual.getHookedClasses());
            assertEquals("hookedClasses size",
                       expected.getHookedClasses().size(),
                       actual.getHookedClasses().size());
            for (int i = 0; i < expected.getHookedClasses().size(); i++) {
                assertEquals("hookedClasses[" + i + "]",
                           expected.getHookedClasses().get(i),
                           actual.getHookedClasses().get(i));
            }
        }

        if (expected.getStatistics() != null) {
            assertNotNull("statistics not null", actual.getStatistics());
            assertEquals("statistics size",
                       expected.getStatistics().size(),
                       actual.getStatistics().size());
            for (String key : expected.getStatistics().keySet()) {
                Object expVal = expected.getStatistics().get(key);
                Object actVal = actual.getStatistics().get(key);
                if (expVal instanceof Number && actVal instanceof Number) {
                    assertEquals("statistics[" + key + "]",
                              ((Number) expVal).doubleValue(),
                              ((Number) actVal).doubleValue(),
                              0.001);
                } else {
                    assertEquals("statistics[" + key + "]", expVal, actVal);
                }
            }
        }

        System.out.println("  ✓ All ComplexHookResult fields equal");
    }
}
