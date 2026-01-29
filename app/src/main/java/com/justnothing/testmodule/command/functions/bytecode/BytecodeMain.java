package com.justnothing.testmodule.command.functions.bytecode;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BYTECODE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class BytecodeMain extends CommandBase {

    private String commandName;

    public BytecodeMain() {
        super("Bytecode");
        this.commandName = "bytecode";
    }

    public BytecodeMain(String commandName) {
        super(commandName);
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        if ("binfo".equals(commandName)) {
            return String.format("""
                    语法: binfo [options] <class>

                    查看类的字节码信息.

                    可选项:
                        -v, --verbose                详细输出

                    示例:
                        binfo java.lang.String
                        binfo -v java.util.ArrayList

                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
        } else if ("banalyze".equals(commandName)) {
            return String.format("""
                    语法: banalyze [options] <class>

                    分析类的字节码结构.

                    可选项:
                        -v, --verbose                详细输出

                    示例:
                        banalyze java.lang.String
                        banalyze -v java.util.ArrayList

                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
        } else if ("bdecompile".equals(commandName)) {
            return String.format("""
                    语法: bdecompile [options] <class>

                    反编译类为Java代码.

                    可选项:
                        -o, --output <path>         指定输出文件路径

                    示例:
                        bdecompile java.lang.String
                        bdecompile com.example.MyClass -o /sdcard/MyClass.java

                    (Submodule bytecode %s)
                    """, CMD_BYTECODE_VER);
        } else {
            return String.format("""
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
        }
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();

        if (args.length < 1) {
            return getHelpText();
        }

        String subcmd = args[0];
        String targetPackage = context.targetPackage();
        String outputPath = null;
        boolean verbose = false;
        boolean hexFormat = false;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 < args.length) {
                    outputPath = args[++i];
                }
            } else if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
            } else if (arg.equals("-h") || arg.equals("--hex")) {
                hexFormat = true;
            }
        }

        ClassLoader classLoader = context.classLoader();

        return switch (subcmd) {
            case "info" -> handleInfo(args, classLoader, targetPackage, verbose);
            case "method" -> handleMethod(args, classLoader, targetPackage, hexFormat);
            case "dump" -> handleDump(args, classLoader, targetPackage, outputPath);
            case "analyze" -> handleAnalyze(args, classLoader, targetPackage, verbose);
            case "disasm" -> handleDisasm(args, classLoader, targetPackage, hexFormat);
            case "constants" -> handleConstants(args, classLoader, targetPackage);
            case "verify" -> handleVerify(args, classLoader, targetPackage);
            case "decompile" -> handleDecompile(args, classLoader, targetPackage, outputPath);
            case "batch_export" -> handleBatchExport(args, classLoader, outputPath);
            case "list_classes" -> handleListClasses(args, classLoader);
            default -> "未知子命令: " + subcmd + "\n" + getHelpText();
        };
    }

    private String handleInfo(String[] args, ClassLoader classLoader, String targetPackage, boolean verbose) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className;
            }

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
            if (verbose && interfaces.length > 0) {
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

            java.lang.reflect.Field[] fields = targetClass.getDeclaredFields();
            sb.append("字段: ").append(fields.length).append(" 个\n");
            if (verbose) {
                for (java.lang.reflect.Field field : fields) {
                    sb.append("  - ").append(field.getType().getSimpleName()).append(" ").append(field.getName())
                            .append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            logger.error("获取类信息失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleMethod(String[] args, ClassLoader classLoader, String targetPackage, boolean hexFormat) {
        if (args.length < 3) {
            return "参数不足，需要指定类名和方法名";
        }

        String className = args[1];
        String methodName = args[2];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className;
            }

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
                            sb.append(disasmMethodWithASM(targetClass, targetMethod, bytecode));
                        }
                    }
                } catch (Exception e) {
                    sb.append("\n注意: 无法获取字节码详细信息: ").append(e.getMessage()).append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            logger.error("获取方法信息失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleDump(String[] args, ClassLoader classLoader, String targetPackage, String outputPath) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        if (outputPath == null) {
            outputPath = "/sdcard/" + className.replace('.', '_') + ".class";
        }

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className;
            }

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                return "无法获取类字节码";
            }

            java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath);
            fos.write(bytecode);
            fos.close();

            logger.info("字节码已导出到: " + outputPath);
            return "字节码已导出到: " + outputPath + "\n大小: " + bytecode.length + " 字节";

        } catch (Exception e) {
            logger.error("导出字节码失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleAnalyze(String[] args, ClassLoader classLoader, String targetPackage, boolean verbose) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader.getClass().getName() : "无");
            }

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
            logger.error("分析字节码失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleDisasm(String[] args, ClassLoader classLoader, String targetPackage, boolean hexFormat) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];
        String methodName = args.length > 2 ? args[2] : null;

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader.getClass().getName() : "无");
            }

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
                        sb.append(disasmMethodWithASM(targetClass, method, bytecode));
                        break;
                    }
                }
            } else {
                sb.append(disasmClassWithASM(targetClass, bytecode));
            }

            return sb.toString();

        } catch (Exception e) {
            logger.error("反汇编失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleConstants(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader.getClass().getName() : "无");
            }

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
            logger.error("获取常量池失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleVerify(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className;
            }

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
                Class.forName(targetClass.getName());
                sb.append("✓ 类可以正常加载\n");
            } catch (Exception e) {
                sb.append("❌ 类加载失败: ").append(e.getMessage()).append("\n");
                valid = false;
            }

            sb.append("\n验证结果: ").append(valid ? "✓ 通过" : "❌ 失败").append("\n");

            return sb.toString();

        } catch (Exception e) {
            logger.error("验证字节码失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleDecompile(String[] args, ClassLoader classLoader, String targetPackage, String outputPath) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }

        String className = args[1];

        try {
            Class<?> targetClass = loadClass(className, classLoader);
            if (targetClass == null) {
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader.getClass().getName() : "无");
            }

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

            String decompiledCode = decompileWithCFR(bytecode, className);

            if (outputPath != null) {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath);
                fos.write(decompiledCode.getBytes());
                fos.close();
                logger.info("反编译代码已导出到: " + outputPath);
                return "反编译代码已导出到: " + outputPath + "\n\n" + decompiledCode;
            } else {
                return "反编译结果:\n\n" + decompiledCode;
            }

        } catch (Exception e) {
            logger.error("反编译失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleBatchExport(String[] args, ClassLoader classLoader, String outputPath) {
        if (outputPath == null) {
            outputPath = "/sdcard/class_dump_" + System.currentTimeMillis() + "/";
        }

        try {
            SystemBytecodeExtractor.exportAllClasses(classLoader, outputPath);
            return "批量导出完成，保存到: " + outputPath;
        } catch (Exception e) {
            logger.error("批量导出失败", e);
            return "错误: " + e.getMessage();
        }
    }

    private String handleListClasses(String[] args, ClassLoader classLoader) {
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
                if (count >= 100) { // 只显示前100个
                    sb.append("... (总共 " + classes.size() + " 个类)\n");
                    break;
                }
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("获取类列表失败", e);
            return "错误: " + e.getMessage();
        }
    }

    private Class<?> loadClass(String className, ClassLoader classLoader) {
        try {
            if (classLoader != null) {
                return XposedHelpers.findClass(className, classLoader);
            } else {
                return XposedHelpers.findClass(className, null);
            }
        } catch (Throwable e) {
            logger.error("找不到类: " + className, e);
            return null;
        }
    }

    private byte[] getClassBytecode(Class<?> clazz) {
        return SystemBytecodeExtractor.getEnhancedClassBytecode(clazz);
    }

    private String disasmClassWithASM(Class<?> clazz, byte[] bytecode) {
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

    private String disasmMethodWithASM(Class<?> clazz, Method method, byte[] bytecode) {
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

    private String decompileWithCFR(byte[] bytecode, String className) {
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
                            return new Sink<>() {
                                @Override
                                public void write(Object o) {
                                    if (o != null) {
                                        sb.append(o.toString());
                                    }
                                }
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
