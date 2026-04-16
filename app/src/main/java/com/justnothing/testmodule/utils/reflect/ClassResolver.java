package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClassResolver {

    public static final String TAG = "ClassResolver";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static final List<ClassLoader> registeredLoaders = new CopyOnWriteArrayList<>();
    private static ClassLoader apkClassLoader = null;
    private static final Object loaderLock = new Object();

    public static void registerClassLoader(ClassLoader loader) {
        if (loader == null) {
            logger.warn("尝试注册null ClassLoader");
            return;
        }

        if (!registeredLoaders.contains(loader)) {
            registeredLoaders.add(loader);
            logger.debug("注册ClassLoader: " + loader);
        }
    }

    public static void registerApkClassLoader(ClassLoader loader) {
        synchronized (loaderLock) {
            apkClassLoader = loader;
            registerClassLoader(loader);
            logger.info("注册APK ClassLoader");
        }
    }

    public static boolean isTypeCompatible(Class<?> expected, Class<?> actual) {
        if (expected == Void.class && actual == Void.class)
            return true;

        if (actual == Void.class)
            return !expected.isPrimitive();

        if (expected.isPrimitive())
            return ReflectionUtils.isPrimitiveWrapperMatch(expected, actual);

        return expected.isAssignableFrom(actual);
    }


    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        // 如果是可变参数方法
        if (isVarArgs) {
            // 可变参数方法至少要有一个参数（可变参数数组本身）
            if (methodArgsTypes.length == 0)
                return false;

            // 可变参数数组类型是最后一个参数
            Class<?> varArgsType = methodArgsTypes[methodArgsTypes.length - 1];
            if (varArgsType.isArray()) {
                // 获取可变参数的元素类型
                Class<?> varArgsComponentType = varArgsType.getComponentType();

                // 固定参数的数量（不包括可变参数）
                int fixedParamCount = methodArgsTypes.length - 1;

                // 如果传入参数少于固定参数数量，不匹配
                if (usingArgTypes.size() < fixedParamCount) {
                    return false;
                }

                // 检查固定参数
                for (int i = 0; i < fixedParamCount; i++) {
                    Class<?> methodArgType = methodArgsTypes[i];
                    Class<?> usingArgType = usingArgTypes.get(i);
                    if (!isTypeCompatible(methodArgType, usingArgType)) {
                        return false;
                    }
                }

                // 检查可变参数
                for (int i = fixedParamCount; i < usingArgTypes.size(); i++) {
                    Class<?> usingArgType = usingArgTypes.get(i);

                    // 每个可变参数都必须可以赋值给可变参数的元素类型
                    if (!isTypeCompatible(varArgsComponentType, usingArgType)) {
                        return false;
                    }
                }
                return true;
            }
        }

        // 非可变参数方法或处理为普通方法
        if (methodArgsTypes.length != usingArgTypes.size()) {
            return false;
        }

        for (int i = 0; i < methodArgsTypes.length; i++) {
            if (!isTypeCompatible(methodArgsTypes[i], usingArgTypes.get(i))) {
                return false;
            }
        }

        return true;
    }

    // 实际上这仨重载就只用了两次
    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes, boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes), isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, Class<?>[] usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
    }

    public static ClassLoader getApkClassLoader() {
        return apkClassLoader;
    }

    public static Class<?> findClass(String className) {
        return findClass(className, null);
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        switch (className) {
            case "int":
                logger.debug("基本类型: int");
                return int.class;
            case "long":
                logger.debug("基本类型: long");
                return long.class;
            case "float":
                logger.debug("基本类型: float");
                return float.class;
            case "double":
                logger.debug("基本类型: double");
                return double.class;
            case "boolean":
                logger.debug("基本类型: boolean");
                return boolean.class;
            case "char":
                logger.debug("基本类型: char");
                return char.class;
            case "byte":
                logger.debug("基本类型: byte");
                return byte.class;
            case "short":
                logger.debug("基本类型: short");
                return short.class;
            case "void":
                logger.debug("基本类型: void");
                return void.class;
        }
        return findClassInternal(className, classLoader);
    }
    

    
    public static Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {

        switch (className) {
            case "int":
                logger.debug("基本类型: int");
                return int.class;
            case "long":
                logger.debug("基本类型: long");
                return long.class;
            case "float":
                logger.debug("基本类型: float");
                return float.class;
            case "double":
                logger.debug("基本类型: double");
                return double.class;
            case "boolean":
                logger.debug("基本类型: boolean");
                return boolean.class;
            case "char":
                logger.debug("基本类型: char");
                return char.class;
            case "byte":
                logger.debug("基本类型: byte");
                return byte.class;
            case "short":
                logger.debug("基本类型: short");
                return short.class;
            case "void":
                logger.debug("基本类型: void");
                return void.class;
        }

        Class<?> clazz;
        if (className.contains(".")) {
            logger.debug("尝试完整类名: " + className);
            clazz = findClassInternal(className, classLoader);

            if (clazz != null) {
                logger.debug("通过完整类名找到类: " + clazz.getName());
                return clazz;
            }

            String[] parts = className.split("\\.");
            if (parts.length >= 2) {
                for (int i = parts.length - 1; i >= 1; i--) {
                    StringBuilder nestedClassName = new StringBuilder(parts[0]);
                    for (int j = 1; j < parts.length; j++) {
                        if (j < i) {
                            nestedClassName.append('.').append(parts[j]);
                        } else {
                            nestedClassName.append('$').append(parts[j]);
                        }
                    }
                    clazz = findClassInternal(nestedClassName.toString(), classLoader);

                    if (clazz != null) {
                        logger.debug("通过嵌套类名找到类: " + clazz.getName());
                        return clazz;
                    }
                }
            }
        }

        for (String importStmt : imports) {
            String fullClassName;
            if (importStmt.endsWith(".*")) {
                String packageName = importStmt.substring(0, importStmt.length() - 2);
                fullClassName = packageName + "." + className;
            } else {
                fullClassName = importStmt;
                String lastName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                if (!lastName.equals(className)) {
                    continue;
                }
            }
            clazz = findClassInternal(fullClassName, classLoader);
            if (clazz != null) {
                logger.debug("通过导入找到类: " + clazz.getName());
                return clazz;
            }

            String[] parts = fullClassName.split("\\.");
            if (parts.length >= 2) {
                for (int i = parts.length - 1; i >= 1; i--) {
                    StringBuilder nestedClassName = new StringBuilder(parts[0]);
                    for (int j = 1; j < parts.length; j++) {
                        if (j < i) {
                            nestedClassName.append('.').append(parts[j]);
                        } else {
                            nestedClassName.append('$').append(parts[j]);
                        }
                    }
                    clazz = findClassInternal(nestedClassName.toString(), classLoader);

                    if (clazz != null) {
                        logger.debug("通过嵌套类名找到类: " + clazz.getName());
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    protected static Class<?> findClassInternal(String className, ClassLoader preferredLoader) {
        if (preferredLoader != null) {
            Class<?> clazz = findInLoader(className, preferredLoader);
            if (clazz != null) {
                logger.debug("在首选ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        ClassLoader apkLoader = ClassLoaderManager.getApkClassLoader();
        if (apkLoader != null && apkLoader != apkClassLoader) {
            apkClassLoader = apkLoader;
        }
        
        if (apkClassLoader != null) {
            Class<?> clazz = findInLoader(className, apkClassLoader);
            if (clazz != null) {
                logger.debug("在APK ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        ClassLoader moduleLoader = ClassResolver.class.getClassLoader();
        if (moduleLoader != null && moduleLoader != preferredLoader && moduleLoader != apkClassLoader) {
            Class<?> clazz = findInLoader(className, moduleLoader);
            if (clazz != null) {
                logger.debug("在模块ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        for (ClassLoader loader : registeredLoaders) {
            if (loader == preferredLoader || loader == apkClassLoader || loader == moduleLoader) {
                continue;
            }

            Class<?> clazz = findInLoader(className, loader);
            if (clazz != null) {
                logger.debug("在注册的ClassLoader中找到类: " + className);
                return clazz;
            }
        }

        Class<?> clazz = findInLoader(className, null);
        if (clazz != null) {
            logger.debug("在系统ClassLoader中找到类: " + className);
            return clazz;
        }

        logger.debug("未找到类: " + className);
        return null;
    }

    private static Class<?> findInLoader(String className, ClassLoader loader) {
        try {
            if (AppEnvironment.isHookEnv()) {
                return XposedBasicHook.ClassFinder.withCl(loader).find(className);
            } else {
                if (loader != null) {
                    return Class.forName(className, false, loader);
                } else {
                    return Class.forName(className);
                }
            }
        } catch (Throwable e) {
            logger.debug("在ClassLoader中查找类失败: " + className + ", " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    public static Method findMethod(String className, String methodName, Object... paramTypes) {
        return findMethod(className, methodName, null, paramTypes);
    }

    public static Method findMethod(String className, String methodName, ClassLoader preferredLoader, Object... paramTypes) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法查找方法: " + className + "." + methodName);
            return null;
        }

        if (AppEnvironment.isHookEnv()) {
            try {
                return XposedBasicHook.MethodFinder.withCl(preferredLoader)
                    .find(className, methodName, paramTypes);
            } catch (Exception e) {
                logger.debug("查找方法失败: " + className + "." + methodName + ", " + e.getMessage() + ", 尝试使用反射");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (ClassResolver.isApplicableArgs(m.getParameterTypes(), (Class<?>[]) paramTypes, m.isVarArgs()))
                return m;
        }
        logger.debug("反射查找也失败了，方法名：" + className + "." + methodName);
        return null;
    }

    public static List<Method> findAllMethods(String className, String methodName) {
        return findAllMethods(className, methodName, null);
    }

    public static List<Method> findAllMethods(String className, String methodName, ClassLoader preferredLoader) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法查找方法: " + className + "." + methodName);
            return new ArrayList<>();
        }

        if (AppEnvironment.isHookEnv()) {
            try {
                return XposedBasicHook.MethodFinder.withCl(preferredLoader)
                    .findAll(className, methodName);
            } catch (Exception e) {
                logger.debug("查找所有方法失败: " + className + "." + methodName + ", " + e.getMessage());
                return new ArrayList<>();
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> result = new ArrayList<>();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                result.add(m);
            }
        }
        return result;
    }

    public static Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        return findClassOrFail(className, null);
    }

    public static Class<?> findClassOrFail(String className, ClassLoader preferredLoader) throws ClassNotFoundException {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            throw new ClassNotFoundException("未找到类: " + className);
        }
        return clazz;
    }

    public static Class<?> findClassWithImportsOrFail(String className, List<String> imports)
                                throws ClassNotFoundException {
        return findClassWithImportsOrFail(className, null, imports);
    }

    public static Class<?> findClassWithImportsOrFail(String className, ClassLoader classLoader, List<String> imports) 
                                throws ClassNotFoundException {
        Class<?> clazz = findClassWithImports(className, classLoader, imports);
        if (clazz == null) {
            throw new ClassNotFoundException("未找到类: " + className);
        }
        return clazz;
    }

    public static Field findStaticField(String className, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        return findStaticField(className, fieldName, null, true, true);
    }

    public static Field findStaticField(String className, String fieldName, ClassLoader classLoader) {
        return findStaticField(className, fieldName, classLoader, true, true);
    }

    public static Field findStaticField(String className, String fieldName,
                                         ClassLoader preferredLoader,
                                        boolean accessSuper, boolean accessInterfaces) {
        Class<?> clazz = findClassInternal(className, preferredLoader);
        if (clazz == null) {
            logger.debug("未找到类，无法获取静态字段: " + className + "." + fieldName);
            return null;
        }

        try {
            return findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        } catch (Throwable e) {
            logger.debug("使用反射获取静态字段失败: " + className + "." + fieldName + ", " + e.getMessage());
            return null;
        }
    }

    public static Field findStaticField(Class<?> clazz, String fieldName,
                                        boolean accessSuper, boolean accessInterfaces) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                int modifiers = field.getModifiers();
                
                if (!Modifier.isStatic(modifiers)) {
                    current = current.getSuperclass();
                    continue;
                }
                
                return field;
            } catch (NoSuchFieldException e) {
                if (!accessSuper) break;
                current = current.getSuperclass();
            }
        }
        if (accessInterfaces) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return null;
    }

    public static Field findStaticFieldOrFail(Class<?> clazz, String fieldName,
                    boolean accessSuper, boolean accessInterfaces) throws NoSuchFieldException {
        Field field = findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        if (field == null) {
            throw new NoSuchFieldException("找不到字段: " + clazz.getName() + "." + fieldName);
        }
        return field;
    }

    public static Object getStaticFieldInternal(Class<?> clazz, String fieldName,
             boolean accessSuper, boolean accessInterfaces) throws IllegalAccessException {
        Field field = findStaticField(clazz, fieldName, accessSuper, accessInterfaces);
        field.setAccessible(true);
        return field.get(null);
    }
}
