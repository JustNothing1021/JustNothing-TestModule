package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * 构造函数生成器。
 * 
 * <h3>构造函数在字节码中的表示</h3>
 * <p>在Java字节码中，构造函数有特殊的表示方式：
 * <ul>
 *   <li><b>名称</b>：所有构造函数在字节码中都命名为 "&lt;init&gt;"</li>
 *   <li><b>返回类型</b>：构造函数的返回类型固定为 void (V)</li>
 *   <li><b>调用方式</b>：使用 INVOKESPECIAL 指令调用（而非 INVOKEVIRTUAL）</li>
 * </ul>
 * </p>
 * 
 * <h3>构造函数执行流程</h3>
 * <p>一个典型的构造函数字节码执行流程：
 * <ol>
 *   <li>加载 this 引用到栈 (ALOAD_0)</li>
 *   <li>加载父类构造函数参数</li>
 *   <li>调用父类构造函数 (INVOKESPECIAL)</li>
 *   <li>初始化实例字段</li>
 *   <li>执行构造函数体</li>
 *   <li>返回 (RETURN)</li>
 * </ol>
 * </p>
 * 
 * <h3>动态类构造函数的特殊处理</h3>
 * <p>对于动态生成的类，构造函数需要特殊处理：
 * <ul>
 *   <li>字面量初始值直接在字节码中设置</li>
 *   <li>非字面量初始值通过调用 MethodBodyExecutor 在运行时执行</li>
 *   <li>支持父类是动态生成类的情况</li>
 * </ul>
 * </p>
 * 
 * @author JustNothing
 * @see DynamicClassGenerator
 * @see MethodBodyExecutor
 */
public class ConstructorGenerator {
    
    private final ExecutionContext context;
    private final ClassLoader classLoader;
    
    public ConstructorGenerator(ExecutionContext context, ClassLoader classLoader) {
        this.context = context;
        this.classLoader = classLoader;
    }

    /**
     * 添加默认构造函数（无参构造函数）。
     * 
     * <p>当类没有显式定义构造函数时，Java编译器会自动生成一个默认的无参构造函数。
     * 默认构造函数会：
     * <ol>
     *   <li>调用父类的无参构造函数</li>
     *   <li>初始化实例字段（如果有字面量初始值）</li>
     * </ol>
     * </p>
     * 
     * @param cw 类写入器
     * @param className 类名
     * @param superInternalName 父类内部名
     * @param fields 字段声明列表
     */
    public void addDefaultConstructor(ClassWriter cw, String className, 
            String superInternalName, List<FieldDeclarationNode> fields, 
            ConstructorDeclarationNode constructorDecl) {
        try {
            Class<?> superClass = loadClass(superInternalName);
            checkSuperConstructorAccessible(superClass, null, constructorDecl);
            
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
            
            initializeFieldsWithLiterals(mv, className, fields);
            
            boolean hasNonLiteralInit = hasNonLiteralFieldInitializers(fields);
            if (hasNonLiteralInit) {
                generateMethodBodyCall(mv, className, "<init>", null, 0);
            }
            
            mv.visitInsn(RETURN);
            
            int maxStack = calculateMaxStack(0, fields, hasNonLiteralInit);
            mv.visitMaxs(maxStack, 1);
            mv.visitEnd();
            
            registerDefaultConstructorBody(className, fields);
            
        } catch (Exception e) {
            generateFallbackConstructor(cw, className, superInternalName, fields);
        }
    }
    
    /**
     * 添加带参数类型的构造函数。
     * 
     * <p>当需要创建与特定父类构造函数匹配的构造函数时使用。</p>
     * 
     * @param cw 类写入器
     * @param className 类名
     * @param superInternalName 父类内部名
     * @param paramTypes 构造函数参数类型
     * @param fields 字段声明列表
     * @throws Exception 如果生成失败
     */
    public void addConstructor(ClassWriter cw, String className, String superInternalName,
            Class<?>[] paramTypes, List<FieldDeclarationNode> fields, 
            ConstructorDeclarationNode constructorDecl) throws Exception {
        
        String descriptor = BytecodeUtils.getConstructorDescriptor(paramTypes);
        String superDescriptor = BytecodeUtils.getConstructorDescriptor(paramTypes);
        
        Class<?> superClass = loadClass(superInternalName);
        checkSuperConstructorAccessible(superClass, paramTypes, constructorDecl);
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        
        int paramIndex = 1;
        for (Class<?> paramType : paramTypes) {
            BytecodeUtils.loadVariable(mv, paramType, paramIndex);
            paramIndex += BytecodeUtils.getSlotSize(paramType);
        }
        
        mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDescriptor, false);
        
