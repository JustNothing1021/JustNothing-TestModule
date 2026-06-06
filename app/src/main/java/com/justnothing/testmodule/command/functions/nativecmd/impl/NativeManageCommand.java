package com.justnothing.testmodule.command.functions.nativecmd.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.NativeManager;
import com.justnothing.testmodule.command.functions.nativecmd.NativeResult;
import com.justnothing.testmodule.command.functions.nativecmd.request.*;
import com.justnothing.testmodule.command.output.Colors;

import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NativeManageCommand extends AbstractNativeCommand<CommandRequest, NativeResult> {

    public NativeResult handleCli(NativeCliRequest request) {
        String className = request.getClassName();
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

            Method[] methods = targetClass.getDeclaredMethods();
            List<Method> nativeMethods = new ArrayList<>();

            for (Method method : methods) {
                if (Modifier.isNative(method.getModifiers())) {
                    nativeMethods.add(method);
                }
            }

            out("Native方法: ", Colors.CYAN);
            outln(className, Colors.GREEN);
            out("数量: ", Colors.CYAN);
            outln(nativeMethods.size() + " 个", Colors.YELLOW);
            outln("", Colors.WHITE);

            for (Method method : nativeMethods) {
                out("  " + Modifier.toString(method.getModifiers()), Colors.GRAY);
                out(" " + method.getReturnType().getSimpleName(), Colors.CYAN);
                out(" " + method.getName(), Colors.GREEN);
                outln("(" + Arrays.toString(method.getParameterTypes()) + ")", Colors.GRAY);

                if (verbose) {
                    String signature = manager.getNativeSignature(method);
                    out("    JNI签名: ", Colors.CYAN);
                    outln(signature, Colors.GRAY);
                }
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native cli", e, context, "获取native方法失败");
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("cli");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleStack(NativeStackRequest request) {
        String threadId = request.getThreadId();

        try {
            String stackTrace = manager.getNativeStackTrace(threadId);

            out("Native栈跟踪", Colors.CYAN);
            if (threadId != null) {
                out(" (TID: ", Colors.GRAY);
                out(threadId, Colors.YELLOW);
                out(")", Colors.GRAY);
            }
            outln(":", Colors.CYAN);
            outln("", Colors.WHITE);
            outln(stackTrace, Colors.GRAY);

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native stack", e, context, "获取native栈失败");
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("stack");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleSearch(NativeSearchRequest request) {
        String pattern = request.getPattern();

        try {
            List<String> libraries = manager.getLoadedLibraries();
            List<String> results = new ArrayList<>();

            for (String lib : libraries) {
                if (lib.contains(pattern)) {
                    results.add(lib);
                }

                List<String> symbols = manager.getLibrarySymbols(lib);
                for (String symbol : symbols) {
                    if (symbol.contains(pattern)) {
                        results.add(lib + "::" + symbol);
                    }
                }
            }

            out("搜索结果: \"", Colors.CYAN);
            out(pattern, Colors.YELLOW);
            outln("\"", Colors.CYAN);
            out("数量: ", Colors.CYAN);
            outln(results.size() + " 个", Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String result : results) {
                out("  - ", Colors.GRAY);
                outln(result, Colors.GREEN);
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native search", e, context, "搜索失败");
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("search");
        r.setSuccess(true);
        return r;
    }

    @Override
    protected NativeResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof NativeCliRequest r) return handleCli(r);
        if (request instanceof NativeStackRequest r) return handleStack(r);
        if (request instanceof NativeSearchRequest r) return handleSearch(r);

        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
