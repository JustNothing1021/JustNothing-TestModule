package com.justnothing.testmodule.command.functions.bytecode.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BYTECODE_VER;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.bytecode.util.SystemBytecodeExtractor;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Modifier;
import java.util.Arrays;


import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("bytecode")
public class BytecodeMain extends MainCommand<BytecodeRequest, BytecodeResult> {

    private final String commandName;

    public BytecodeMain() {
        super("Bytecode", BytecodeResult.class);
        this.commandName = "bytecode";
    }

    public BytecodeMain(String commandName) {
        super(commandName, BytecodeResult.class);
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "binfo" -> String.format("""
                    语法: binfo [options] <class>
                    
                    查看类的字节码信息.
                    
                    可选项:
                        -v, --verbose                详细输出
                    
                    示例:
                        binfo java.lang.String
                        binfo -v java.util.ArrayList
                    
                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
            case "banalyze" -> String.format("""
                    语法: banalyze [options] <class>
                    
                    分析类的字节码结构.
                    
                    可选项:
                        -v, --verbose                详细输出
                    
                    示例:
                        banalyze java.lang.String
                        banalyze -v java.util.ArrayList
                    
                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
            case "bdecompile" -> String.format("""
                    语法: bdecompile [options] <class>
                    
                    反编译类为Java代码.
                    
                    可选项:
                        -o, --output <path>         指定输出文件路径
                    
                    示例:
                        bdecompile java.lang.String
                        bdecompile com.example.MyClass -o /sdcard/MyClass.java
                    
                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
            default -> String.format("""
                    语法: bytecode <subcmd> [args...]
                    
                    查看和分析Java字节码，研究类和方法实现.
                    
                    子命令:
                        info <class_name>                         - 查看类的字节码信息
                        method <class_name> <method_name>         - 查看指定方法的字节码
                        dump <class_name>                         - 导出类的字节码到文件
                        analyze <class_name>                      - 分析类的字节码结构
                        disasm <class_name> [method_name]         - 反汇编字节码
                        constants <class_name>                    - 查看常量池
                        verify <class_name>                       - 验证字节码有效性
                        decompile <class_name>                    - 反编译为Java代码
                        batch_export                              - 批量导出类型信息
                        list_classes                              - 列出所有类
                    
                    选项:
                        -o, --output <path>                      - 指定输出文件路径
                        -v, --verbose                            - 详细输出
                        -h, --hex                                - 以十六进制格式显示
                    
                    示例:
                        bytecode info java.lang.String
                        bytecode method java.lang.String valueOf
                        bytecode dump java.lang.String -o /sdcard/String.class
                        bytecode analyze java.util.ArrayList
                        bytecode disasm java.lang.String
                        bytecode disasm java.lang.String valueOf
                        bytecode constants java.lang.String
                        bytecode verify java.lang.String
                        bytecode decompile java.lang.String
                        bytecode decompile com.example.MyClass -o /sdcard/MyClass.java
                    
                    快捷命令:
                        binfo      - 等同于 bytecode info
                        banalyze   - 等同于 bytecode analyze
                        bdecompile - 等同于 bytecode decompile
                    
                    注意:
                        - 需要类已加载才能查看字节码
                        - 反汇编使用ASM库
                        - 反编译使用CFR库
                        - 常量池包含类的所有字面量和符号引用
                        - 字节码验证会检查类文件的完整性
                        - DEX格式可能无法完全分析，建议使用dexdump工具
                        - 使用上层命令指定ClassLoader（如：methods -cl android）
                    
                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
        };
    }

    @Override
    public BytecodeResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            if (shouldReturnStructuredData(context)) {
                return createErrorResult("参数不足，需要指定子命令");
            }
            return null;
        }

