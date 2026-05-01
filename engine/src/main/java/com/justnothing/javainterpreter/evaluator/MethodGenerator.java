package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.nodes.AnnotationNode;
import com.justnothing.javainterpreter.ast.nodes.MethodDeclarationNode;
import com.justnothing.javainterpreter.ast.nodes.ParameterNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 方法生成器。
 * 
 * <h3>方法在字节码中的表示</h3>
 * <p>在Java字节码中，方法由以下要素组成：
 * <ul>
 *   <li><b>访问标志</b>：public/private/protected, static, final, abstract等</li>
 *   <li><b>名称</b>：方法名（构造函数为"&lt;init&gt;"，静态初始化块为"&lt;clinit&gt;"）</li>
 *   <li><b>描述符</b>：方法签名，格式为(参数类型)返回类型</li>
 *   <li><b>属性</b>：Code属性（方法体）、Exceptions属性（异常）等</li>
 * </ul>
 * </p>
 * 
 * <h3>方法调用指令</h3>
 * <p>JVM提供了四种方法调用指令：
 * <ul>
 *   <li><b>INVOKEVIRTUAL</b>：调用实例方法（虚方法），支持多态</li>
 *   <li><b>INVOKESPECIAL</b>：调用构造函数、私有方法、父类方法</li>
 *   <li><b>INVOKESTATIC</b>：调用静态方法</li>
 *   <li><b>INVOKEINTERFACE</b>：调用接口方法</li>
 * </ul>
 * </p>
 * 
 * <h3>动态方法执行机制</h3>
 * <p>对于动态生成的类，方法体不是直接编译为字节码，而是通过以下机制执行：
 * <ol>
 *   <li>生成的方法字节码调用 MethodBodyExecutor.executeMethod()</li>
 *   <li>executeMethod() 从注册表中查找对应的方法AST节点</li>
 *   <li>使用解释器执行AST节点</li>
 *   <li>返回执行结果</li>
 * </ol>
 * </p>
 * 
 * <h3>方法描述符示例</h3>
 * <pre>
 * Java方法                              字节码描述符
 * ----------------------------------   --------------------
 * void foo()                           ()V
 * int bar(String s)                    (Ljava/lang/String;)I
 * String baz(int a, long b)            (IJ)Ljava/lang/String;
 * void varargs(String... args)         ([Ljava/lang/String;)V
 * </pre>
 * 
 * @author JustNothing
 * @see DynamicClassGenerator
 * @see MethodBodyExecutor
 */
public class MethodGenerator {
    
    private final ExecutionContext context;
    
    public MethodGenerator(ExecutionContext context) {
        this.context = context;
    }

    /**
     * 添加方法到类中。
     * 
     * <p>方法生成流程：
     * <ol>
     *   <li>构建方法描述符</li>
     *   <li>创建MethodVisitor</li>
     *   <li>添加注解（如果有）</li>
     *   <li>生成方法体（调用MethodBodyExecutor）</li>
     *   <li>注册方法到执行器</li>
     * </ol>
     * </p>
     * 
     * @param cw 类写入器
     * @param methodDecl 方法声明节点
     * @param className 类名
     * @param isInterface 是否为接口
     */
    public void addMethod(ClassWriter cw, MethodDeclarationNode methodDecl,
            String className, boolean isInterface) {
        addMethod(cw, methodDecl, className, isInterface, null);
    }

    public void addMethod(ClassWriter cw, MethodDeclarationNode methodDecl,
            String className, boolean isInterface, Class<?> parentClass) {

        String methodName = methodDecl.getMethodName();
        String returnDescriptor = methodDecl.getReturnType().getDescriptor();

        String descriptor = buildMethodDescriptor(methodDecl, returnDescriptor);

        int modifiers = calculateModifiers(methodDecl, isInterface, parentClass, methodName, descriptor);
        
        if (methodDecl.getBody() == null) {
            if (isInterface) {
                generateAbstractMethod(cw, methodName, descriptor);
            }
            return;
        }
        
        MethodVisitor mv = cw.visitMethod(modifiers, methodName, descriptor, null, null);
        
        addAnnotations(mv, methodDecl);
        
        mv.visitCode();
        
        generateMethodBodyCall(mv, methodDecl, className);
        
        int maxLocals = calculateMaxLocals(methodDecl);
        mv.visitMaxs(10, maxLocals);
        mv.visitEnd();
        
        MethodBodyExecutor.registerMethod(className, methodName, methodDecl, context);
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建方法描述符。
     * 
     * <p>描述符格式：(参数类型描述符)返回类型描述符</p>
     */
    private String buildMethodDescriptor(MethodDeclarationNode methodDecl, String returnDescriptor) {
        StringBuilder descriptor = new StringBuilder("(");
        for (ParameterNode param : methodDecl.getParameters()) {
            descriptor.append(param.getType().getDescriptor());
        }
        descriptor.append(")").append(returnDescriptor);
        return descriptor.toString();
    }
    
    /**
     * 计算方法访问标志。
     */
    private int calculateModifiers(MethodDeclarationNode methodDecl, boolean isInterface,
                                     Class<?> parentClass, String methodName, String descriptor) {
        int modifiers = methodDecl.getModifiers().toAccessFlags();

        if (isInterface) {
            modifiers = modifiers | ACC_PUBLIC;
        }

        if (parentClass != null && !Modifier.isPrivate(modifiers)) {
            try {
                String[] interfaceNames = splitDescriptorParams(descriptor);
                Class<?>[] paramTypes = resolveParamTypes(interfaceNames);
                Method superMethod = findPublicMethod(parentClass, methodName, paramTypes);
                if (superMethod != null && Modifier.isPublic(superMethod.getModifiers())) {
                    modifiers = (modifiers & ~(ACC_PRIVATE | ACC_PROTECTED)) | ACC_PUBLIC;
                }
            } catch (Exception ignored) {
            }
        }

        return modifiers;
    }

    private static java.lang.reflect.Method findPublicMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        if (clazz == null || clazz == Object.class) return null;
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            java.lang.reflect.Method m = findPublicMethod(clazz.getSuperclass(), name, paramTypes);
            if (m != null) return m;
            for (Class<?> iface : clazz.getInterfaces()) {
                m = findPublicMethod(iface, name, paramTypes);
                if (m != null) return m;
            }
            return null;
        }
    }

    private static Class<?>[] resolveParamTypes(String[] descriptors) {
        if (descriptors == null) return new Class<?>[0];
        Class<?>[] types = new Class<?>[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            types[i] = descriptorToClass(descriptors[i]);
        }
        return types;
    }

    private static Class<?> descriptorToClass(String desc) {
        return switch (desc) {
            case "Z" -> boolean.class;
            case "B" -> byte.class;
            case "C" -> char.class;
            case "S" -> short.class;
            case "I" -> int.class;
            case "J" -> long.class;
            case "F" -> float.class;
            case "D" -> double.class;
            case "V" -> void.class;
            default -> Object.class;
        };
    }

    private static String[] splitDescriptorParams(String methodDescriptor) {
        int end = methodDescriptor.lastIndexOf(')');
        if (end <= 1) return new String[0];
        String params = methodDescriptor.substring(1, end);
        if (params.isEmpty()) return new String[0];
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c == 'L') {
                int semi = params.indexOf(';', i);
                current.append(params, i, semi + 1);
                result.add(current.toString());
                current.setLength(0);
                i = semi;
            } else if (c == '[') {
                current.append(c);
            } else {
                current.append(c);
                result.add(current.toString());
                current.setLength(0);
            }
        }
        return result.toArray(new String[0]);
    }
    
    /**
     * 生成抽象方法（用于接口）。
     */
    private void generateAbstractMethod(ClassWriter cw, String methodName, String descriptor) {
        int abstractModifiers = ACC_PUBLIC | ACC_ABSTRACT;
        MethodVisitor mv = cw.visitMethod(abstractModifiers, methodName, descriptor, null, null);
        mv.visitEnd();
    }
    
    /**
     * 添加方法注解。
     */
    private void addAnnotations(MethodVisitor mv, MethodDeclarationNode methodDecl) {
        for (AnnotationNode annotation : methodDecl.getAnnotations()) {
            AnnotationGenerator.addMethodAnnotation(mv, annotation);
        }
    }
    
    /**
     * 生成方法体调用字节码。
     * 
     * <p>生成的字节码会调用 MethodBodyExecutor.executeMethod()，
     * 传入类名、方法名、this对象和参数数组。</p>
     * 
     * <p>字节码执行流程：
     * <ol>
     *   <li>LDC - 加载类名字符串</li>
     *   <li>LDC - 加载方法名字符串</li>
     *   <li>ALOAD_0 或 ACONST_NULL - 加载this引用（静态方法为null）</li>
     *   <li>创建Object数组并填充参数</li>
     *   <li>INVOKESTATIC - 调用executeMethod</li>
     *   <li>拆箱并返回结果</li>
     * </ol>
     * </p>
     */
    private void generateMethodBodyCall(MethodVisitor mv, MethodDeclarationNode methodDecl, String className) {
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodDecl.getMethodName());
        
        boolean isStatic = methodDecl.getModifiers().isStatic();
        
        if (isStatic) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitVarInsn(ALOAD, 0);
        }
        
        int paramCount = methodDecl.getParameters().size();
        if (paramCount > 0) {
            mv.visitIntInsn(BIPUSH, paramCount);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            
            int slotOffset = isStatic ? 0 : 1;
            for (int i = 0; i < paramCount; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                ParameterNode param = methodDecl.getParameters().get(i);
                BytecodeUtils.loadAndBoxParameter(mv, param.getType(), i + slotOffset);
                mv.visitInsn(AASTORE);
            }
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        
        mv.visitMethodInsn(INVOKESTATIC, 
            "com/justnothing/javainterpreter/evaluator/MethodBodyExecutor", 
            "executeMethod", 
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", 
            false);
        
        BytecodeUtils.unboxAndReturn(mv, methodDecl.getReturnType());
    }
    
    /**
     * 计算局部变量表大小。
     * 
     * <p>局部变量表包含：
     * <ul>
     *   <li>this引用（非静态方法，占用槽位0）</li>
     *   <li>方法参数（从槽位1或0开始）</li>
     * </ul>
     * </p>
     */
    private int calculateMaxLocals(MethodDeclarationNode methodDecl) {
        int count = methodDecl.getModifiers().isStatic() ? 0 : 1;
        for (ParameterNode param : methodDecl.getParameters()) {
            count += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
        }
        return count;
    }
}
