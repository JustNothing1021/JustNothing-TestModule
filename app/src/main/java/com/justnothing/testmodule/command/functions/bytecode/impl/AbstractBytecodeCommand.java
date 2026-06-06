package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.bytecode.util.SystemBytecodeExtractor;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.logging.Logger;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.objectweb.asm.util.Textifier;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public abstract class AbstractBytecodeCommand<Req extends CommandRequest> extends AbstractCommand<Req, BytecodeResult> {

    protected static final Logger logger = Logger.getLoggerForName("BytecodeCmd");

    protected AbstractBytecodeCommand(String commandName, Class<Req> requestType) {
        super(commandName, requestType, BytecodeResult.class);
    }

    @Override
    public BytecodeResult execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        try {
            return executeInternal((CommandExecutor.CmdExecContext<Req>) context);
        } catch (Exception e) {
            logger.error("执行 bytecode 命令失败", e);
            CommandExceptionHandler.handleException("bytecode", e, context, "执行命令失败");
            return buildErrorResult("执行命令失败: " + e.getMessage());
        }
    }

    protected abstract BytecodeResult executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    protected void out(CommandExecutor.CmdExecContext<?> context, String message) {
        out(context, message, Colors.DEFAULT);
    }

    protected void out(CommandExecutor.CmdExecContext<?> context, String message, byte color) {
        context.println(message, color);
    }

    protected BytecodeResult buildSuccessResult(String subCommand, String className, String output) {
        BytecodeResult result = new BytecodeResult(java.util.UUID.randomUUID().toString());
        result.setSuccess(true);
        result.setSubCommand(subCommand);
        result.setClassName(className);
        result.setOutput(output);
        return result;
    }

    protected BytecodeResult buildErrorResult(String message) {
        BytecodeResult result = new BytecodeResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    protected Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className, classLoader);
    }

    protected byte[] getClassBytecode(Class<?> clazz) {
        return SystemBytecodeExtractor.getClassBytecode(clazz);
    }

    protected String disasmClassWithASM(byte[] bytecode) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            ClassReader reader = new ClassReader(bytecode);
            TraceClassVisitor tracer = new TraceClassVisitor(pw);
            reader.accept(tracer, ClassReader.EXPAND_FRAMES);

            return sw.toString();

        } catch (Exception e) {
            logger.error("ASM反汇编失败", e);
            return "错误: " + e.getMessage() + "\n" + getStackTrace(e);
        }
    }

    protected String disasmMethodWithASM(Method method, byte[] bytecode) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            ClassReader reader = new ClassReader(bytecode);

            final String methodName = method.getName();
            final Textifier textifier = new Textifier();

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    if (name.equals(methodName)) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new TraceMethodVisitor(mv, textifier);
                    }
                    return null;
                }
            };

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            textifier.print(pw);
            String result = sw.toString();

            return !result.isEmpty() ? result : "未找到方法: " + methodName;

        } catch (Exception e) {
            logger.error("ASM反汇编方法失败", e);
            return "错误: " + e.getMessage() + "\n" + getStackTrace(e);
        }
    }

    protected MethodBytecodeInfo getMethodBytecodeInfo(ClassReader reader, Method method) {
        try {
            final String methodName = method.getName();
            final MethodBytecodeInfo info = new MethodBytecodeInfo();

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    if (name.equals(methodName)) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                            }

                            @Override
                            public void visitMaxs(int maxStack, int maxLocals) {
                                info.maxStack = maxStack;
                                info.maxLocals = maxLocals;
                                super.visitMaxs(maxStack, maxLocals);
                            }

                            @Override
                            public void visitEnd() {
                                super.visitEnd();
                            }
                        };
                    }
                    return null;
                }
            };

            reader.accept(visitor, 0);
            return info;

        } catch (Exception e) {
            logger.error("获取方法字节码信息失败", e);
            return null;
        }
    }

    protected String decompileWithCFR(String className) {
        try {
            StringBuilder sb = new StringBuilder();

            CfrDriver driver = new CfrDriver.Builder()
                    .withOutputSink(new OutputSinkFactory() {
                        @Override
                        public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                            return Collections.singletonList(SinkClass.DECOMPILED);
                        }

                        @Override
                        public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                            return o -> {
                                if (o != null) sb.append(o);
                            };
                        }
                    })
                    .build();

            List<String> args = new ArrayList<>();
            args.add(className);
            driver.analyse(args);

            return sb.toString();

        } catch (Exception e) {
            logger.error("CFR反编译失败", e);
            return "错误: " + e.getMessage() + "\n" + getStackTrace(e);
        }
    }

    protected String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }
        return sb.toString();
    }

    protected String buildBytecodeUnavailableMessage(String className, ClassLoader cl) {
        String errorMsg = "无法获取类字节码: " + className + "\n";
        errorMsg += "类加载器: " + (cl != null ? cl.getClass().getName() : "null (系统类)") + "\n";
        errorMsg += "说明: \n";
        if (cl == null) {
            errorMsg += "  - 这是Android系统类（如android.*包下的类）\n";
            errorMsg += "  - 系统类位于boot classpath中，无法通过常规方式获取字节码\n";
            errorMsg += "  - 建议使用应用类（非系统类）进行测试\n";
        } else {
            errorMsg += "  - 类加载器无法找到.class文件\n";
            errorMsg += "  - 可能是DEX格式而非标准.class格式\n";
        }
        return errorMsg;
    }

    static class MethodBytecodeInfo {
        int maxStack;
        int maxLocals;
        int codeLength;
    }
}
