package com.justnothing.testmodule.command.framework;

import com.google.gson.Gson;
import com.justnothing.methodsclient.metadata.CommandMetadataScanner;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 验证 GsonFactory 新的注解驱动策略：
 * 请求类（@CmdParam）应正常序列化/反序列化；
 * 结果/模型类（@Expose）行为保持不变。
 */
public class GsonFactoryAnnotationTest {

    /**
     * Request 的 commandType 由路由派生，构造 Request 之前必须先把路由表建好。
     * 生产环境由 CommandExecutor（服务端）/ UiClient（GUI）保证，单元测试需显式触发。
     */
    @BeforeClass
    public static void registerRoutes() {
        CommandMetadataScanner.ensureRegistered();
    }

    @Test
    public void testClassInfoRequestSerializeContainsClassName() {
        ClassInfoRequest request = new ClassInfoRequest("java.lang.String");
        request.setShowInterfaces(true);
        request.setShowMethods(true);

        String json = request.toJsonString();
        assertTrue("className 应被序列化, 实际: " + json, json.contains("\"className\":\"java.lang.String\""));
        assertTrue("showInterfaces 应被序列化", json.contains("\"showInterfaces\":true"));
        assertTrue("showMethods(无@CmdParam) 应被序列化", json.contains("\"showMethods\":true"));
    }

    @Test
    public void testClassInfoRequestRoundTrip() {
        ClassInfoRequest request = new ClassInfoRequest("java.util.ArrayList");
        request.setVerbose(true);
        request.setShowConstructors(true);

        String json = request.toJsonString();
        ClassInfoRequest restored = new ClassInfoRequest().fromJsonString(json);

        assertEquals("className 应往返一致", "java.util.ArrayList", restored.getClassName());
        assertTrue("verbose 应往返一致", restored.isVerbose());
        assertTrue("showConstructors 应往返一致", restored.isShowConstructors());
    }

    @Test
    public void testCommandResultExposeBehaviorPreserved() {
        CommandResult result = new CommandResult("req-x");
        result.setMessage("hello");
        result.setData("payload");

        String json = result.toJsonString();
        assertTrue("requestId 应序列化", json.contains("\"requestId\":\"req-x\""));
        assertTrue("message 应序列化", json.contains("\"message\":\"hello\""));
        assertTrue("data 应序列化", json.contains("\"data\":\"payload\""));
    }

    @Test
    public void testClassInfoResultExposeBehavior() {
        ClassInfoResult result = new ClassInfoResult("req-y");
        result.setSuccess(true);
        result.setMessage("ok");

        String json = result.toJsonString();
        // classInfo 为 null 时不应输出字段
        assertFalse("classInfo 为 null 不应序列化", json.contains("classInfo"));
    }

    @Test
    public void testGsonFactorySingleInstance() {
        Gson g1 = GsonFactory.getInstance();
        Gson g2 = GsonFactory.getInstance();
        assertSame("Gson 实例应单例", g1, g2);
    }

    /**
     * 结果类（CommandResult 子类）无 @Expose 的数据字段应默认参与序列化——
     * 这是 GUI 能拿到结构化数据的前提（此前无注解字段被静默丢弃）。
     */
    @Test
    public void testResultSubclassFieldsSerializedWithoutExpose() {
        InvokeConstructorResult result = new InvokeConstructorResult("req-z");
        result.setSuccess(true);
        result.setResultString("java.lang.String@1a2b");
        result.setResultTypeName("java.lang.String");
        result.setResultHash(4660);

        String json = result.toJsonString();
        assertTrue("resultString 应被序列化, 实际: " + json,
                json.contains("\"resultString\":\"java.lang.String@1a2b\""));
        assertTrue("resultTypeName 应被序列化, 实际: " + json,
                json.contains("\"resultTypeName\":\"java.lang.String\""));
        assertTrue("resultHash 应被序列化, 实际: " + json, json.contains("\"resultHash\":4660"));
    }

    @Test
    public void testResultSubclassRoundTrip() {
        InvokeConstructorResult original = new InvokeConstructorResult("req-w");
        original.setSuccess(true);
        original.setResultString("abc");
        original.setResultTypeName("java.lang.String");
        original.setResultHash(42);

        InvokeConstructorResult restored =
                new InvokeConstructorResult().fromJsonString(original.toJsonString());

        assertEquals("resultString 应往返一致", "abc", restored.getResultString());
        assertEquals("resultTypeName 应往返一致", "java.lang.String", restored.getResultTypeName());
        assertEquals("resultHash 应往返一致", 42, restored.getResultHash());
        assertTrue("success 应往返一致", restored.isSuccess());
    }

    /**
     * 结构化 error 必须上线（否则客户端 {@code postError(result.getError(), ...)} 永远拿到 null），
     * 但 {@code ErrorInfo.stacktrace} 必须保持排除——堆栈只留在服务端。
     */
    @Test
    public void testErrorIsSerializedButStacktraceIsNot() {
        CommandResult result = new CommandResult("req-e");
        result.setSuccess(false);
        result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "boom", new RuntimeException("x")));

        String json = result.toJsonString();
        assertTrue("error 字段应上线, 实际: " + json, json.contains("\"error\""));
        assertTrue("error.code 应上线, 实际: " + json, json.contains("INTERNAL_ERROR"));
        assertFalse("stacktrace 不应出现", json.contains("stacktrace"));
    }

    @Test
    public void testClassInfoRequestIsCommandRequest() {
        assertTrue("ClassInfoRequest 应是 CommandRequest 子类",
                CommandRequest.class.isAssignableFrom(ClassInfoRequest.class));
    }
}
