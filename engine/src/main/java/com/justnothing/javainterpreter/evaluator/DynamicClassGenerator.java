package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.nodes.AnnotationNode;
import com.justnothing.javainterpreter.ast.nodes.ClassDeclarationNode;
import com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode;
import com.justnothing.javainterpreter.ast.nodes.FieldDeclarationNode;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 动态类生成器 - 核心类。
 * 
 * <h2>概述</h2>
 * <p>本类是脚本引擎的核心组件，负责在运行时动态生成Java类。它使用ASM库直接生成字节码，
 * 而不是通过Java源代码编译。这使得脚本语言可以像原生Java类一样使用。</p>
 * 
 * <h2>ASM字节码生成基础</h2>
 * <h3>核心概念</h3>
 * <ul>
 *   <li><b>ClassWriter</b>：用于生成类的字节码</li>
 *   <li><b>MethodVisitor</b>：用于生成方法的字节码</li>
 *   <li><b>FieldVisitor</b>：用于生成字段的字节码</li>
 *   <li><b>AnnotationVisitor</b>：用于生成注解的字节码</li>
 * </ul>
 * 
 * <h3>类生成流程</h3>
 * <pre>
 * 1. 创建ClassWriter
 * 2. 调用visit()开始类定义
 * 3. 添加字段、方法、注解
 * 4. 调用visitEnd()结束类定义
 * 5. 获取字节数组并定义类
 * </pre>
 * 
 * <h2>动态类执行机制</h2>
 * <p>动态生成的类使用"解释执行"模式：
 * <ol>
 *   <li>方法体被编译为调用 {@link MethodBodyExecutor#executeMethod} 的字节码</li>
 *   <li>方法AST节点被注册到全局方法表</li>
 *   <li>运行时，executeMethod查找并解释执行AST节点</li>
 * </ol>
 * </p>
 * 
 * <h2>继承支持</h2>
 * <p>支持以下继承场景：
 * <ul>
 *   <li>继承标准Java类</li>
 *   <li>继承其他动态生成的类</li>
 *   <li>实现接口（包括动态生成的接口）</li>
 * </ul>
 * </p>
 * 
 * <h2>类加载器层次</h2>
 * <pre>
 * Bootstrap ClassLoader (核心Java类)
 *        ↑
 * Platform ClassLoader (Java平台类)
 *        ↑
 * Application ClassLoader (应用类)
 *        ↑
 * DynamicClassLoader (动态生成的类)
 * </pre>
 * 
 * @author JustNothing
 * @see ConstructorGenerator
 * @see MethodGenerator
 * @see FieldGenerator
 * @see AnnotationGenerator
 * @see BytecodeUtils
 */
public class DynamicClassGenerator {
    
    /**
     * 默认的类定义器。
     * 用于将字节数组转换为Class对象。
     */
    private static volatile ClassDefiner defaultClassDefiner = new StandardClassDefiner();
    
    /**
     * 设置默认类定义器。
     * 
     * <p>类定义器负责将字节码转换为Class对象。默认使用标准类加载器机制，
     * 但在Android等特殊环境可能需要使用DexClassDefiner。</p>
     * 
     * @param definer 类定义器实例
     */
    public static void setDefaultClassDefiner(ClassDefiner definer) {
        if (definer != null) {
            defaultClassDefiner = definer;
        }
    }

    /**
     * 获取默认类定义器。
     * 
     * @return 当前的默认类定义器
     */
    public static ClassDefiner getDefaultClassDefiner() {
        return defaultClassDefiner;
    }

    /**
     * 执行上下文，包含变量、导入等信息。
     */
    private final ExecutionContext context;
    
    /**
     * 已生成的类缓存。
     * 键为类名，值为生成的Class对象。
     */
    private final Map<String, Class<?>> generatedClasses;
    
    /**
     * 类定义器，用于将字节码转换为Class对象。
     */
    private final ClassDefiner classDefiner;
    
    /**
     * 自定义类加载器，用于加载动态生成的类。
     */
    private final DynamicClassLoader classLoader;
    
    /**
     * 构造函数生成器。
     */
    private final ConstructorGenerator constructorGenerator;
    
    /**
     * 方法生成器。
     */
    private final MethodGenerator methodGenerator;

    /**
     * 创建动态类生成器。
     * 
     * @param context 执行上下文
     */
    public DynamicClassGenerator(ExecutionContext context) {
        this(context, defaultClassDefiner);
    }
    
    /**
     * 创建动态类生成器，指定类定义器。
     * 
     * @param context 执行上下文
     * @param classDefiner 类定义器
     */
    public DynamicClassGenerator(ExecutionContext context, ClassDefiner classDefiner) {
        this.context = context;
        this.generatedClasses = new HashMap<>();
        this.classDefiner = classDefiner != null ? classDefiner : defaultClassDefiner;
        this.classLoader = new DynamicClassLoader(context.getClassLoader(), this);
        this.constructorGenerator = new ConstructorGenerator(context, classLoader);
        this.methodGenerator = new MethodGenerator(context);
    }
    
    /**
     * 创建动态类生成器，指定父类加载器。
     * 
     * @param parentClassLoader 父类加载器
     * @param context 执行上下文
     */
    public DynamicClassGenerator(ClassLoader parentClassLoader, ExecutionContext context) {
        this(parentClassLoader, context, defaultClassDefiner);
    }

    /**
     * 创建动态类生成器的完整构造函数。
     * 
     * @param parentClassLoader 父类加载器
     * @param context 执行上下文
     * @param classDefiner 类定义器
     */
    public DynamicClassGenerator(ClassLoader parentClassLoader, ExecutionContext context, 
            ClassDefiner classDefiner) {
        this.context = context;
        this.generatedClasses = new HashMap<>();
        this.classDefiner = classDefiner != null ? classDefiner : defaultClassDefiner;
        this.classLoader = new DynamicClassLoader(parentClassLoader, this);
        this.constructorGenerator = new ConstructorGenerator(context, classLoader);
        this.methodGenerator = new MethodGenerator(context);
    }

    /**
     * 生成动态类。
     * 
     * <p>这是主要的入口方法，将AST节点转换为可执行的Java类。</p>
     * 
     * @param classDecl 类声明AST节点
     * @return 生成的Class对象
     * @throws EvaluationException 如果生成失败
     */
    public Class<?> generateClass(ClassDeclarationNode classDecl) throws EvaluationException {
        return generateClass(classDecl, null);
    }
    
    /**
     * 生成动态类，可指定构造函数参数类型。
     * 
     * <h3>生成流程</h3>
     * <ol>
     *   <li>检查缓存，避免重复生成</li>
     *   <li>预处理父类（如果是动态生成的类）</li>
     *   <li>创建ClassWriter并开始类定义</li>
     *   <li>添加字段、构造函数、方法</li>
     *   <li>生成字节码并定义类</li>
     *   <li>缓存生成的类</li>
     * </ol>
     * 
     * @param classDecl 类声明AST节点
     * @param constructorArgTypes 构造函数参数类型（可选）
     * @return 生成的Class对象
     * @throws EvaluationException 如果生成失败
     */
    public Class<?> generateClass(ClassDeclarationNode classDecl, Class<?>[] constructorArgTypes) 
            throws EvaluationException {
        
        String className = classDecl.getClassName();
        
        if (generatedClasses.containsKey(className)) {
            return generatedClasses.get(className);
        }
        
        preprocessParentClass(classDecl);
        
        try {
            ClassWriter cw = new FrameComputingClassWriter(classLoader);
            
            String superInternalName = resolveSuperClass(classDecl);
            String[] interfaces = resolveInterfaces(classDecl);
            
            int classAccess = calculateClassAccess(classDecl);
            cw.visit(V1_8, classAccess, BytecodeUtils.toInternalName(className), 
                null, superInternalName, interfaces);
            
            addClassAnnotations(cw, classDecl);
            
            addFields(cw, classDecl);
            
            if (!classDecl.isInterface()) {
                addConstructors(cw, classDecl, className, superInternalName, constructorArgTypes);
            }
            
            addMethods(cw, classDecl, className);
            
            cw.visitEnd();

            byte[] bytecode = cw.toByteArray();

            Class<?> generatedClass = classDefiner.defineClass(className, bytecode, classLoader.getParent());
            
            generatedClasses.put(className, generatedClass);
            
            return generatedClass;
            
        } catch (Exception e) {
            throw createGenerationException(className, e, classDecl);
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 预处理父类。
     * 
     * <p>如果父类也是动态生成的类，需要确保它已经被正确加载。</p>
     */
    private void preprocessParentClass(ClassDeclarationNode classDecl) throws EvaluationException {
        String superClassName = "java.lang.Object";
        if (classDecl.getSuperClass() != null) {
            superClassName = classDecl.getSuperClass().getOriginalTypeName();
        }
        
        if (superClassName != null && !superClassName.equals("java.lang.Object")) {
            if (!isClassAvailable(superClassName)) {
                if (context.hasCustomClass(superClassName)) {
                    Class<?> superClass = context.getCustomClass(superClassName);
                    
                    try {
                        Class.forName(superClass.getName(), true, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new EvaluationException(
                            "Failed to load parent class: " + superClassName,
                            classDecl.getLocation(),
                            ErrorCode.EVAL_CLASS_NOT_FOUND,
                            e
                        );
                    }
                    generatedClasses.put(superClassName, superClass);
                }
            }
        }
    }
    
    /**
     * 解析父类内部名。
     * 
     * <p>处理父类是动态生成类的情况。</p>
     */
    private String resolveSuperClass(ClassDeclarationNode classDecl) {
        String superInternalName = "java/lang/Object";
        
        if (classDecl.getSuperClass() != null) {
            String superClassOriginalName = classDecl.getSuperClass().getOriginalTypeName();
            
            if (context.hasCustomClass(superClassOriginalName)) {
                Class<?> superClass = context.getCustomClass(superClassOriginalName);
                if (superClass.isInterface()) {
                    // 如果是接口，保持默认父类
                } else {
                    superInternalName = BytecodeUtils.toInternalName(superClass.getName());
                }
            } else {
                Class<?> superClass = classDecl.getSuperClass().getResolvedClass();
                if (superClass != null && !superClass.isInterface()) {
                    superInternalName = classDecl.getSuperClass().getInternalName();
                }
            }
        }
        
        return superInternalName;
    }
    
    /**
     * 解析接口列表。
     */
    private String[] resolveInterfaces(ClassDeclarationNode classDecl) {
        List<String> interfaceList = new ArrayList<>();
        
        if (classDecl.getSuperClass() != null) {
            String superClassOriginalName = classDecl.getSuperClass().getOriginalTypeName();
            
            if (context.hasCustomClass(superClassOriginalName)) {
                Class<?> superClass = context.getCustomClass(superClassOriginalName);
                if (superClass.isInterface()) {
                    interfaceList.add(BytecodeUtils.toInternalName(superClass.getName()));
                }
            } else {
                Class<?> superClass = classDecl.getSuperClass().getResolvedClass();
                if (superClass != null && superClass.isInterface()) {
                    interfaceList.add(classDecl.getSuperClass().getInternalName());
                }
            }
        }
        
        for (ClassReferenceNode interfaceRef : classDecl.getInterfaces()) {
            String interfaceOriginalName = interfaceRef.getOriginalTypeName();
            if (context.hasCustomClass(interfaceOriginalName)) {
                Class<?> interfaceClass = context.getCustomClass(interfaceOriginalName);
                interfaceList.add(BytecodeUtils.toInternalName(interfaceClass.getName()));
            } else {
                interfaceList.add(interfaceRef.getInternalName());
            }
        }
        
        return interfaceList.isEmpty() ? null : interfaceList.toArray(new String[0]);
    }
    
    /**
     * 计算类访问标志。
     */
    private int calculateClassAccess(ClassDeclarationNode classDecl) {
        int classAccess = ACC_PUBLIC;
        if (classDecl.isInterface()) {
            classAccess = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
        }
        return classAccess;
    }
    
    /**
     * 添加类注解。
     */
    private void addClassAnnotations(ClassWriter cw, ClassDeclarationNode classDecl) {
        for (AnnotationNode annotation : classDecl.getAnnotations()) {
            AnnotationGenerator.addClassAnnotation(cw, annotation);
        }
    }
    
    /**
     * 添加字段。
     */
    private void addFields(ClassWriter cw, ClassDeclarationNode classDecl) {
        for (FieldDeclarationNode fieldDecl : classDecl.getFields()) {
            FieldGenerator.addField(cw, fieldDecl);
        }
    }
    
    /**
     * 添加构造函数。
     * @throws Exception  如果添加失败了
     */
    private void addConstructors(ClassWriter cw, ClassDeclarationNode classDecl, 
            String className, String superInternalName, Class<?>[] constructorArgTypes) throws Exception {
        
        if (classDecl.getConstructors().isEmpty()) {
            if (constructorArgTypes != null) {
                constructorGenerator.addConstructor(cw, className, superInternalName, 
                    constructorArgTypes, classDecl.getFields(), null);
            } else {
                constructorGenerator.addDefaultConstructor(cw, className, 
                    superInternalName, classDecl.getFields(), null);
            }
        }
        
        for (var constructorDecl : classDecl.getConstructors()) {
            constructorGenerator.addConstructor(cw, constructorDecl, 
                className, superInternalName, classDecl.getFields());
        }
    }
    
    /**
     * 添加方法。
     */
    private void addMethods(ClassWriter cw, ClassDeclarationNode classDecl, String className) {
        for (var methodDecl : classDecl.getMethods()) {
            methodGenerator.addMethod(cw, methodDecl, className, classDecl.isInterface());
        }
    }
    
    /**
     * 检查类是否可用。
     */
    private boolean isClassAvailable(String className) {
        Class<?> clazz = ClassResolver.findClassWithImports(className, classLoader, context.getImports());
        return clazz != null || generatedClasses.containsKey(className);
    }
    
    /**
     * 创建生成异常。
     */
    private EvaluationException createGenerationException(String className, Exception e, 
            ClassDeclarationNode classDecl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to generate class: ").append(className);
        sb.append(" - ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        
        Throwable current = e.getCause();
        int depth = 0;
        while (current != null && depth < 5) {
            sb.append("\n  Caused by: ").append(current.getClass().getSimpleName());
            sb.append(": ").append(current.getMessage());
            current = current.getCause();
            depth++;
        }
        
        return new EvaluationException(sb.toString(), classDecl.getLocation(), 
            ErrorCode.EVAL_INVALID_OPERATION, e);
    }

    /**
     * 获取已生成的类。
     * 
     * @param className 类名
     * @return Class对象，如果不存在返回null
     */
    public Class<?> getGeneratedClass(String className) {
        return generatedClasses.get(className);
    }

    /**
     * 检查是否已生成指定类。
     * 
     * @param className 类名
     * @return 如果已生成返回true
     */
    public boolean hasGeneratedClass(String className) {
        return generatedClasses.containsKey(className);
    }

    /**
     * 动态类加载器。
     * 
     * <p>自定义类加载器，用于加载动态生成的类。
     * 它会先检查已生成的类缓存，然后委托给父类加载器。</p>
     */
    private static class DynamicClassLoader extends ClassLoader {
        
        private final DynamicClassGenerator generator;

        DynamicClassLoader(ClassLoader parent, DynamicClassGenerator generator) {
            super(parent);
            this.generator = generator;
        }
        
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> cached = generator.generatedClasses.get(name);
            if (cached != null) {
                return cached;
            }
            
            if (generator.context != null && generator.context.hasCustomClass(name)) {
                return generator.context.getCustomClass(name);
            }
            
            return getParent().loadClass(name);
        }
    }

    /**
     * 帧计算类写入器。
     * 
     * <p>ASM需要知道类的继承层次来计算栈帧。这个类重写了getCommonSuperClass方法，
     * 支持动态生成的类。</p>
     * 
     * <h3>栈帧</h3>
     * <p>从Java 6开始，字节码必须包含栈帧信息，用于验证字节码的正确性。
     * 栈帧描述了每个执行位置的局部变量表和操作数栈的状态。</p>
     */
    private static class FrameComputingClassWriter extends ClassWriter {
        
        private final ClassLoader classLoader;

        FrameComputingClassWriter(ClassLoader classLoader) {
            super(COMPUTE_FRAMES);
            this.classLoader = classLoader;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            Class<?> c, d;
            try {
                c = Class.forName(BytecodeUtils.toClassName(type1), false, classLoader);
                d = Class.forName(BytecodeUtils.toClassName(type2), false, classLoader);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            
            if (c.isAssignableFrom(d)) return type1;
            if (d.isAssignableFrom(c)) return type2;
            if (c.isInterface() || d.isInterface()) return "java/lang/Object";
            
            do { 
                c = c.getSuperclass(); 
            } while (!c.isAssignableFrom(d));
            
            return BytecodeUtils.toInternalName(c.getName());
        }
    }
}
