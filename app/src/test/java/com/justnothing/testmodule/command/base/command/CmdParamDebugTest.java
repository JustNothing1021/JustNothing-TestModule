package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.utils.CmdParamProcessor;

import org.junit.Test;

import static org.junit.Assert.*;

public class CmdParamDebugTest {

    @Test
    public void debugBasicTest() throws Exception {
        System.out.println("=== 开始调试测试 ===");
        
        MemoryInfoRequest request = new MemoryInfoRequest();
        System.out.println("初始状态: heapOnly=" + request.isHeapOnly());
        
        var fields = CmdParamProcessor.getCmdParamFields(MemoryInfoRequest.class);
        System.out.println("扫描到 " + fields.size() + " 个字段");
        
        for (var fi : fields) {
            System.out.println("  字段: " + fi.field().getName() +
                ", 参数名: " + fi.param().name() +
                ", 别名: " + java.util.Arrays.toString(fi.param().aliases()));
        }
        
        String[] args = {"--heap"};
        System.out.println("\n解析参数: " + java.util.Arrays.toString(args));
        
        try {
            CmdParamProcessor.parseCommandLineArgs(request, args);
            System.out.println("解析后: heapOnly=" + request.isHeapOnly());
            assertTrue("heapOnly 应该为 true", request.isHeapOnly());
        } catch (Exception e) {
            System.out.println("❌ 解析失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        System.out.println("=== 测试通过 ===\n");
    }

    @Test
    public void debugAliasTest() throws Exception {
        System.out.println("=== 别名调试测试 ===");
        
        MemoryInfoRequest request = new MemoryInfoRequest();
        
        String[] args = {"-h"};
        System.out.println("解析参数: " + java.util.Arrays.toString(args));
        
        try {
            CmdParamProcessor.parseCommandLineArgs(request, args);
            System.out.println("解析后: heapOnly=" + request.isHeapOnly());
            assertTrue("heapOnly 应该为 true (别名 -h)", request.isHeapOnly());
        } catch (Exception e) {
            System.out.println("❌ 别名解析失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        System.out.println("=== 别名测试通过 ===\n");
    }
}
