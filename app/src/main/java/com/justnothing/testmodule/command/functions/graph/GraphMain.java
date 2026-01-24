package com.justnothing.testmodule.command.functions.graph;

import static com.justnothing.testmodule.constants.CommandServer.CMD_GRAPH_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class GraphMain extends CommandBase {

    public GraphMain() {
        super("GraphMain");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: graph <subcmd> [args...]
                
                生成类继承图、方法调用图或包依赖图.
                
                子命令:
                    class <class_name>              - 生成类继承图
                    call <class_name> <method>      - 生成方法调用图
                    dependency <package_name>       - 生成包依赖图
                
                示例:
                    graph class java.util.ArrayList
                    graph call com.example.MyClass myMethod
                    graph dependency com.example
                
                注意:
                    - 类继承图显示类的继承层次结构
                    - 方法调用图显示方法的调用关系
                    - 包依赖图显示包之间的依赖关系
                    - 图形以文本形式显示，便于阅读

                
                (Submodule graph %s)
                """, CMD_GRAPH_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行graph命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "class":
                    return handleClassGraph(args, classLoader);
                case "call":
                    return handleCallGraph(args, classLoader);
                case "dependency":
                    return handleDependencyGraph(args, classLoader);
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("执行graph命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleClassGraph(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: graph class <class_name>";
        }

        try {
            String className = args[1];
            Class<?> clazz = Class.forName(className, false, classLoader);
            
            return generateClassInheritanceGraph(clazz);
        } catch (ClassNotFoundException e) {
            return "错误: 未找到类: " + args[1];
        }
    }

    private String handleCallGraph(String[] args, ClassLoader classLoader) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: graph call <class_name> <method>";
        }

        try {
            String className = args[1];
            String methodName = args[2];
            Class<?> clazz = Class.forName(className, false, classLoader);
            
            return generateMethodCallGraph(clazz, methodName);
        } catch (ClassNotFoundException e) {
            return "错误: 未找到类: " + args[1];
        }
    }

    private String handleDependencyGraph(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: graph dependency <package_name>";
        }

        try {
            String packageName = args[1];
            return generatePackageDependencyGraph(packageName, classLoader);
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }

    private String generateClassInheritanceGraph(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 类继承图 =====\n");
        sb.append("类名: ").append(clazz.getName()).append("\n\n");
        
        sb.append("继承层次:\n");
        printClassHierarchy(clazz, 0, sb);
        
        sb.append("\n实现的接口:\n");
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) {
            sb.append("  (无)\n");
        } else {
            for (Class<?> iface : interfaces) {
                sb.append("  ").append(iface.getName()).append("\n");
            }
        }
        
        sb.append("\n子类:\n");
        java.util.List<Class<?>> subclasses = findSubclasses(clazz);
        if (subclasses.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            for (Class<?> subclass : subclasses) {
                sb.append("  ").append(subclass.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private void printClassHierarchy(Class<?> clazz, int level, StringBuilder sb) {
        if (clazz == null) {
            return;
        }
        
        printClassHierarchy(clazz.getSuperclass(), level + 1, sb);
        
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append("└─ ").append(clazz.getSimpleName()).append("\n");
    }

    private java.util.List<Class<?>> findSubclasses(Class<?> superClass) {
        java.util.List<Class<?>> subclasses = new java.util.ArrayList<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(superClass.getClassLoader());
            
            for (Class<?> clazz : classes) {
                if (superClass.isAssignableFrom(clazz) && !clazz.equals(superClass)) {
                    subclasses.add(clazz);
                }
            }
        } catch (Exception e) {
            logger.warn("查找子类失败", e);
        }
        
        return subclasses;
    }

    private String generateMethodCallGraph(Class<?> clazz, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 方法调用图 =====\n");
        sb.append("类名: ").append(clazz.getName()).append("\n");
        sb.append("方法: ").append(methodName).append("\n\n");
        
        try {
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            java.lang.reflect.Method targetMethod = null;
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                return "错误: 未找到方法: " + methodName;
            }
            
            sb.append("方法签名: ").append(targetMethod.toString()).append("\n\n");
            sb.append("调用关系:\n");
            sb.append("  (需要运行时分析，当前仅显示静态信息)\n\n");
            
            sb.append("参数类型:\n");
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                sb.append("  ").append(paramType.getName()).append("\n");
            }
            
            sb.append("\n返回类型: ").append(targetMethod.getReturnType().getName()).append("\n");
            
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
        
        return sb.toString();
    }

    private String generatePackageDependencyGraph(String packageName, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 包依赖图 =====\n");
        sb.append("包名: ").append(packageName).append("\n\n");
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(classLoader);
            
            java.util.Set<String> dependencies = new java.util.HashSet<>();
            
            for (Class<?> clazz : classes) {
                if (clazz.getPackage() != null && clazz.getPackage().getName().startsWith(packageName)) {
                    analyzeClassDependencies(clazz, dependencies);
                }
            }
            
            if (dependencies.isEmpty()) {
                sb.append("未找到依赖\n");
            } else {
                sb.append("依赖的包:\n");
                for (String dep : dependencies) {
                    sb.append("  ").append(dep).append("\n");
                }
            }
            
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
        
        return sb.toString();
    }

    private void analyzeClassDependencies(Class<?> clazz, java.util.Set<String> dependencies) {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass.getPackage() != null) {
            dependencies.add(superClass.getPackage().getName());
        }
        
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getPackage() != null) {
                dependencies.add(iface.getPackage().getName());
            }
        }
        
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            Class<?> returnType = method.getReturnType();
            if (returnType.getPackage() != null) {
                dependencies.add(returnType.getPackage().getName());
            }
            
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.getPackage() != null) {
                    dependencies.add(paramType.getPackage().getName());
                }
            }
        }
    }
}
