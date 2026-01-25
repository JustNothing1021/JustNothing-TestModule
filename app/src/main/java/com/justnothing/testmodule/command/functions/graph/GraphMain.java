package com.justnothing.testmodule.command.functions.graph;

import static com.justnothing.testmodule.constants.CommandServer.CMD_GRAPH_VER;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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
        
        List<Class<?>> hierarchy = getClassHierarchy(clazz);
        
        sb.append("继承层次（从顶层父类到当前类）:\n");
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);
            
            for (int j = 0; j < i; j++) {
                sb.append("  ");
            }
            sb.append("└─> ").append(currentClass.getSimpleName()).append("\n");
            
            Class<?>[] interfaces = currentClass.getInterfaces();
            if (interfaces.length > 0) {
                for (int j = 0; j < i + 1; j++) {
                    sb.append("  ");
                }
                sb.append("├─ 实现接口: ");
                for (int k = 0; k < interfaces.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(interfaces[k].getSimpleName());
                }
                sb.append("\n");
            }
        }
        
        sb.append("\n子类:\n");
        List<Class<?>> subclasses = findSubclasses(clazz);
        if (subclasses.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            for (Class<?> subclass : subclasses) {
                sb.append("  ").append(subclass.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private List<Class<?>> getClassHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        
        return hierarchy;
    }

    private List<Class<?>> findSubclasses(Class<?> superClass) {
        List<Class<?>> subclasses = new ArrayList<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(superClass.getClassLoader());
            
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
            Method[] methods = clazz.getDeclaredMethods();
            List<Method> targetMethods = new ArrayList<>();
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethods.add(method);
                }
            }
            
            if (targetMethods.isEmpty()) {
                return "错误: 未找到方法: " + methodName;
            }
            
            if (targetMethods.size() > 1) {
                sb.append("找到 ").append(targetMethods.size()).append(" 个重载方法:\n\n");
            }
            
            for (int idx = 0; idx < targetMethods.size(); idx++) {
                Method targetMethod = targetMethods.get(idx);
                
                if (targetMethods.size() > 1) {
                    sb.append("--- 重载方法 #").append(idx + 1).append(" ---\n");
                }
                
                sb.append("方法签名:\n");
                sb.append("  ").append(getMethodSignature(targetMethod)).append("\n\n");
                
                sb.append("访问修饰符: ").append(getMethodModifiers(targetMethod)).append("\n");
                
                sb.append("参数类型:\n");
                Class<?>[] paramTypes = targetMethod.getParameterTypes();
                if (paramTypes.length == 0) {
                    sb.append("  (无参数)\n");
                } else {
                    for (int i = 0; i < paramTypes.length; i++) {
                        sb.append("  ").append(i + 1).append(". ").append(paramTypes[i].getName()).append("\n");
                    }
                }
                
                sb.append("\n返回类型: ").append(targetMethod.getReturnType().getName()).append("\n");
                
                Class<?>[] exceptions = targetMethod.getExceptionTypes();
                if (exceptions.length > 0) {
                    sb.append("\n抛出的异常:\n");
                    for (Class<?> exception : exceptions) {
                        sb.append("  ").append(exception.getName()).append("\n");
                    }
                }
                
                sb.append("\n方法继承关系:\n");
                analyzeMethodInheritance(targetMethod, clazz, sb);
                
                sb.append("\n方法调用关系:\n");
                analyzeMethodCalls(targetMethod, clazz, sb);
                
                if (idx < targetMethods.size() - 1) {
                    sb.append("\n");
                }
            }
            
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
        
        return sb.toString();
    }

    private String getMethodSignature(Method method) {
        StringBuilder sig = new StringBuilder();
        
        sig.append(getMethodModifiers(method)).append(" ");
        sig.append(method.getReturnType().getSimpleName()).append(" ");
        sig.append(method.getName()).append("(");
        
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sig.append(", ");
            }
            sig.append(params[i].getSimpleName());
        }
        
        sig.append(")");
        
        return sig.toString();
    }

    private String getMethodModifiers(Method method) {
        StringBuilder mods = new StringBuilder();
        
        int modifiers = method.getModifiers();
        
        if (java.lang.reflect.Modifier.isPublic(modifiers)) {
            mods.append("public");
        } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
            mods.append("protected");
        } else if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
            mods.append("private");
        } else {
            mods.append("[package-private]");
        }
        
        if (java.lang.reflect.Modifier.isStatic(modifiers)) {
            mods.append(" static");
        }
        if (java.lang.reflect.Modifier.isFinal(modifiers)) {
            mods.append(" final");
        }
        if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
            mods.append(" synchronized");
        }
        if (java.lang.reflect.Modifier.isNative(modifiers)) {
            mods.append(" native");
        }
        if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
            mods.append(" abstract");
        }
        
        return mods.toString();
    }

    private void analyzeMethodInheritance(Method method, Class<?> clazz, StringBuilder sb) {
        sb.append("  └─> 当前类: ").append(clazz.getSimpleName()).append("\n");
        
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            try {
                Method superMethod = superClass.getMethod(method.getName(), method.getParameterTypes());
                if (superMethod != null) {
                    sb.append("    └─> 重写自: ").append(superClass.getSimpleName()).append(".").append(superMethod.getName()).append("\n");
                }
            } catch (NoSuchMethodException e) {
                sb.append("    └─> 父类: ").append(superClass.getSimpleName()).append(" (未找到同名方法)\n");
            }
        }
        
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            try {
                Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                if (ifaceMethod != null) {
                    sb.append("    └─> 实现自接口: ").append(iface.getSimpleName()).append(".").append(ifaceMethod.getName()).append("\n");
                }
            } catch (NoSuchMethodException e) {
            }
        }
    }

    private void analyzeMethodCalls(Method method, Class<?> clazz, StringBuilder sb) {
        sb.append("  (静态分析: 需要字节码分析才能准确显示调用关系)\n");
        sb.append("  当前仅显示方法基本信息\n");
        
        sb.append("\n  相关方法:\n");
        
        try {
            Method[] allMethods = clazz.getDeclaredMethods();
            Set<String> relatedMethods = new HashSet<>();
            
            for (Method m : allMethods) {
                if (m.getName().contains(method.getName()) || 
                    method.getName().contains(m.getName())) {
                    if (!m.equals(method) && !relatedMethods.contains(m.getName())) {
                        relatedMethods.add(m.getName());
                    }
                }
            }
            
            if (relatedMethods.isEmpty()) {
                sb.append("    (未找到相关方法)\n");
            } else {
                for (String relatedName : relatedMethods) {
                    sb.append("    └─> ").append(relatedName).append("()\n");
                }
            }
        } catch (Exception e) {
            sb.append("    (分析失败: ").append(e.getMessage()).append(")\n");
        }
    }

    private String generatePackageDependencyGraph(String packageName, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 包依赖图 =====\n");
        sb.append("包名: ").append(packageName).append("\n\n");
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);
            
            Set<String> dependencies = new HashSet<>();
            
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
        
        for (Method method : clazz.getDeclaredMethods()) {
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
