package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.exception.EvaluationException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UsingAliasTest {

    private ScriptRunner runner;
    private DefaultOutputHandler outputHandler;

    @Before
    public void setUp() {
        runner = new ScriptRunner();
        outputHandler = new DefaultOutputHandler();
    }

    private Object eval(String code) throws Exception {
        runner.getExecutionContext().clearVariables();
        return runner.executeWithResult(code, outputHandler, outputHandler);
    }

    // ========== 基本类型别名测试 ==========

    @Test
    public void usingAlias_basicHashMap() throws Exception {
        Object result = eval("""
            using HMap = java.util.HashMap;
            auto map = new HMap();
            map.put("key", "value");
            map.get("key")
            """);
        assertEquals("value", result);
    }

    @Test
    public void usingAlias_basicArrayList() throws Exception {
        Object result = eval("""
            using AList = java.util.ArrayList;
            auto list = new AList();
            list.add("hello");
            list.add("world");
            list.size()
            """);
        assertEquals(2, ((Number) result).intValue());
    }

    @Test
    public void usingAlias_stringClass() throws Exception {
        Object result = eval("""
            using Str = java.lang.String;
            auto s = new Str("test");
            s.length()
            """);
        assertEquals(4, ((Number) result).intValue());
    }

    @Test
    public void usingAlias_nestedClass() throws Exception {
        Object result = eval("""
            using Entry = java.util.Map.Entry;
            auto map = new HashMap();
            map.put("k", "v");
            auto e = map.entrySet().iterator().next();
            e.getKey()
            """);
        assertEquals("k", result);
    }

    // ========== 多个别名同时使用 ==========

    @Test
    public void usingAlias_multipleAliases() throws Exception {
        Object result = eval("""
            using HMap = java.util.HashMap;
            using AList = java.util.ArrayList;
            auto map = new HMap();
            auto list = new AList();
            map.put("list", list);
            list.add(1);
            list.add(2);
            ((AList)map.get("list")).size()
            """);
        assertEquals(2, ((Number) result).intValue());
    }

    // ========== 链式别名（别名引用别名）==========

    @Test
    public void usingAlias_chainAlias() throws Exception {
        Object result = eval("""
            using HMap = java.util.HashMap;
            using IntMap = HMap;
            auto map = new IntMap();
            map.put("a", 1);
            map.get("a")
            """);
        assertEquals(1, result);
    }

    // ========== 与变量声明结合 ==========

    @Test
    public void usingAlias_typedVariableDeclaration() throws Exception {
        Object result = eval("""
            using ArrayList = java.util.ArrayList;
            ArrayList list = new ArrayList();
            list.add("item");
            list.get(0)
            """);
        assertEquals("item", result);
    }

    // ========== 方法调用和字段访问 ==========

    @Test
    public void usingAlias_methodCalls() throws Exception {
        Object result = eval("""
            using StringBuilder = java.lang.StringBuilder;
            auto sb = new StringBuilder();
            sb.append("Hello").append(" ").append("World");
            sb.toString()
            """);
        assertEquals("Hello World", result);
    }

    @Test
    public void usingAlias_staticMethodAccess() throws Exception {
        Object result = eval("""
            using Objects = java.util.Objects;
            Objects.equals(null, null)
            """);
        assertTrue((Boolean) result);
    }

    // ========== 复杂嵌套场景 ==========

    @Test
    public void usingAlias_complexNesting() throws Exception {
        Object result = eval("""
            using LMap = java.util.LinkedHashMap;
            using LList = java.util.LinkedList;
            auto map = new LMap();
            auto innerList = new LList();
            innerList.add("a");
            innerList.add("b");
            map.put("items", innerList);
            ((LList)map.get("items")).size()
            """);
        assertEquals(2, ((Number) result).intValue());
    }

    // ========== 接口别名 ==========

    @Test
    public void usingAlias_interfaceType() throws Exception {
        Object result = eval("""
            using List = java.util.List;
            auto arrayList = new ArrayList();
            List ref = arrayList;
            ref.add("test");
            ref.size()
            """);
        assertEquals(1, ((Number) result).intValue());
    }
}
