package com.justnothing.testmodule.command.functions.nativecmd.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.functions.nativecmd.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NativeManageCommand extends AbstractNativeCommand<CommandRequest<?>, NativeResult> {

    @SuppressWarnings("unchecked")
    public NativeManageCommand() {
        super("native manage", (Class) CommandRequest.class, NativeResult.class);
    }

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

            out(Text.zhEn("Native方法: ", "Native methods: ").text(), Colors.CYAN);
            outln(className, Colors.GREEN);
            out(NativeTexts.LABEL_COUNT.text(), Colors.CYAN);
            outln(NativeTexts.COUNT_UNIT.format(nativeMethods.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (Method method : nativeMethods) {
                out("  " + Modifier.toString(method.getModifiers()), Colors.GRAY);
                out(" " + method.getReturnType().getSimpleName(), Colors.CYAN);
                out(" " + method.getName(), Colors.GREEN);
                outln("(" + Arrays.toString(method.getParameterTypes()) + ")", Colors.GRAY);

                if (verbose) {
                    String signature = manager.getNativeSignature(method);
                    out(Text.zhEn("    JNI签名: ", "    JNI signature: ").text(), Colors.CYAN);
                    outln(signature, Colors.GRAY);
                }
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native cli", e, context, Text.zhEn("获取native方法失败", "Failed to get native methods").text());
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

            out(Text.zhEn("Native栈跟踪", "Native stack trace").text(), Colors.CYAN);
            if (threadId != null) {
                out(" (TID: ", Colors.GRAY);
                out(threadId, Colors.YELLOW);
                out(")", Colors.GRAY);
            }
            outln(":", Colors.CYAN);
            outln("", Colors.WHITE);
            outln(stackTrace, Colors.GRAY);

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native stack", e, context, Text.zhEn("获取native栈失败", "Failed to get the native stack trace").text());
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

            out(Text.zhEn("搜索结果: \"", "Search results: \"").text(), Colors.CYAN);
            out(pattern, Colors.YELLOW);
            outln("\"", Colors.CYAN);
            out(NativeTexts.LABEL_COUNT.text(), Colors.CYAN);
            outln(NativeTexts.COUNT_UNIT.format(results.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String result : results) {
                out("  - ", Colors.GRAY);
                outln(result, Colors.GREEN);
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native search", e, context, Text.zhEn("搜索失败", "Search failed").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("search");
        r.setSuccess(true);
        return r;
    }

    @Override
    protected NativeResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof NativeCliRequest r) return handleCli(r);
        if (request instanceof NativeStackRequest r) return handleStack(r);
        if (request instanceof NativeSearchRequest r) return handleSearch(r);

        throw new IllegalArgumentException(
                NativeTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
