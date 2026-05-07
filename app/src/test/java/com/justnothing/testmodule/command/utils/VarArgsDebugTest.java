package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import org.junit.Test;

import static org.junit.Assert.*;

public class VarArgsDebugTest {

    @Test
    public void testInvokeMethodRequest_VarArgsParsing() throws Exception {
        System.out.println("\n=== Debug Test: InvokeMethodRequest varArgs ===\n");

        String[] testArgs = {"java.lang.Math", "sqrt", "16.0"};
        System.out.println("Input args: " + java.util.Arrays.toString(testArgs));

        InvokeMethodRequest request = InvokeMethodRequest.class.newInstance().fromCommandLine(testArgs);

        System.out.println("Parsed className: " + request.getClassName());
        System.out.println("Parsed methodName: " + request.getMethodName());
        System.out.println("Parsed paramsRaw: " + request.getClass().getDeclaredField("paramsRaw"));
        System.out.println("Parsed params: " + request.getParams());
        System.out.println("Params size: " + request.getParams().size());

        assertEquals("className should be 'java.lang.Math'", "java.lang.Math", request.getClassName());
        assertEquals("methodName should be 'sqrt'", "sqrt", request.getMethodName());
        assertNotNull("params should not be null", request.getParams());
        assertEquals("❌ FAIL: params should contain 1 element (16.0)", 1, request.getParams().size());
        assertEquals("❌ FAIL: params[0] should be '16.0'", "16.0", request.getParams().get(0));
    }

    @Test
    public void testInvokeMethodRequest_MultipleVarArgs() throws Exception {
        System.out.println("\n=== Debug Test: Multiple varArgs ===\n");

        String[] testArgs = {"java.lang.String", "substring", "2", "5"};
        System.out.println("Input args: " + java.util.Arrays.toString(testArgs));

        InvokeMethodRequest request = InvokeMethodRequest.class.newInstance().fromCommandLine(testArgs);

        assertEquals("className should be 'java.lang.String'", "java.lang.String", request.getClassName());
        assertEquals("methodName should be 'substring'", "substring", request.getMethodName());
        assertEquals("❌ FAIL: params should contain 2 elements", 2, request.getParams().size());
        assertEquals("params[0] should be '2'", "2", request.getParams().get(0));
        assertEquals("params[1] should be '5'", "5", request.getParams().get(1));
    }

    @Test
    public void testParamParser_VarArgsDirectly() {
        System.out.println("\n=== Debug Test: ParamParser varArgs directly ===\n");

        String[] testArgs = {"java.lang.Math", "sqrt", "16.0", "25.0", "true"};
        System.out.println("Input args to ParamParser: " + java.util.Arrays.toString(testArgs));

        InvokeMethodRequest request = ParamParser.parse(InvokeMethodRequest.class, testArgs);

        System.out.println("Parsed className: " + request.getClassName());
        System.out.println("Parsed methodName: " + request.getMethodName());

        try {
            java.lang.reflect.Field paramsRawField = InvokeMethodRequest.class.getDeclaredField("paramsRaw");
            paramsRawField.setAccessible(true);
            String paramsRaw = (String) paramsRawField.get(request);
            System.out.println("Parsed paramsRaw: '" + paramsRaw + "'");
        } catch (Exception e) {
            System.out.println("Failed to read paramsRaw: " + e.getMessage());
        }

        System.out.println("Parsed params (before fromCommandLine): " + request.getParams());

        assertEquals("className should be 'java.lang.Math'", "java.lang.Math", request.getClassName());
        assertEquals("methodName should be 'sqrt'", "sqrt", request.getMethodName());
        assertNotNull("params should not be null", request.getParams());

        System.out.println("\n⚠️ 已知限制: ParamParser.parse() 不执行 varArgs 后处理");
        System.out.println("   必须调用 fromCommandLine() 才能正确填充 params 列表\n");

        InvokeMethodRequest fullParsed = request.fromCommandLine(testArgs);
        System.out.println("After fromCommandLine(): params = " + fullParsed.getParams());
        assertEquals("❌ FAIL: After fromCommandLine, params should contain 3 elements",
                     3, fullParsed.getParams().size());
    }
}