        String subcmd = args[0];
        String outputPath = null;
        boolean verbose = false;
        boolean hexFormat = false;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-o", "--output" -> {
                    if (i + 1 < args.length) {
                        outputPath = args[++i];
                    }
                }
                case "-v", "--verbose" -> verbose = true;
                case "-h", "--hex" -> hexFormat = true;
            }
        }

        ClassLoader classLoader = context.classLoader();
        String resultOutput = null;
        String targetClassName = args.length >= 2 ? args[1] : null;

        switch (subcmd) {
            case "info" -> resultOutput = handleInfo(args, classLoader, verbose, context);
            case "method" -> resultOutput = handleMethod(args, classLoader, hexFormat, context);
            case "dump" -> resultOutput = handleDump(args, classLoader, outputPath, context);
            case "analyze" -> resultOutput = handleAnalyze(args, classLoader, verbose, context);
            case "disasm" -> resultOutput = handleDisasm(args, classLoader, context);
            case "constants" -> resultOutput = handleConstants(args, classLoader, context);
            case "verify" -> resultOutput = handleVerify(args, classLoader, context);
            case "decompile" -> resultOutput = handleDecompile(args, classLoader, outputPath, context);
            case "batch_export" -> { handleBatchExport(classLoader, outputPath, context); resultOutput = "批量导出完成"; }
            case "list_classes" -> { handleListClasses(classLoader, context); resultOutput = "类列表已输出"; }
            default -> {
                context.println("未知子命令: " + subcmd, Colors.RED);
                context.println(getHelpText(), Colors.WHITE);
                if (shouldReturnStructuredData(context)) {
                    return createErrorResult("未知子命令: " + subcmd);
                }
            }
        }
        if (shouldReturnStructuredData(context)) {
            BytecodeResult result = new BytecodeResult(java.util.UUID.randomUUID().toString());
            result.setSubCommand(subcmd);
            result.setClassName(targetClassName);
            result.setOutput(resultOutput);
            return result;
        }
        return null;
    }

    private String handleInfo(String[] args, ClassLoader classLoader, boolean verbose, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

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

            return sb.toString();

        } catch (Exception e) {
            logger.error("获取类信息失败", e);
            return CommandExceptionHandler.handleException("bytecode", e, context, "获取类信息失败");
        }
    }

    private String handleMethod(String[] args, ClassLoader classLoader, boolean hexFormat, CommandExecutor.CmdExecContext context) {
        if (args.length < 3) {
            return "参数不足，需要指定类名和方法名";
        }

        String className = args[1];
        String methodName = args[2];

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
                return "找不到方法: " + methodName;
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

            return sb.toString();

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "获取方法信息失败");
        }
    }

    private String handleDump(String[] args, ClassLoader classLoader, String outputPath, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        if (outputPath == null) {
            outputPath = "/sdcard/" + className.replace('.', '_') + ".class";
        }

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                return "无法获取类字节码";
            }

            IOManager.writeFile(outputPath, bytecode);

            logger.info("字节码已导出到: " + outputPath);
            return "字节码已导出到: " + outputPath + "\n大小: " + bytecode.length + " 字节";

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "导出字节码失败");
        }
    }

    private String handleAnalyze(String[] args, ClassLoader classLoader, boolean verbose, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
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

            return sb.toString();

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "分析字节码失败");
        }
    }

    private String handleDisasm(String[] args, ClassLoader classLoader, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];
        String methodName = args.length > 2 ? args[2] : null;

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);
            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
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

            return sb.toString();

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "反汇编失败");
        }
    }

    private String handleConstants(String[] args, ClassLoader classLoader, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
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

            int constantPoolCount = ((bytecode[8] & 0xFF) << 8) | (bytecode[9] & 0xFF);

            StringBuilder sb = new StringBuilder();
            sb.append("常量池: ").append(className).append("\n");
            sb.append("常量数量: ").append(constantPoolCount - 1).append("\n\n");

            sb.append("注意: 完整的常量池解析需要复杂的字节码分析\n");
            sb.append("这里显示类中声明的常量字段:\n\n");

            java.lang.reflect.Field[] fields = targetClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
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

            return sb.toString();

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "获取常量池失败");
        }
    }

    private String handleVerify(String[] args, ClassLoader classLoader, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                return "无法获取类字节码";
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

            return sb.toString();

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "验证字节码失败");
        }
    }

    private String handleDecompile(String[] args, ClassLoader classLoader, String outputPath, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);
            if (bytecode == null) {
                ClassLoader cl = targetClass.getClassLoader();
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

            String decompiledCode = decompileWithCFR(className);

            if (outputPath != null) {
                IOManager.writeFile(outputPath, decompiledCode.getBytes());
                logger.info("反编译代码已导出到: " + outputPath);
                return "反编译代码已导出到: " + outputPath + "\n\n" + decompiledCode;
            } else {
                return "反编译结果:\n\n" + decompiledCode;
            }

        } catch (Exception e) {
            return CommandExceptionHandler.handleException("bytecode", e, context, "反编译失败");
        }
    }

    private void handleBatchExport(ClassLoader classLoader, String outputPath, CommandExecutor.CmdExecContext context) {
        if (outputPath == null) {
            outputPath = "/sdcard/class_dump_" + System.currentTimeMillis() + "/";
        }

        try {
            SystemBytecodeExtractor.exportAllClasses(classLoader, outputPath);
            context.println("批量导出完成，保存到: " + outputPath, Colors.LIGHT_GREEN);
        } catch (Exception e) {
            logger.error("批量导出失败", e);
            context.println("错误: " + e.getMessage(), Colors.RED);
        }
    }

    private void handleListClasses(ClassLoader classLoader, CommandExecutor.CmdExecContext context) {
        try {
            Map<String, byte[]> classes = SystemBytecodeExtractor.getAllClassesBytecode(classLoader);

            StringBuilder sb = new StringBuilder();
            sb.append("类加载器: ").append(classLoader.getClass().getName()).append("\n");
            sb.append("已加载类数: ").append(classes.size()).append("\n\n");

            List<String> classNames = new ArrayList<>(classes.keySet());
            Collections.sort(classNames);

            int count = 0;
            for (String className : classNames) {
                sb.append(className).append("\n");
                count++;
                if (count >= 100) {
                    sb.append("... (总共 ").append(classes.size()).append(" 个类)\n");
                    break;
                }
            }

            context.println(sb.toString(), Colors.DEFAULT);
        } catch (Exception e) {
            logger.error("获取类列表失败", e);
            context.println("错误: " + e.getMessage(), Colors.RED);
        }
    }

    private Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className, classLoader);
    }

    private byte[] getClassBytecode(Class<?> clazz) {
        return SystemBytecodeExtractor.getClassBytecode(clazz);
    }

    private String disasmClassWithASM(byte[] bytecode) {
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

    private String disasmMethodWithASM(Method method, byte[] bytecode) {
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

    private MethodBytecodeInfo getMethodBytecodeInfo(ClassReader reader, Method method) {
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

    private String decompileWithCFR(String className) {
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

    private static class MethodBytecodeInfo {
        int maxStack;
        int maxLocals;
        int codeLength;
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }
        return sb.toString();
    }
}
