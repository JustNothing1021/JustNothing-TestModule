package com.justnothing.testmodule.command.functions.script.engine_new.evaluator;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ClassDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ConstructorDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.FieldDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MethodDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ParameterNode;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.EvaluationException;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 动态类生成器
 * <p>
 * 使用ASM从ClassDeclarationNode动态生成Java类
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class DynamicClassGenerator {
    
    private final ExecutionContext context;
    private final Map<String, Class<?>> generatedClasses;
    private final DynamicClassLoader classLoader;
    
    public DynamicClassGenerator(ExecutionContext context) {
        this.context = context;
        this.generatedClasses = new HashMap<>();
        this.classLoader = new DynamicClassLoader(context.getClassLoader());
    }
    
    /**
     * 生成类
     */
    public Class<?> generateClass(ClassDeclarationNode classDecl) throws EvaluationException {
        String className = classDecl.getClassName();
        
        if (generatedClasses.containsKey(className)) {
            return generatedClasses.get(className);
        }
        
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            
            String superClassName = classDecl.getSuperClassName();
            String superInternalName = superClassName != null ? 
                getInternalName(superClassName) : "java/lang/Object";
            
            String[] interfaces = null;
            if (!classDecl.getInterfaceNames().isEmpty()) {
                interfaces = new String[classDecl.getInterfaceNames().size()];
                for (int i = 0; i < classDecl.getInterfaceNames().size(); i++) {
                    interfaces[i] = getInternalName(classDecl.getInterfaceNames().get(i));
                }
            }
            
            cw.visit(V1_8, ACC_PUBLIC, className.replace('.', '/'), null, superInternalName, interfaces);
            
            for (FieldDeclarationNode fieldDecl : classDecl.getFields()) {
                addField(cw, fieldDecl);
            }
            
            addDefaultConstructor(cw, className, superInternalName);
            
            for (ConstructorDeclarationNode constructorDecl : classDecl.getConstructors()) {
                addConstructor(cw, constructorDecl, className);
            }
            
            for (MethodDeclarationNode methodDecl : classDecl.getMethods()) {
                addMethod(cw, methodDecl, className);
            }
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> generatedClass = classLoader.defineClass(className, bytecode);
            
            generatedClasses.put(className, generatedClass);
            
            return generatedClass;
            
        } catch (Exception e) {
            throw new EvaluationException(
                "Failed to generate class: " + className + " - " + e.getMessage(),
                classDecl.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION
            );
        }
    }
    
    /**
     * 添加字段
     */
    private void addField(ClassWriter cw, FieldDeclarationNode fieldDecl) {
        String fieldName = fieldDecl.getFieldName();
        String descriptor = getTypeDescriptor(fieldDecl.getTypeName());
        int modifiers = fieldDecl.getModifiers().toAccessFlags();
        if (modifiers == 0) {
            modifiers = ACC_PUBLIC;
        }
        
        FieldVisitor fv = cw.visitField(modifiers, fieldName, descriptor, null, null);
        fv.visitEnd();
    }
    
    /**
     * 添加默认构造函数
     */
    private void addDefaultConstructor(ClassWriter cw, String className, String superInternalName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    /**
     * 添加构造函数
     */
    private void addConstructor(ClassWriter cw, ConstructorDeclarationNode constructorDecl, String className) {
        StringBuilder descriptor = new StringBuilder("(");
        for (ParameterNode param : constructorDecl.getParameters()) {
            descriptor.append(getTypeDescriptor(param.getTypeName()));
        }
        descriptor.append(")V");
        
        int modifiers = constructorDecl.getModifiers().toAccessFlags();
        if (modifiers == 0) {
            modifiers = ACC_PUBLIC;
        }
        String internalClassName = className.replace('.', '/');
        
        MethodVisitor mv = cw.visitMethod(modifiers, "<init>", descriptor.toString(), null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        
        for (int i = 0; i < constructorDecl.getParameters().size(); i++) {
            ParameterNode param = constructorDecl.getParameters().get(i);
            mv.visitVarInsn(ALOAD, 0);
            
            int loadOpcode = getLoadOpcode(param.getTypeName());
            mv.visitVarInsn(loadOpcode, i + 1);
            
            mv.visitFieldInsn(PUTFIELD, internalClassName, param.getParameterName(), 
                getTypeDescriptor(param.getTypeName()));
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, constructorDecl.getParameters().size() + 1);
        mv.visitEnd();
    }
    
    /**
     * 添加方法
     */
    private void addMethod(ClassWriter cw, MethodDeclarationNode methodDecl, String className) {
        String methodName = methodDecl.getMethodName();
        String returnDescriptor = getTypeDescriptor(methodDecl.getReturnTypeName());
        
        StringBuilder descriptor = new StringBuilder("(");
        for (ParameterNode param : methodDecl.getParameters()) {
            descriptor.append(getTypeDescriptor(param.getTypeName()));
        }
        descriptor.append(")").append(returnDescriptor);
        
        int modifiers = methodDecl.getModifiers().toAccessFlags();
        
        MethodVisitor mv = cw.visitMethod(modifiers, methodName, descriptor.toString(), null, null);
        mv.visitCode();
        
        addDefaultReturn(mv, methodDecl.getReturnTypeName());
        
        mv.visitMaxs(1, methodDecl.getParameters().size() + 1);
        mv.visitEnd();
    }
    
    /**
     * 添加默认返回
     */
    private void addDefaultReturn(MethodVisitor mv, String typeName) {
        if (typeName == null || typeName.equals("void")) {
            mv.visitInsn(RETURN);
        } else if (typeName.equals("int") || typeName.equals("byte") || 
                   typeName.equals("short") || typeName.equals("char") || 
                   typeName.equals("boolean")) {
            mv.visitInsn(ICONST_0);
            mv.visitInsn(IRETURN);
        } else if (typeName.equals("long")) {
            mv.visitInsn(LCONST_0);
            mv.visitInsn(LRETURN);
        } else if (typeName.equals("float")) {
            mv.visitInsn(FCONST_0);
            mv.visitInsn(FRETURN);
        } else if (typeName.equals("double")) {
            mv.visitInsn(DCONST_0);
            mv.visitInsn(DRETURN);
        } else {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
    }
    
    /**
     * 获取类型描述符
     */
    private String getTypeDescriptor(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "V";
        }
        
        String baseType = typeName;
        int arrayDepth = 0;
        
        while (baseType.endsWith("[]")) {
            baseType = baseType.substring(0, baseType.length() - 2);
            arrayDepth++;
        }
        
        int genericIndex = baseType.indexOf('<');
        if (genericIndex > 0) {
            baseType = baseType.substring(0, genericIndex);
        }
        
        String descriptor = switch (baseType) {
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "boolean" -> "Z";
            case "char" -> "C";
            case "byte" -> "B";
            case "short" -> "S";
            case "void" -> "V";
            default -> "L" + baseType.replace('.', '/') + ";";
        };

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < arrayDepth; i++) {
            result.append("[");
        }
        result.append(descriptor);
        
        return result.toString();
    }
    
    /**
     * 获取内部名称
     */
    private String getInternalName(String className) {
        String baseName = className;
        while (baseName.endsWith("[]")) {
            baseName = baseName.substring(0, baseName.length() - 2);
        }
        int genericIndex = baseName.indexOf('<');
        if (genericIndex > 0) {
            baseName = baseName.substring(0, genericIndex);
        }
        return baseName.replace('.', '/');
    }
    
    /**
     * 获取加载操作码
     */
    private int getLoadOpcode(String typeName) {
        if (typeName == null) return ALOAD;
        
        String baseType = typeName;
        while (baseType.endsWith("[]")) {
            baseType = baseType.substring(0, baseType.length() - 2);
        }
        int genericIndex = baseType.indexOf('<');
        if (genericIndex > 0) {
            baseType = baseType.substring(0, genericIndex);
        }

        return switch (baseType) {
            case "int", "byte", "short", "char", "boolean" -> ILOAD;
            case "long" -> LLOAD;
            case "float" -> FLOAD;
            case "double" -> DLOAD;
            default -> ALOAD;
        };
    }
    
    /**
     * 获取已生成的类
     */
    public Class<?> getGeneratedClass(String className) {
        return generatedClasses.get(className);
    }
    
    /**
     * 检查类是否已生成
     */
    public boolean hasGeneratedClass(String className) {
        return generatedClasses.containsKey(className);
    }
    
    /**
     * 动态类加载器
     */
    private static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }
        
        public Class<?> defineClass(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