        initializeFieldsWithLiterals(mv, className, fields);
        
        mv.visitInsn(RETURN);
        
        int maxStack = calculateMaxStack(paramTypes.length, fields, false);
        int localVarCount = 1 + BytecodeUtils.calculateParamSlotCount(paramTypes);
        mv.visitMaxs(maxStack, localVarCount);
        mv.visitEnd();
    }
    
    /**
     * 添加自定义声明的构造函数。
     * 
     * <p>根据AST节点中的构造函数声明生成对应的字节码。</p>
     * 
     * @param cw 类写入器
     * @param constructorDecl 构造函数声明节点
     * @param className 类名
     * @param superInternalName 父类内部名
     * @param fields 字段声明列表
     */
    public void addConstructor(ClassWriter cw, ConstructorDeclarationNode constructorDecl,
            String className, String superInternalName, List<FieldDeclarationNode> fields) {
        
        StringBuilder descriptor = new StringBuilder("(");
        for (ParameterNode param : constructorDecl.getParameters()) {
            descriptor.append(param.getType().getDescriptor());
        }
        descriptor.append(")V");
        
        int modifiers = constructorDecl.getModifiers().toAccessFlags();
        if (modifiers == 0) {
            modifiers = ACC_PUBLIC;
        }
        
        String internalClassName = BytecodeUtils.toInternalName(className);
        MethodVisitor mv = cw.visitMethod(modifiers, "<init>", descriptor.toString(), null, null);
        
        mv.visitCode();
        
        generateSuperConstructorCall(mv, superInternalName, constructorDecl.getParameters());
        
        initializeFieldsFromParameters(mv, constructorDecl, fields, internalClassName, superInternalName);
        
        if (constructorDecl.getBody() != null) {
            generateConstructorBodyCall(mv, constructorDecl, className);
        }
        
        mv.visitInsn(RETURN);
        
        int maxStack = calculateMaxStackForConstructor(constructorDecl);
        int localVarCount = calculateLocalVarCount(constructorDecl);
        mv.visitMaxs(maxStack, localVarCount);
        mv.visitEnd();
        
        registerConstructorBody(className, constructorDecl, fields);
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 加载类（支持动态生成的类）。
     */
    private Class<?> loadClass(String internalName) throws ClassNotFoundException {
        String className = BytecodeUtils.toClassName(internalName);
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return classLoader.loadClass(className);
        }
    }
    
    /**
     * 检查父类构造函数是否可访问。
     */
    private void checkSuperConstructorAccessible(Class<?> superClass, Class<?>[] paramTypes, 
            ConstructorDeclarationNode constructorDecl) 
            throws EvaluationException {
        Constructor<?> target = null;
        try {
            if (paramTypes == null || paramTypes.length == 0) {
                target = superClass.getDeclaredConstructor();
            } else {
                target = superClass.getDeclaredConstructor(paramTypes);
            }
        } catch (NoSuchMethodException e) {
            for (Constructor<?> constructor : superClass.getDeclaredConstructors()) {
                if (constructor.getParameterTypes().length == (paramTypes != null ? paramTypes.length : 0)) {
                    target = constructor;
                    break;
                }
            }
        }
        
        if (target != null && Modifier.isPrivate(target.getModifiers())) {
            throw new EvaluationException(
                "Cannot extend " + superClass.getSimpleName() + 
                ": its constructor is private",
                constructorDecl == null ? new SourceLocation(-1, -1) : constructorDecl.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION,
                null);
        }
    }
    
    /**
     * 初始化字段的字面量初始值。
     */
    private void initializeFieldsWithLiterals(MethodVisitor mv, String className, 
            List<FieldDeclarationNode> fields) {
        if (fields == null) return;
        
        String internalClassName = BytecodeUtils.toInternalName(className);
        
        for (FieldDeclarationNode fieldDecl : fields) {
            ASTNode initialValue = fieldDecl.getInitialValue();
            if (initialValue instanceof LiteralNode literal) {
                mv.visitVarInsn(ALOAD, 0);
                BytecodeUtils.loadLiteralValue(mv, literal.getValue(), fieldDecl.getType().getDescriptor());
                mv.visitFieldInsn(PUTFIELD, internalClassName, 
                    fieldDecl.getFieldName(), fieldDecl.getType().getDescriptor());
            }
        }
    }
    
    /**
     * 检查是否有非字面量的字段初始化器。
     */
    private boolean hasNonLiteralFieldInitializers(List<FieldDeclarationNode> fields) {
        if (fields == null) return false;
        
        for (FieldDeclarationNode field : fields) {
            ASTNode initValue = field.getInitialValue();
            if (initValue != null && !(initValue instanceof LiteralNode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成调用父类构造函数的字节码。
     */
    private void generateSuperConstructorCall(MethodVisitor mv, String superInternalName, 
            List<ParameterNode> constructorParams) {
        
        Class<?> superClass = null;
        Constructor<?> matchedConstructor = null;
        
        try {
            superClass = loadClass(superInternalName);
            matchedConstructor = BytecodeUtils.findBestMatchingConstructor(superClass, constructorParams);
        } catch (Exception ignored) {
        }
        
        if (matchedConstructor != null) {
            Class<?>[] superParamTypes = matchedConstructor.getParameterTypes();
            StringBuilder superDesc = new StringBuilder("(");
            for (Class<?> pt : superParamTypes) {
                superDesc.append(BytecodeUtils.getDescriptor(pt));
            }
            superDesc.append(")V");
            
            mv.visitVarInsn(ALOAD, 0);
            
            int loadIdx = 1;
            int paramCount = constructorParams != null ? constructorParams.size() : 0;
            
            for (int i = 0; i < superParamTypes.length && i < paramCount; i++) {
                ParameterNode param = constructorParams.get(i);
                BytecodeUtils.loadVariable(mv, param.getType(), loadIdx);
                loadIdx += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
            }
            
            for (int i = paramCount; i < superParamTypes.length; i++) {
                BytecodeUtils.loadDefaultValue(mv, superParamTypes[i]);
            }
            
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDesc.toString(), false);
            
        } else if (superClass != null) {
            generateAnyConstructorCall(mv, superClass, superInternalName);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
        }
    }
    
    /**
     * 生成调用任意可用构造函数的字节码。
     */
    private void generateAnyConstructorCall(MethodVisitor mv, Class<?> superClass, String superInternalName) {
        Constructor<?>[] constructors = superClass.getDeclaredConstructors();
        if (constructors.length > 0) {
            Constructor<?> anyConstructor = constructors[0];
            Class<?>[] superParamTypes = anyConstructor.getParameterTypes();
            
            StringBuilder superDesc = new StringBuilder("(");
            for (Class<?> pt : superParamTypes) {
                superDesc.append(BytecodeUtils.getDescriptor(pt));
            }
            superDesc.append(")V");
            
            mv.visitVarInsn(ALOAD, 0);
            for (Class<?> pt : superParamTypes) {
                BytecodeUtils.loadDefaultValue(mv, pt);
            }
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDesc.toString(), false);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
        }
    }
    
    /**
     * 从构造函数参数初始化字段。
     */
    private void initializeFieldsFromParameters(MethodVisitor mv, ConstructorDeclarationNode constructorDecl,
            List<FieldDeclarationNode> fields, String internalClassName, String superInternalName) {
        
        int paramIndex = 1;
        
        for (int i = 0; i < constructorDecl.getParameters().size(); i++) {
            ParameterNode param = constructorDecl.getParameters().get(i);
            String paramName = param.getParameterName();
            
            mv.visitVarInsn(ALOAD, 0);
            BytecodeUtils.loadVariable(mv, param.getType(), paramIndex);
            
            boolean fieldFound = false;
            
            if (fields != null) {
                for (FieldDeclarationNode fieldDecl : fields) {
                    if (fieldDecl.getFieldName().equals(paramName)) {
                        mv.visitFieldInsn(PUTFIELD, internalClassName, paramName, 
                            param.getType().getDescriptor());
                        fieldFound = true;
                        break;
                    }
                }
            }
            
            if (!fieldFound) {
                trySetSuperClassField(mv, superInternalName, paramName, param.getType(), internalClassName);
            }
            
            paramIndex += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
        }
    }
    
    /**
     * 尝试设置父类字段。
     */
    private void trySetSuperClassField(MethodVisitor mv, String superInternalName, 
            String fieldName, com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode type,
            String internalClassName) {
        try {
            Class<?> superClass = loadClass(superInternalName);
            Field field = superClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            mv.visitFieldInsn(PUTFIELD, internalClassName, fieldName, type.getDescriptor());
        } catch (Exception ignored) {
        }
    }
    
    /**
     * 生成构造函数体调用。
     */
    private void generateConstructorBodyCall(MethodVisitor mv, ConstructorDeclarationNode constructorDecl, 
            String className) {
        mv.visitLdcInsn(className);
        mv.visitLdcInsn("<init>");
        mv.visitVarInsn(ALOAD, 0);
        
        int paramCount = constructorDecl.getParameters().size();
        if (paramCount > 0) {
            mv.visitIntInsn(BIPUSH, paramCount);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            
            int paramIndex = 1;
            for (int i = 0; i < paramCount; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                ParameterNode param = constructorDecl.getParameters().get(i);
                BytecodeUtils.loadAndBoxParameter(mv, param.getType(), paramIndex);
                mv.visitInsn(AASTORE);
                
                paramIndex += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
            }
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        
        mv.visitMethodInsn(INVOKESTATIC, 
            "com/justnothing/javainterpreter/evaluator/MethodBodyExecutor", 
            "executeMethod", 
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", 
            false);
        mv.visitInsn(POP);
    }
    
    /**
     * 生成调用方法体的字节码（用于非字面量初始化）。
     */
    private void generateMethodBodyCall(MethodVisitor mv, String className, String methodName,
            List<ParameterNode> params, int paramSlotOffset) {
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodName);
        mv.visitVarInsn(ALOAD, 0);
        
        int paramCount = params != null ? params.size() : 0;
        if (paramCount > 0) {
            mv.visitIntInsn(BIPUSH, paramCount);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            
            int paramIndex = paramSlotOffset + 1;
            for (int i = 0; i < paramCount; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                ParameterNode param = params.get(i);
                BytecodeUtils.loadAndBoxParameter(mv, param.getType(), paramIndex);
                mv.visitInsn(AASTORE);
                
                paramIndex += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
            }
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        
        mv.visitMethodInsn(INVOKESTATIC, 
            "com/justnothing/javainterpreter/evaluator/MethodBodyExecutor", 
            "executeMethod", 
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", 
            false);
        mv.visitInsn(POP);
    }
    
    /**
     * 生成后备构造函数（当正常生成失败时）。
     */
    private void generateFallbackConstructor(ClassWriter cw, String className, 
            String superInternalName, List<FieldDeclarationNode> fields) {
        try {
            Class<?> superClass = loadClass(superInternalName);
            Constructor<?>[] constructors = superClass.getDeclaredConstructors();
            
            if (constructors.length > 0) {
                Constructor<?> constructor = constructors[0];
                Class<?>[] paramTypes = constructor.getParameterTypes();
                
                String descriptor = BytecodeUtils.getConstructorDescriptor(paramTypes);
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
                mv.visitCode();
                
                mv.visitVarInsn(ALOAD, 0);
                for (Class<?> pt : paramTypes) {
                    BytecodeUtils.loadDefaultValue(mv, pt);
                }
                mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", descriptor, false);
                
                initializeFieldsWithLiterals(mv, className, fields);
                
                mv.visitInsn(RETURN);
                
                int maxStack = 1 + paramTypes.length + (fields != null && !fields.isEmpty() ? 2 : 0);
                int localVarCount = 1 + BytecodeUtils.calculateParamSlotCount(paramTypes);
                mv.visitMaxs(maxStack, localVarCount);
                mv.visitEnd();
            } else {
                generateSimpleConstructor(cw, className, superInternalName, fields);
            }
        } catch (Exception ex) {
            generateSimpleConstructor(cw, className, superInternalName, fields);
        }
    }
    
    /**
     * 生成简单构造函数（无参数）。
     */
    private void generateSimpleConstructor(ClassWriter cw, String className, 
            String superInternalName, List<FieldDeclarationNode> fields) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
        
        initializeFieldsWithLiterals(mv, className, fields);
        
        mv.visitInsn(RETURN);
        
        int maxStack = 1 + (fields != null && !fields.isEmpty() ? 2 : 0);
        mv.visitMaxs(maxStack, 1);
        mv.visitEnd();
    }
    
    /**
     * 计算最大栈深度。
     */
    private int calculateMaxStack(int paramCount, List<FieldDeclarationNode> fields, boolean hasNonLiteralInit) {
        int maxStack = 1 + paramCount;
        if (fields != null && !fields.isEmpty()) {
            maxStack += 2;
        }
        if (hasNonLiteralInit) {
            maxStack += 5;
        }
        return maxStack;
    }
    
    /**
     * 计算构造函数的最大栈深度。
     */
    private int calculateMaxStackForConstructor(ConstructorDeclarationNode constructorDecl) {
        int maxStack = 2;
        for (ParameterNode param : constructorDecl.getParameters()) {
            maxStack += BytecodeUtils.getStackSize(param.getType().getResolvedClass());
        }
        if (constructorDecl.getBody() != null) {
            maxStack += 5;
        }
        return maxStack;
    }
    
    /**
     * 计算局部变量表大小。
     */
    private int calculateLocalVarCount(ConstructorDeclarationNode constructorDecl) {
        int count = 1;
        for (ParameterNode param : constructorDecl.getParameters()) {
            count += BytecodeUtils.getSlotSize(param.getType().getResolvedClass());
        }
        return count;
    }
    
    /**
     * 注册默认构造函数体（用于运行时执行非字面量初始化）。
     */
    private void registerDefaultConstructorBody(String className, List<FieldDeclarationNode> fields) {
        if (fields == null || fields.isEmpty()) return;
        
        List<ASTNode> bodyStatements = new ArrayList<>();
        for (FieldDeclarationNode field : fields) {
            ASTNode initValue = field.getInitialValue();
            if (initValue != null && !(initValue instanceof LiteralNode)) {
                FieldAssignmentNode assignment = new FieldAssignmentNode(
                    new VariableNode("this", field.getLocation()),
                    field.getFieldName(),
                    initValue,
                    field.getLocation()
                );
                bodyStatements.add(assignment);
            }
        }
        
        if (bodyStatements.isEmpty()) return;
        
        BlockNode body = new BlockNode(bodyStatements, fields.get(0).getLocation());
        MethodDeclarationNode methodDecl = new MethodDeclarationNode(
            "<init>",
            ClassReferenceNode.of("void", void.class, true, fields.get(0).getLocation()),
            new ArrayList<>(),
            body,
            new ClassModifiers(),
            fields.get(0).getLocation()
        );
        
        MethodBodyExecutor.registerMethod(className, "<init>", methodDecl, context);
    }
    
    /**
     * 注册自定义构造函数体。
     */
    private void registerConstructorBody(String className, ConstructorDeclarationNode constructorDecl, 
            List<FieldDeclarationNode> fields) {
        List<ASTNode> allStatements = new ArrayList<>();
        
        if (fields != null) {
            for (FieldDeclarationNode field : fields) {
                ASTNode initValue = field.getInitialValue();
                if (initValue != null && !(initValue instanceof LiteralNode)) {
                    FieldAssignmentNode assignment = new FieldAssignmentNode(
                        new VariableNode("this", field.getLocation()),
                        field.getFieldName(),
                        initValue,
                        field.getLocation()
                    );
                    allStatements.add(assignment);
                }
            }
        }
        
        if (constructorDecl.getBody() != null) {
            ASTNode bodyNode = constructorDecl.getBody();
            if (bodyNode instanceof BlockNode) {
                allStatements.addAll(((BlockNode) bodyNode).getStatements());
            } else {
                allStatements.add(bodyNode);
            }
        }
        
        BlockNode combinedBody = new BlockNode(allStatements, constructorDecl.getLocation());
        
        MethodDeclarationNode methodDecl = new MethodDeclarationNode(
            "<init>", 
            ClassReferenceNode.of("void", void.class, true, constructorDecl.getLocation()),
            constructorDecl.getParameters(),
            combinedBody,
            constructorDecl.getModifiers(),
            constructorDecl.getLocation()
        );
        
        MethodBodyExecutor.registerMethod(className, "<init>", methodDecl, context);
    }
}
