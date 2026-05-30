package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AutoSerializerRegressionTest {

    private ClassInfoResult resultWithFullData;

    @Before
    public void setUp() {
        resultWithFullData = new ClassInfoResult("test-request-001");
        resultWithFullData.setSuccess(true);
        resultWithFullData.setMessage("查询成功");

        ClassInfo classInfo = new ClassInfo();
        classInfo.setName("java.lang.String");
        classInfo.setSuperClass("java.lang.Object");
        classInfo.setModifiers(17);
        classInfo.setInterface(false);
        classInfo.setAnnotation(false);
        classInfo.setEnum(false);

        List<String> interfaces = new ArrayList<>();
        interfaces.add("java.io.Serializable");
        interfaces.add("java.lang.Comparable");
        classInfo.setInterfaces(interfaces);

        List<MethodInfo> methods = createTestMethods();
        classInfo.setMethods(methods);

        List<FieldInfo> fields = createTestFields();
        classInfo.setFields(fields);

        List<MethodInfo> constructors = createTestConstructors();
        classInfo.setConstructors(constructors);

        classInfo.setClassLoader("java.lang.BootClassLoader@12345");

        resultWithFullData.setClassInfo(classInfo);
    }

    // ============================================
    // 回归测试 1: CommandResult.toJson() 必须包含子类业务字段
    // (修复前: 只有 {success, resultType}, 丢失所有业务数据)
    // ============================================

    @Test
    public void testCommandResultToJson_ContainsSubclassBusinessFields() throws Exception {
        String jsonStr = AutoSerializer.toJson(resultWithFullData);

        System.out.println("\n=== Regression Test 1: CommandResult toJson() ===");
        System.out.println("Actual JSON (" + jsonStr.length() + " chars):");
        System.out.println(jsonStr);

        assertNotNull("JSON must not be null", jsonStr);
        assertTrue("JSON must contain 'success'", jsonStr.contains("\"success\""));
        assertTrue("❌ FAIL: JSON must contain 'classInfo' field (subclass business data)",
                   jsonStr.contains("\"classInfo\""));

        assertTrue("classInfo must contain 'name'", jsonStr.contains("\"name\"") && jsonStr.contains("java.lang.String"));
        assertTrue("classInfo must contain 'superClass'", jsonStr.contains("\"superClass\""));
        assertTrue("classInfo must contain 'modifiers': 17", jsonStr.contains("\"modifiers\"") && jsonStr.contains("17"));
        assertTrue("classInfo must contain 'interfaces'", jsonStr.contains("\"interfaces\""));
        assertTrue("classInfo must contain 'methods'", jsonStr.contains("\"methods\""));
        assertTrue("classInfo must contain 'fields'", jsonStr.contains("\"fields\""));
        assertTrue("classInfo must contain 'constructors'", jsonStr.contains("\"constructors\""));
    }

    @Test
    public void testCommandResultToJson_MethodsArrayNotEmpty() throws Exception {
        String jsonStr = AutoSerializer.toJson(resultWithFullData);

        assertNotNull("JSON must not be null", jsonStr);
        assertTrue("JSON must contain 'methods' array", jsonStr.contains("\"methods\""));
        assertTrue("JSON must contain 2 methods (charAt, length)",
                   jsonStr.contains("charAt") && jsonStr.contains("length"));
    }

    @Test
    public void testCommandResultToJson_ConstructorsArrayNotEmpty() throws Exception {
        String jsonStr = AutoSerializer.toJson(resultWithFullData);

        assertNotNull("JSON must not be null", jsonStr);
        assertTrue("JSON must contain 'constructors' array", jsonStr.contains("\"constructors\""));
        assertTrue("JSON must contain constructor",
                   jsonStr.contains("java.lang.String"));
    }

    @Test
    public void testCommandResultToJson_FieldsArrayNotEmpty() throws Exception {
        String jsonStr = AutoSerializer.toJson(resultWithFullData);

        assertNotNull("JSON must not be null", jsonStr);
        assertTrue("JSON must contain 'fields' array", jsonStr.contains("\"fields\""));
        assertTrue("JSON must contain field 'value'",
                   jsonStr.contains("value"));
    }

    // ============================================
    // 回归测试 2: Collection 泛型反序列化必须保持正确类型
    // (修复前: List<MethodInfo> 反序列化为 List<HashMap>, 导致 ClassCastException)
    // ============================================

    @Test
    public void testDeserializeCollection_PreservesMethodInfoType() {
        String json = AutoSerializer.toJson(resultWithFullData);
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);

        assertNotNull("Restored result must not be null", restored);
        assertNotNull("Restored classInfo must not be null", restored.getClassInfo());

        List<MethodInfo> methods = restored.getClassInfo().getMethods();

        assertNotNull("Methods list must not be null", methods);
        assertFalse("Methods list must not be empty", methods.isEmpty());
        assertEquals("Methods count must match", 2, methods.size());

        for (int i = 0; i < methods.size(); i++) {
            MethodInfo method = methods.get(i);

            assertNotEquals("❌ FAIL: Method[" + i + "] must be MethodInfo, not HashMap",
                           "java.util.HashMap", method.getClass().getName());
            assertTrue("Method[" + i + "] must be instance of MethodInfo",
                       method instanceof MethodInfo);

            assertNotNull("Method[" + i + "] name must not be null", method.getName());
            assertNotNull("Method[" + i + "] returnType must not be null", method.getReturnType());
            assertNotNull("Method[" + i + "] parameterTypes must not be null", method.getParameterTypes());
        }
    }

    @Test
    public void testDeserializeCollection_PreservesFieldInfoType() {
        String json = AutoSerializer.toJson(resultWithFullData);
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);

        List<FieldInfo> fields = restored.getClassInfo().getFields();

        assertNotNull("Fields list must not be null", fields);
        assertFalse("Fields list must not be empty", fields.isEmpty());
        assertEquals("Fields count must match", 1, fields.size());

        FieldInfo field = fields.get(0);

        assertNotEquals("❌ FAIL: Field must be FieldInfo, not HashMap",
                       "java.util.HashMap", field.getClass().getName());
        assertTrue("Field must be instance of FieldInfo",
                   field instanceof FieldInfo);

        assertNotNull("Field name must not be null", field.getName());
        assertNotNull("Field type must not be null", field.getType());
        assertNotNull("Field modifiers must be accessible", field.getModifiersString());
    }

    @Test
    public void testDeserializeCollection_PreservesConstructorTypeInfo() {
        String json = AutoSerializer.toJson(resultWithFullData);
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);

        List<MethodInfo> constructors = restored.getClassInfo().getConstructors();

        assertNotNull("Constructors list must not be null", constructors);
        assertFalse("Constructors list must not be empty", constructors.isEmpty());
        assertEquals("Constructors count must match", 1, constructors.size());

        MethodInfo constructor = constructors.get(0);

        assertNotEquals("❌ FAIL: Constructor must be MethodInfo, not HashMap",
                       "java.util.HashMap", constructor.getClass().getName());
        assertTrue("Constructor must be instance of MethodInfo",
                   constructor instanceof MethodInfo);

        assertEquals("Constructor name must be 'java.lang.String'",
                     "java.lang.String", constructor.getName());
        assertNotNull("Constructor parameterTypes must not be null", constructor.getParameterTypes());
    }

    // ============================================
    // 回归测试 3: 端到端完整流程验证
    // (模拟 GUI 客户端接收响应的完整流程)
    // ============================================

    @Test
    public void testEndToEnd_ClassInfoResult_RoundTrip() {
        System.out.println("\n=== Regression Test 3: End-to-End Round Trip ===");

        String serverJson;
        try {
            serverJson = AutoSerializer.toJson(resultWithFullData);
            System.out.println("Server sends:\n" + serverJson.substring(0, Math.min(500, serverJson.length())) + "...");
        } catch (Exception e) {
            fail("❌ FAIL: Server serialization failed: " + e.getMessage());
            return;
        }

        System.out.println("\nClient receives JSON length: " + serverJson.length() + " chars");

        ClassInfoResult clientResult;
        try {
            clientResult = AutoSerializer.fromJson(serverJson, ClassInfoResult.class);
        } catch (Exception e) {
            fail("❌ FAIL: Client deserialization failed: " + e.getMessage());
            return;
        }

        assertNotNull("Client result must not be null", clientResult);
        assertTrue("Client result must be success", clientResult.isSuccess());
        assertNotNull("Client classInfo must not be null", clientResult.getClassInfo());

        ClassInfo info = clientResult.getClassInfo();
        assertEquals("Class name must survive round-trip",
                     "java.lang.String", info.getName());
        assertEquals("Super class must survive round-trip",
                     "java.lang.Object", info.getSuperClass());
        assertEquals("Modifiers must survive round-trip",
                     17, info.getModifiers());
        assertFalse("Must not be interface", info.isInterface());

        assertEquals("Interfaces count must match", 2, info.getInterfaces().size());
        assertTrue("Must contain Serializable",
                   info.getInterfaces().contains("java.io.Serializable"));

        assertEquals("Methods count must match", 2, info.getMethods().size());
        assertEquals("Fields count must match", 1, info.getFields().size());
        assertEquals("Constructors count must match", 1, info.getConstructors().size());

        System.out.println("✅ Round-trip successful! All business data preserved.");
    }

    @Test
    public void testEndToEnd_LargeDataset_PerformanceAndCorrectness() {
        ClassInfoResult largeResult = new ClassInfoResult("perf-test");

        ClassInfo largeClassInfo = new ClassInfo();
        largeClassInfo.setName("com.example.LargeClass");
        largeClassInfo.setSuperClass("java.lang.Object");

        List<MethodInfo> manyMethods = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            MethodInfo m = new MethodInfo();
            m.setName("method" + i);
            m.setReturnType("void");
            m.setParameterTypes(Arrays.asList("String", "int"));
            m.setParameters(Arrays.asList("arg0", "arg1"));
            m.setModifiers(1);
            manyMethods.add(m);
        }
        largeClassInfo.setMethods(manyMethods);

        List<FieldInfo> manyFields = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            FieldInfo f = new FieldInfo();
            f.setName("field" + i);
            f.setType(i % 2 == 0 ? "String" : "int");
            f.setModifiers(i % 2 == 0 ? 2 : 1);
            manyFields.add(f);
        }
        largeClassInfo.setFields(manyFields);

        largeResult.setClassInfo(largeClassInfo);

        long startSerialize = System.currentTimeMillis();
        String json;
        try {
            json = AutoSerializer.toJson(largeResult);
        } catch (Exception e) {
            fail("Serialization failed: " + e.getMessage());
            return;
        }
        long serializeTime = System.currentTimeMillis() - startSerialize;

        long startDeserialize = System.currentTimeMillis();
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);
        long deserializeTime = System.currentTimeMillis() - startDeserialize;

        System.out.println("\n=== Performance Test: Large Dataset ===");
        System.out.println("Methods: 100, Fields: 50");
        System.out.println("Serialization time: " + serializeTime + "ms");
        System.out.println("Deserialization time: " + deserializeTime + "ms");
        System.out.println("JSON size: " + json.length() + " chars");

        assertEquals("All 100 methods must survive round-trip",
                     100, restored.getClassInfo().getMethods().size());
        assertEquals("All 50 fields must survive round-trip",
                     50, restored.getClassInfo().getFields().size());

        for (MethodInfo m : restored.getClassInfo().getMethods()) {
            assertTrue("Every method must be MethodInfo instance", m instanceof MethodInfo);
            assertNotNull("Every method must have name", m.getName());
        }

        for (FieldInfo f : restored.getClassInfo().getFields()) {
            assertTrue("Every field must be FieldInfo instance", f instanceof FieldInfo);
            assertNotNull("Every field must have name", f.getName());
        }

        assertTrue("Serialization should complete within 500ms", serializeTime < 500);
        assertTrue("Deserialization should complete within 500ms", deserializeTime < 500);
    }

    // ============================================
    // 边界条件测试
    // ============================================

    @Test
    public void testBoundary_EmptyCollections() {
        ClassInfoResult emptyResult = new ClassInfoResult();
        ClassInfo emptyInfo = new ClassInfo();
        emptyInfo.setName("EmptyClass");
        emptyInfo.setMethods(new ArrayList<>());
        emptyInfo.setFields(new ArrayList<>());
        emptyInfo.setConstructors(new ArrayList<>());
        emptyInfo.setInterfaces(new ArrayList<>());
        emptyResult.setClassInfo(emptyInfo);

        String json = AutoSerializer.toJson(emptyResult);
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);

        assertNotNull("Restored from empty collections must work", restored);
        assertNotNull("ClassInfo must exist", restored.getClassInfo());
        assertNotNull("Methods list must exist (even if empty)", restored.getClassInfo().getMethods());
        assertTrue("Methods list must be empty", restored.getClassInfo().getMethods().isEmpty());
        assertTrue("Fields list must be empty", restored.getClassInfo().getFields().isEmpty());
        assertTrue("Constructors list must be empty", restored.getClassInfo().getConstructors().isEmpty());
    }

    @Test
    public void testBoundary_NullSafeHandling() {
        ClassInfoResult partialResult = new ClassInfoResult();
        ClassInfo partialInfo = new ClassInfo();
        partialInfo.setName("PartialClass");

        partialResult.setClassInfo(partialInfo);

        String json = AutoSerializer.toJson(partialResult);
        ClassInfoResult restored = AutoSerializer.fromJson(json, ClassInfoResult.class);

        assertNotNull("Must handle partial data gracefully", restored);
        assertNotNull("ClassInfo must exist", restored.getClassInfo());
        assertEquals("Name must be preserved", "PartialClass", restored.getClassInfo().getName());
        assertTrue("Null interfaces should be null or empty list",
                   restored.getClassInfo().getInterfaces() == null || restored.getClassInfo().getInterfaces().isEmpty());
        assertTrue("Null methods should be null or empty list",
                   restored.getClassInfo().getMethods() == null || restored.getClassInfo().getMethods().isEmpty());
    }

    // ============================================
    // Helper 方法
    // ============================================

    private List<MethodInfo> createTestMethods() {
        List<MethodInfo> methods = new ArrayList<>();

        MethodInfo method1 = new MethodInfo();
        method1.setName("charAt");
        method1.setReturnType("char");
        method1.setGenericReturnType("char");
        method1.setParameterTypes(Arrays.asList("int"));
        method1.setParameters(Arrays.asList("index"));
        method1.setGenericParameterTypes(Arrays.asList("int"));
        method1.setModifiers(273);
        method1.setDeclaringClass("java.lang.String");
        method1.setDeclaringClassIsInterface(false);
        methods.add(method1);

        MethodInfo method2 = new MethodInfo();
        method2.setName("length");
        method2.setReturnType("int");
        method2.setGenericReturnType("int");
        method2.setParameterTypes(new ArrayList<>());
        method2.setParameters(new ArrayList<>());
        method2.setGenericParameterTypes(new ArrayList<>());
        method2.setModifiers(273);
        method2.setDeclaringClass("java.lang.String");
        method2.setDeclaringClassIsInterface(false);
        methods.add(method2);

        return methods;
    }

    private List<FieldInfo> createTestFields() {
        List<FieldInfo> fields = new ArrayList<>();

        FieldInfo field1 = new FieldInfo();
        field1.setName("value");
        field1.setType("byte[]");
        field1.setGenericType("byte[]");
        field1.setModifiers(18);
        field1.setDeclaringClass("java.lang.String");
        field1.setDeclaringClassIsInterface(false);
        fields.add(field1);

        return fields;
    }

    private List<MethodInfo> createTestConstructors() {
        List<MethodInfo> constructors = new ArrayList<>();

        MethodInfo ctor = new MethodInfo();
        ctor.setName("java.lang.String");
        ctor.setReturnType("void");
        ctor.setGenericReturnType("void");
        ctor.setParameterTypes(new ArrayList<>());
        ctor.setParameters(new ArrayList<>());
        ctor.setGenericParameterTypes(new ArrayList<>());
        ctor.setModifiers(1);
        ctor.setDeclaringClass("java.lang.String");
        ctor.setDeclaringClassIsInterface(false);
        constructors.add(ctor);

        return constructors;
    }
}
