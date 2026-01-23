package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class ClassMain {

    public static class ClassLogger extends Logger {
        @Override
        public String getTag() {
            return "ClassExecutor";
        }
    }

    public static final ClassLogger logger = new ClassLogger();

    public static String getHelpText() {
        return String.format("""
                语法: class [选项] <类名>
                
                查看类的详细信息，包括继承关系、接口、构造函数等.
                
                选项:
                    -i, --interfaces  显示实现的接口
                    -c, --constructors 显示构造函数
                    -s, --super       显示父类信息
                    -m, --modifiers   显示修饰符信息
                    -a, --all         显示所有信息 (默认)
                
                示例:
                    class java.lang.String
                    class -i java.util.ArrayList
                    class -c android.view.View
                
                (Submodule class %s)
                """, CMD_CLASS_VER);
    }

    public static String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行class命令，参数: " + java.util.Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);
        
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean showInterfaces = false;
        boolean showConstructors = false;
        boolean showSuper = false;
        boolean showModifiers = false;
        boolean showAll = true;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            if (arg.equals("-i") || arg.equals("--interfaces")) {
                showInterfaces = true;
                showAll = false;
            } else if (arg.equals("-c") || arg.equals("--constructors")) {
                showConstructors = true;
                showAll = false;
            } else if (arg.equals("-s") || arg.equals("--super")) {
                showSuper = true;
                showAll = false;
            } else if (arg.equals("-m") || arg.equals("--modifiers")) {
                showModifiers = true;
                showAll = false;
            } else if (arg.equals("-a") || arg.equals("--all")) {
                showAll = true;
            }
        }
        
        logger.debug("目标类: " + className + ", 显示全部: " + showAll);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            StringBuilder sb = new StringBuilder();
            
            sb.append("=== 基本信息 ===\n");
            sb.append("类名: ").append(targetClass.getName()).append("\n");
            sb.append("简单类名: ").append(targetClass.getSimpleName()).append("\n");
            sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无").append("\n");
            
            if (showAll || showModifiers) {
                sb.append("修饰符: ").append(getModifiers(targetClass.getModifiers())).append("\n");
            }
            
            sb.append("是否为数组: ").append(targetClass.isArray()).append("\n");
            sb.append("是否为接口: ").append(targetClass.isInterface()).append("\n");
            sb.append("是否为注解: ").append(targetClass.isAnnotation()).append("\n");
            sb.append("是否为枚举: ").append(targetClass.isEnum()).append("\n");
            sb.append("是否为原始类型: ").append(targetClass.isPrimitive()).append("\n");
            sb.append("是否为抽象类: ").append(Modifier.isAbstract(targetClass.getModifiers())).append("\n");
            sb.append("是否为final类: ").append(Modifier.isFinal(targetClass.getModifiers())).append("\n\n");

            if (showAll || showSuper) {
                sb.append("=== 继承关系 ===\n");
                Class<?> superClass = targetClass.getSuperclass();
                if (superClass != null) {
                    sb.append("父类: ").append(superClass.getName()).append("\n");
                    
                    Class<?> current = superClass;
                    int level = 1;
                    while (current != null) {
                        Class<?> parent = current.getSuperclass();
                        if (parent != null) {
                            sb.append("  ".repeat(level)).append("└─ ").append(parent.getName()).append("\n");
                            current = parent;
                            level++;
                        } else {
                            break;
                        }
                    }
                } else {
                    sb.append("父类: 无\n");
                }
                sb.append("\n");
            }

            if (showAll || showInterfaces) {
                sb.append("=== 实现的接口 ===\n");
                Class<?>[] interfaces = targetClass.getInterfaces();
                if (interfaces.length == 0) {
                    sb.append("无接口\n");
                } else {
                    for (Class<?> iface : interfaces) {
                        sb.append("  - ").append(iface.getName()).append("\n");
                    }
                }
                sb.append("接口总数: ").append(interfaces.length).append("\n\n");
            }

            if (showAll || showConstructors) {
                sb.append("=== 构造函数 ===\n");
                Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
                if (constructors.length == 0) {
                    sb.append("无构造函数\n");
                } else {
                    for (Constructor<?> constructor : constructors) {
                        sb.append("  ").append(getConstructorDescriptor(constructor)).append("\n");
                    }
                }
                sb.append("构造函数总数: ").append(constructors.length).append("\n\n");
            }

            if (showAll) {
                sb.append("=== 字段概览 ===\n");
                Field[] fields = targetClass.getDeclaredFields();
                sb.append("字段总数: ").append(fields.length).append("\n");
                int publicFields = 0, privateFields = 0, protectedFields = 0, staticFields = 0;
                for (Field field : fields) {
                    int mod = field.getModifiers();
                    if (Modifier.isPublic(mod)) publicFields++;
                    if (Modifier.isPrivate(mod)) privateFields++;
                    if (Modifier.isProtected(mod)) protectedFields++;
                    if (Modifier.isStatic(mod)) staticFields++;
                }
                sb.append("  public: ").append(publicFields).append("\n");
                sb.append("  private: ").append(privateFields).append("\n");
                sb.append("  protected: ").append(protectedFields).append("\n");
                sb.append("  static: ").append(staticFields).append("\n\n");

                sb.append("=== 方法概览 ===\n");
                Method[] methods = targetClass.getDeclaredMethods();
                sb.append("方法总数: ").append(methods.length).append("\n");
                int publicMethods = 0, privateMethods = 0, protectedMethods = 0, staticMethods = 0;
                for (Method method : methods) {
                    int mod = method.getModifiers();
                    if (Modifier.isPublic(mod)) publicMethods++;
                    if (Modifier.isPrivate(mod)) privateMethods++;
                    if (Modifier.isProtected(mod)) protectedMethods++;
                    if (Modifier.isStatic(mod)) staticMethods++;
                }
                sb.append("  public: ").append(publicMethods).append("\n");
                sb.append("  private: ").append(privateMethods).append("\n");
                sb.append("  protected: ").append(protectedMethods).append("\n");
                sb.append("  static: ").append(staticMethods).append("\n\n");

                sb.append("=== 包信息 ===\n");
                sb.append("包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
                sb.append("类加载器: ").append(classLoader != null ? classLoader.toString() : "无").append("\n");
            }
            
            logger.info("执行成功");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行class命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private static String getModifiers(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        
        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        
        return sb.toString().trim();
    }

    private static String getConstructorDescriptor(Constructor<?> constructor) {
        StringBuilder sb = new StringBuilder();
        int modifiers = constructor.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        sb.append(constructor.getName()).append("(");

        Class<?>[] params = constructor.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }
}
