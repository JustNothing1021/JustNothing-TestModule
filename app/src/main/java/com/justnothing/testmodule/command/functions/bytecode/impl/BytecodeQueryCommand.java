package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeInfoRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeMethodRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeAnalyzeRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDisasmRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeConstantsRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeVerifyRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDecompileRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.io.IOManager;

import org.objectweb.asm.ClassReader;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

public class BytecodeQueryCommand extends AbstractBytecodeCommand<CommandRequest> {

    public BytecodeQueryCommand() {
        super("bytecode query", CommandRequest.class);
    }

    @Override
    protected BytecodeResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();
        ClassLoader classLoader = context.classLoader();

        if (request instanceof BytecodeInfoRequest req) {
            return handleInfo(req, classLoader, context);
        } else if (request instanceof BytecodeMethodRequest req) {
            return handleMethod(req, classLoader, context);
        } else if (request instanceof BytecodeAnalyzeRequest req) {
            return handleAnalyze(req, classLoader, context);
        } else if (request instanceof BytecodeDisasmRequest req) {
            return handleDisasm(req, classLoader, context);
        } else if (request instanceof BytecodeConstantsRequest req) {
            return handleConstants(req, classLoader, context);
        } else if (request instanceof BytecodeVerifyRequest req) {
            return handleVerify(req, classLoader, context);
        } else if (request instanceof BytecodeDecompileRequest req) {
            return handleDecompile(req, classLoader, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private BytecodeResult handleInfo(BytecodeInfoRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        boolean verbose = request.isVerbose();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            StringBuilder sb = new StringBuilder();
            sb.append("类名: ").append(targetClass.getName()).append("\n");
            sb.append("类加载器: ").append(targetClass.getClassLoader()).append("\n");
            sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无")
                    .append("\n");
            sb.append("修饰符: ").append(Modifier.toString(targetClass.getModifiers())).append("\n");
            sb.append("父类: ").append(targetClass.getSuperclass() != null ? targetClass.getSuperclass().getName() : "无")
                    .append("\n");

            Class<?>[] interfaces = targetClass.getInterfaces();
            sb.append("接口: ").append(interfaces.length).append(" 个\n");
            if (verbose) {
                for (Class<?> iface : interfaces) {
                    sb.append("  - ").append(iface.getName()).append("\n");
                }
            }

            Method[] methods = targetClass.getDeclaredMethods();
            sb.append("方法: ").append(methods.length).append(" 个\n");
            if (verbose) {
                for (Method method : methods) {
                    sb.append("  - ").append(method.getName())
                            .append("(").append(Arrays.toString(method.getParameterTypes())).append(")\n");
                }
            }

            Field[] fields = targetClass.getDeclaredFields();
            sb.append("字段: ").append(fields.length).append(" 个\n");
            if (verbose) {
                for (Field field : fields) {
                    sb.append("  - ").append(field.getType().getSimpleName()).append(" ").append(field.getName())
                            .append("\n");
                }
            }

            return buildSuccessResult("info", className, sb.toString());

        } catch (Exception e) {
            logger.error("获取类信息失败", e);
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取类信息失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleMethod(BytecodeMethodRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        boolean hexFormat = request.isHexFormat();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            Method[] methods = targetClass.getDeclaredMethods();
            Method targetMethod = null;

            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }

            if (targetMethod == null) {
                return buildErrorResult("找不到方法: " + methodName);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("方法: ").append(targetMethod.getName()).append("\n");
            sb.append("修饰符: ").append(Modifier.toString(targetMethod.getModifiers())).append("\n");
            sb.append("返回类型: ").append(targetMethod.getReturnType().getName()).append("\n");
            sb.append("参数类型: ").append(Arrays.toString(targetMethod.getParameterTypes())).append("\n");
            sb.append("异常: ").append(Arrays.toString(targetMethod.getExceptionTypes())).append("\n");

            byte[] bytecode = getClassBytecode(targetClass);
            if (bytecode != null) {
                try {
                    ClassReader reader = new ClassReader(bytecode);
                    MethodBytecodeInfo info = getMethodBytecodeInfo(reader, targetMethod);

                    if (info != null) {
                        sb.append("\n字节码信息:\n");
                        sb.append("  最大栈深度: ").append(info.maxStack).append("\n");
                        sb.append("  局部变量表大小: ").append(info.maxLocals).append("\n");
                        sb.append("  字节码长度: ").append(info.codeLength).append(" 字节\n");

                        if (hexFormat) {
                            sb.append("\n字节码（十六进制）:\n");
                            sb.append(disasmMethodWithASM(targetMethod, bytecode));
                        }
                    }
                } catch (Exception e) {
                    sb.append("\n注意: 无法获取字节码详细信息: ").append(e.getMessage()).append("\n");
                }
            }

            return buildSuccessResult("method", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取方法信息失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleAnalyze(BytecodeAnalyzeRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        boolean verbose = request.isVerbose();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
                return buildErrorResult(buildBytecodeUnavailableMessage(className, cl));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("字节码分析: ").append(className).append("\n");
            sb.append("文件大小: ").append(bytecode.length).append(" 字节\n");

            if (bytecode.length >= 4) {
                sb.append("魔数: 0x");
                for (int i = 0; i < 4; i++) {
                    sb.append(String.format("%02X", bytecode[i] & 0xFF));
                }
                sb.append("\n");
            }

            if (bytecode.length >= 8) {
                int minorVersion = ((bytecode[4] & 0xFF) << 8) | (bytecode[5] & 0xFF);
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                sb.append("版本: ").append(majorVersion).append(".").append(minorVersion).append("\n");
            }

            if (bytecode.length >= 10) {
                int constantPoolCount = ((bytecode[8] & 0xFF) << 8) | (bytecode[9] & 0xFF);
                sb.append("常量池大小: ").append(constantPoolCount - 1).append(" 个常量\n");
            }

            if (verbose) {
                sb.append("\n字节码头部（前64字节）:\n");
                int bytesToShow = Math.min(64, bytecode.length);
                for (int i = 0; i < bytesToShow; i += 16) {
                    sb.append(String.format("%04X: ", i));
                    for (int j = 0; j < 16 && i + j < bytesToShow; j++) {
                        sb.append(String.format("%02X ", bytecode[i + j] & 0xFF));
                    }
                    sb.append("\n");
                }
            }

            return buildSuccessResult("analyze", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "分析字节码失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleDisasm(BytecodeDisasmRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        String methodName = request.getMethodName();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);
            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
                return buildErrorResult(buildBytecodeUnavailableMessage(className, cl));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("反汇编: ").append(className).append("\n\n");

            if (methodName != null) {
                Method[] methods = targetClass.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        sb.append(disasmMethodWithASM(method, bytecode));
                        break;
                    }
                }
            } else {
                sb.append(disasmClassWithASM(bytecode));
            }

            return buildSuccessResult("disasm", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "反汇编失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleConstants(BytecodeConstantsRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
                return buildErrorResult(buildBytecodeUnavailableMessage(className, cl));
            }

            int constantPoolCount = ((bytecode[8] & 0xFF) << 8) | (bytecode[9] & 0xFF);

            StringBuilder sb = new StringBuilder();
            sb.append("常量池: ").append(className).append("\n");
            sb.append("常量数量: ").append(constantPoolCount - 1).append("\n\n");

            sb.append("注意: 完整的常量池解析需要复杂的字节码分析\n");
            sb.append("这里显示类中声明的常量字段:\n\n");

            Field[] fields = targetClass.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(null);
                        sb.append("  ").append(field.getType().getSimpleName())
                                .append(" ").append(field.getName())
                                .append(" = ").append(value).append("\n");
                    } catch (Exception e) {
                        sb.append("  ").append(field.getType().getSimpleName())
                                .append(" ").append(field.getName())
                                .append(" = [无法获取值]\n");
                    }
                }
            }

            return buildSuccessResult("constants", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取常量池失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleVerify(BytecodeVerifyRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                return buildErrorResult("无法获取类字节码");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("字节码验证: ").append(className).append("\n\n");

            boolean valid = true;

            if (bytecode.length < 4) {
                sb.append("❌ 字节码太短\n");
                valid = false;
            } else {
                int magic = ((bytecode[0] & 0xFF) << 24) | ((bytecode[1] & 0xFF) << 16) |
                        ((bytecode[2] & 0xFF) << 8) | (bytecode[3] & 0xFF);
                if (magic == 0xCAFEBABE) {
                    sb.append("✓ 魔数正确: 0xCAFEBABE\n");
                } else {
                    sb.append("❌ 魔数错误: 0x").append(String.format("%08X", magic)).append("\n");
                    valid = false;
                }
            }

            if (bytecode.length >= 8) {
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                sb.append("✓ 版本: ").append(majorVersion).append("\n");
            }

            if (bytecode.length >= 10) {
                int constantPoolCount = ((bytecode[8] & 0xFF) << 8) | (bytecode[9] & 0xFF);
                sb.append("✓ 常量池大小: ").append(constantPoolCount - 1).append("\n");
            }

            try {
                ClassResolver.findClassOrFail(className, classLoader);
                sb.append("✓ 类可以正常加载\n");
            } catch (Exception e) {
                sb.append("❌ 类加载失败: ").append(e.getMessage()).append("\n");
                valid = false;
            }

            sb.append("\n验证结果: ").append(valid ? "✓ 通过" : "❌ 失败").append("\n");

            return buildSuccessResult("verify", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "验证字节码失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleDecompile(BytecodeDecompileRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        String outputPath = request.getOutputPath();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);
            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
                return buildErrorResult(buildBytecodeUnavailableMessage(className, cl));
            }

            String decompiledCode = decompileWithCFR(className);

            if (outputPath != null) {
                IOManager.writeFile(outputPath, decompiledCode.getBytes());
                logger.info("反编译代码已导出到: " + outputPath);
                return buildSuccessResult("decompile", className,
                        "反编译代码已导出到: " + outputPath + "\n\n" + decompiledCode);
            } else {
                return buildSuccessResult("decompile", className, "反编译结果:\n\n" + decompiledCode);
            }

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "反编译失败");
            return buildErrorResult(errorMsg);
        }
    }
}
