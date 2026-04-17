package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import com.justnothing.javainterpreter.ast.SourceLocation;


public class DynamicClassGenerator {
    
    private static volatile ClassDefiner defaultClassDefiner = new StandardClassDefiner();
    
    public static void setDefaultClassDefiner(ClassDefiner definer) {
        if (definer != null) {
            defaultClassDefiner = definer;
        }
    }

    public static ClassDefiner getDefaultClassDefiner() {
        return defaultClassDefiner;
    }

    private final ExecutionContext context;
    private final Map<String, Class<?>> generatedClasses;
    private final ClassDefiner classDefiner;
    private final DynamicClassLoader classLoader;
    
    public DynamicClassGenerator(ExecutionContext context) {
        this(context, defaultClassDefiner);
    }
    
    public DynamicClassGenerator(ExecutionContext context, ClassDefiner classDefiner) {
        this.context = context;
        this.generatedClasses = new HashMap<>();
        this.classDefiner = classDefiner != null ? classDefiner : defaultClassDefiner;
        this.classLoader = new DynamicClassLoader(context.getClassLoader(), this);
    }
    
    public DynamicClassGenerator(ClassLoader parentClassLoader, ExecutionContext context) {
        this(parentClassLoader, context, defaultClassDefiner);
    }

    public DynamicClassGenerator(ClassLoader parentClassLoader, ExecutionContext context, ClassDefiner classDefiner) {
        this.context = context;
        this.generatedClasses = new HashMap<>();
        this.classDefiner = classDefiner != null ? classDefiner : defaultClassDefiner;
        this.classLoader = new DynamicClassLoader(parentClassLoader, this);
    }
    

    public Class<?> generateClass(ClassDeclarationNode classDecl) throws EvaluationException {
        return generateClass(classDecl, null);
    }
    

    public Class<?> generateClass(ClassDeclarationNode classDecl, Class<?>[] constructorArgTypes) throws EvaluationException {
        String className = classDecl.getClassName();
        
        if (generatedClasses.containsKey(className)) {
            return generatedClasses.get(className);
        }
        
        String superClassName = null;
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
        
        try {
            ClassWriter cw = new FrameComputingClassWriter(classLoader);
            
            String superInternalName = "java/lang/Object"; 
            List<String> interfaceList = new ArrayList<>();
            
            if (classDecl.getSuperClass() != null) {
                
                String superClassOriginalName = classDecl.getSuperClass().getOriginalTypeName();
                
                
                if (context.hasCustomClass(superClassOriginalName)) {
                    
                    Class<?> superClass = context.getCustomClass(superClassOriginalName);
                    if (superClass.isInterface()) {
                        
                        interfaceList.add(superClass.getName().replace('.', '/'));
                    } else {
                        
                        superInternalName = superClass.getName().replace('.', '/');
                    }
                } else {
                    
                    Class<?> superClass = classDecl.getSuperClass().getResolvedClass();
                    if (superClass.isInterface()) {
                        
                        interfaceList.add(classDecl.getSuperClass().getInternalName());
                    } else {
                        
                        superInternalName = classDecl.getSuperClass().getInternalName();
                    }
                }
            }
            
            
            for (ClassReferenceNode interfaceRef : classDecl.getInterfaces()) {
                String interfaceOriginalName = interfaceRef.getOriginalTypeName();
                if (context.hasCustomClass(interfaceOriginalName)) {
                    Class<?> interfaceClass = context.getCustomClass(interfaceOriginalName);
                    interfaceList.add(interfaceClass.getName().replace('.', '/'));
                } else {
                    interfaceList.add(interfaceRef.getInternalName());
                }
            }
            
            String[] interfaces = interfaceList.isEmpty() ? null : interfaceList.toArray(new String[0]);
            
            int classAccess = ACC_PUBLIC;
            if (classDecl.isInterface()) {
                classAccess = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
            }
            cw.visit(V1_8, classAccess, className.replace('.', '/'), null, superInternalName, interfaces);
            
            for (AnnotationNode annotation : classDecl.getAnnotations()) {
                addClassAnnotation(cw, annotation);
            }
            
            
            for (FieldDeclarationNode fieldDecl : classDecl.getFields()) {
                addField(cw, fieldDecl);
            }
            
            
            if (classDecl.getSuperClass() != null) {
                Class<?> superClass;
                
                
                if (context.hasCustomClass(superClassName)) {
                    superClass = context.getCustomClass(superClassName);
                } else {
                    if (superClassName == null) superClassName = "java.lang.Object";
                    superClass = ClassResolver.findClassWithImports(
                        superClassName, classLoader, context.getImports());
                }
                
                if (superClass != null) {
                    try {
                        
                        java.lang.reflect.Field[] fields = superClass.getDeclaredFields();
                        for (java.lang.reflect.Field field : fields) {
                            
                            int modifiers = field.getModifiers();
                            String fieldName = field.getName();

                            if (fieldName.startsWith("shadow$")) continue;

                            String descriptor = getFieldDescriptor(field.getType());
                            
                            FieldVisitor fv = cw.visitField(modifiers, fieldName, descriptor, null, null);
                            fv.visitEnd();
                        }
                    } catch (Exception ignored) {
                        
                    }
                }
            }
            
            if (!classDecl.isInterface()) {
                if (classDecl.getConstructors().isEmpty()) {
                    if (constructorArgTypes != null) {
                        addConstructor(cw, className, superInternalName, constructorArgTypes, classDecl.getFields());
                    } else {
                        addDefaultConstructor(cw, className, superInternalName, classDecl.getFields());
                    }
                }
                
                for (ConstructorDeclarationNode constructorDecl : classDecl.getConstructors()) {
                    addConstructor(cw, constructorDecl, className, superInternalName, classDecl.getFields());
                }
            }
            
            for (MethodDeclarationNode methodDecl : classDecl.getMethods()) {
                addMethod(cw, methodDecl, className, classDecl.isInterface());
            }
            
            cw.visitEnd();

            byte[] bytecode = cw.toByteArray();

            Class<?> generatedClass = classDefiner.defineClass(className, bytecode, classLoader.getParent());
            
            generatedClasses.put(className, generatedClass);
            
            return generatedClass;
            
        } catch (Exception e) {
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
            throw new EvaluationException(
                sb.toString(),
                classDecl.getLocation(),
                ErrorCode.EVAL_INVALID_OPERATION,
                e
            );
        }
    }

    private void addConstructor(ClassWriter cw, String className, String superInternalName, Class<?>[] paramTypes, List<FieldDeclarationNode> fields) throws Exception {
        
        StringBuilder descriptor = new StringBuilder("(");
        for (Class<?> paramType : paramTypes) {
            descriptor.append(org.objectweb.asm.Type.getDescriptor(paramType));
        }
        descriptor.append(")V");
        
        
        StringBuilder superDescriptor = new StringBuilder("(");
        for (Class<?> paramType : paramTypes) {
            superDescriptor.append(org.objectweb.asm.Type.getDescriptor(paramType));
        }
        superDescriptor.append(")V");
        
        
        checkSuperConstructorAccessible(superInternalName, paramTypes);
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor.toString(), null, null);
        mv.visitCode();
        
        
        mv.visitVarInsn(ALOAD, 0);
        
        
        int paramIndex = 1;
        for (Class<?> paramType : paramTypes) {
            if (paramType == int.class) {
                mv.visitVarInsn(ILOAD, paramIndex);
            } else if (paramType == long.class) {
                mv.visitVarInsn(LLOAD, paramIndex);
                paramIndex += 1;
            } else if (paramType == float.class) {
                mv.visitVarInsn(FLOAD, paramIndex);
            } else if (paramType == double.class) {
                mv.visitVarInsn(DLOAD, paramIndex);
                paramIndex += 1;
            } else if (paramType == boolean.class) {
                mv.visitVarInsn(ILOAD, paramIndex);
            } else if (paramType == byte.class) {
                mv.visitVarInsn(ILOAD, paramIndex);
            } else if (paramType == char.class) {
                mv.visitVarInsn(ILOAD, paramIndex);
            } else if (paramType == short.class) {
                mv.visitVarInsn(ILOAD, paramIndex);
            } else {
                mv.visitVarInsn(ALOAD, paramIndex);
            }
            paramIndex += 1;
        }
        
        
        mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDescriptor.toString(), false);
        
        
        if (fields != null) {
            for (FieldDeclarationNode fieldDecl : fields) {
                ASTNode initialValue = fieldDecl.getInitialValue();
                if (initialValue != null) {
                    
                    mv.visitVarInsn(ALOAD, 0);
                    
                    
                    if (initialValue instanceof LiteralNode literal) {
                        Object value = literal.getValue();
                        String descriptorField = fieldDecl.getType().getDescriptor();
                        
                        
                        if (descriptorField.equals("D") && value instanceof Integer) {
                            
                            mv.visitLdcInsn(((Integer) value).doubleValue());
                        } else if (descriptorField.equals("F") && value instanceof Integer) {
                            
                            mv.visitLdcInsn(((Integer) value).floatValue());
                        } else if (descriptorField.equals("J") && value instanceof Integer) {
                            
                            mv.visitLdcInsn(((Integer) value).longValue());
                        } else if (descriptorField.equals("S") && value instanceof Integer) {
                            
                            mv.visitLdcInsn(((Integer) value).shortValue());
                        } else if (descriptorField.equals("B") && value instanceof Integer) {
                            
                            mv.visitLdcInsn(((Integer) value).byteValue());
                        } else if (descriptorField.equals("C") && value instanceof Integer) {
                            
                            mv.visitLdcInsn((char) ((Integer) value).intValue());
                        } else if (value instanceof Integer) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Long) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Float) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Double) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Boolean) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Character) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof String) {
                            mv.visitLdcInsn(value);
                        }
                    }
                    
                    
                    String fieldName = fieldDecl.getFieldName();
                    String descriptorField = fieldDecl.getType().getDescriptor();
                    mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), fieldName, descriptorField);
                }
            }
        }
        
        mv.visitInsn(RETURN);
        
        
        int maxStack = 1 + paramTypes.length;
        for (Class<?> paramType : paramTypes) {
            if (paramType == long.class || paramType == double.class) {
                maxStack += 1;
            }
        }
        
        maxStack += 2;
        
        
        int localVarCount = 1; 
        for (Class<?> paramType : paramTypes) {
            if (paramType == long.class || paramType == double.class) {
                localVarCount += 2;
            } else {
                localVarCount += 1;
            }
        }
        
        mv.visitMaxs(maxStack, localVarCount);
        mv.visitEnd();
    }
    
    private void addField(ClassWriter cw, FieldDeclarationNode fieldDecl) {
        String fieldName = fieldDecl.getFieldName();
        String descriptor = fieldDecl.getType().getDescriptor();
        int modifiers = fieldDecl.getModifiers().toAccessFlags();
        if (modifiers == 0) {
            modifiers = ACC_PUBLIC;
        }
        
        
        ASTNode initialValue = fieldDecl.getInitialValue();
        Object constantValue = null;
        
        
        if (initialValue instanceof LiteralNode literal) {
            constantValue = literal.getValue();
        }
        
        FieldVisitor fv = cw.visitField(modifiers, fieldName, descriptor, null, constantValue);
        
        for (AnnotationNode annotation : fieldDecl.getAnnotations()) {
            addFieldAnnotation(fv, annotation);
        }
        
        fv.visitEnd();
    }
    
    private static void checkSuperConstructorAccessible(String superInternalName, Class<?>[] paramTypes) throws Exception {
        Class<?> superClass = Class.forName(superInternalName.replace('/', '.'));
        
        java.lang.reflect.Constructor<?> targetCtor = null;
        try {
            if (paramTypes == null || paramTypes.length == 0) {
                targetCtor = superClass.getDeclaredConstructor();
            } else {
                targetCtor = superClass.getDeclaredConstructor(paramTypes);
            }
        } catch (NoSuchMethodException e) {
            for (java.lang.reflect.Constructor<?> ctor : superClass.getDeclaredConstructors()) {
                if (ctor.getParameterTypes().length == (paramTypes != null ? paramTypes.length : 0)) {
                    targetCtor = ctor;
                    break;
                }
            }
        }
        
        if (targetCtor != null) {
            int mods = targetCtor.getModifiers();
            if (java.lang.reflect.Modifier.isPrivate(mods)) {
                throw new EvaluationException(
                    "Cannot extend " + superClass.getSimpleName() + 
                    ": its constructor is private",
                    new SourceLocation(0, 0),
                    ErrorCode.EVAL_INVALID_OPERATION,
                    null);
            }
        }
    }
    
    private void addDefaultConstructor(ClassWriter cw, String className, String superInternalName, List<FieldDeclarationNode> fields) {
        try {
            
            Class<?> superClass = Class.forName(superInternalName.replace('/', '.'));

            checkSuperConstructorAccessible(superInternalName, null);
            
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
            
            
            if (fields != null) {
                for (FieldDeclarationNode fieldDecl : fields) {
                    ASTNode initialValue = fieldDecl.getInitialValue();
                    if (initialValue instanceof LiteralNode literal) {
                        mv.visitVarInsn(ALOAD, 0);

                        Object value = literal.getValue();
                        String descriptorField = fieldDecl.getType().getDescriptor();
                        
                        if (descriptorField.equals("D") && value instanceof Integer) {
                            mv.visitLdcInsn(((Integer) value).doubleValue());
                        } else if (descriptorField.equals("F") && value instanceof Integer) {
                            mv.visitLdcInsn(((Integer) value).floatValue());
                        } else if (descriptorField.equals("J") && value instanceof Integer) {
                            mv.visitLdcInsn(((Integer) value).longValue());
                        } else if (descriptorField.equals("S") && value instanceof Integer) {
                            mv.visitLdcInsn(((Integer) value).shortValue());
                        } else if (descriptorField.equals("B") && value instanceof Integer) {
                            mv.visitLdcInsn(((Integer) value).byteValue());
                        } else if (descriptorField.equals("C") && value instanceof Integer) {
                            mv.visitLdcInsn((char) ((Integer) value).intValue());
                        } else if (value instanceof Integer) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Long) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Float) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Double) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Boolean) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof Character) {
                            mv.visitLdcInsn(value);
                        } else if (value instanceof String) {
                            mv.visitLdcInsn(value);
                        }
                        
                        String fieldName = fieldDecl.getFieldName();
                        String descriptorField2 = fieldDecl.getType().getDescriptor();
                        mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), fieldName, descriptorField2);
                    }
                }
            }
            
            boolean hasNonLiteralInit = false;
            if (fields != null) {
                for (FieldDeclarationNode field : fields) {
                    if (field.getInitialValue() != null && !(field.getInitialValue() instanceof LiteralNode)) {
                        hasNonLiteralInit = true;
                        break;
                    }
                }
            }
            
            if (hasNonLiteralInit) {
                mv.visitLdcInsn(className);
                mv.visitLdcInsn("<init>");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ACONST_NULL);
                mv.visitMethodInsn(INVOKESTATIC, 
                    "com/justnothing/javainterpreter/evaluator/MethodBodyExecutor", 
                    "executeMethod", 
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", 
                    false);
                mv.visitInsn(POP);
            }
            
            mv.visitInsn(RETURN);
            int maxStack = 1;
            if (fields != null && !fields.isEmpty()) {
                maxStack += 2;
            }
            if (hasNonLiteralInit) {
                maxStack += 5;
            }
            mv.visitMaxs(maxStack, 1);
            mv.visitEnd();
            
            registerDefaultConstructorBody(className, fields);
        } catch (Exception e) {
            
            try {
                Class<?> superClass = Class.forName(superInternalName.replace('/', '.'));
                
                java.lang.reflect.Constructor<?>[] constructors = superClass.getDeclaredConstructors();
                if (constructors.length > 0) {
                    
                    java.lang.reflect.Constructor<?> constructor = constructors[0];
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    
                    
                    StringBuilder descriptor = new StringBuilder("(");
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == int.class) {
                            descriptor.append("I");
                        } else if (paramType == long.class) {
                            descriptor.append("J");
                        } else if (paramType == float.class) {
                            descriptor.append("F");
                        } else if (paramType == double.class) {
                            descriptor.append("D");
                        } else if (paramType == boolean.class) {
                            descriptor.append("Z");
                        } else if (paramType == byte.class) {
                            descriptor.append("B");
                        } else if (paramType == char.class) {
                            descriptor.append("C");
                        } else if (paramType == short.class) {
                            descriptor.append("S");
                        } else if (paramType == void.class) {
                            descriptor.append("V");
                        } else {
                            descriptor.append("L").append(paramType.getName().replace('.', '/')).append(";");
                        }
                    }
                    descriptor.append(")V");
                    
                    
                    StringBuilder superDescriptor = new StringBuilder("(");
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == int.class) {
                            superDescriptor.append("I");
                        } else if (paramType == long.class) {
                            superDescriptor.append("J");
                        } else if (paramType == float.class) {
                            superDescriptor.append("F");
                        } else if (paramType == double.class) {
                            superDescriptor.append("D");
                        } else if (paramType == boolean.class) {
                            superDescriptor.append("Z");
                        } else if (paramType == byte.class) {
                            superDescriptor.append("B");
                        } else if (paramType == char.class) {
                            superDescriptor.append("C");
                        } else if (paramType == short.class) {
                            superDescriptor.append("S");
                        } else if (paramType == void.class) {
                            superDescriptor.append("V");
                        } else {
                            superDescriptor.append("L").append(paramType.getName().replace('.', '/')).append(";");
                        }
                    }
                    superDescriptor.append(")V");
                    
                    
                    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor.toString(), null, null);
                    mv.visitCode();
                    
                    
                    mv.visitVarInsn(ALOAD, 0);
                    
                    
                    int paramIndex = 1;
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == int.class) {
                            mv.visitVarInsn(ILOAD, paramIndex);
                        } else if (paramType == long.class) {
                            mv.visitVarInsn(LLOAD, paramIndex);
                            paramIndex += 1;
                        } else if (paramType == float.class) {
                            mv.visitVarInsn(FLOAD, paramIndex);
                        } else if (paramType == double.class) {
                            mv.visitVarInsn(DLOAD, paramIndex);
                            paramIndex += 1;
                        } else if (paramType == boolean.class) {
                            mv.visitVarInsn(ILOAD, paramIndex);
                        } else if (paramType == byte.class) {
                            mv.visitVarInsn(ILOAD, paramIndex);
                        } else if (paramType == char.class) {
                            mv.visitVarInsn(ILOAD, paramIndex);
                        } else if (paramType == short.class) {
                            mv.visitVarInsn(ILOAD, paramIndex);
                        } else {
                            mv.visitVarInsn(ALOAD, paramIndex);
                        }
                        paramIndex += 1;
                    }
                    
                    
                    mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDescriptor.toString(), false);
                    
                    
                    if (fields != null) {
                        for (FieldDeclarationNode fieldDecl : fields) {
                            ASTNode initialValue = fieldDecl.getInitialValue();
                            if (initialValue != null) {
                                
                                mv.visitVarInsn(ALOAD, 0);
                                
                                
                                if (initialValue instanceof LiteralNode literal) {
                                    Object value = literal.getValue();
                                    String descriptorField = fieldDecl.getType().getDescriptor();
                                    
                                    
                                    if (descriptorField.equals("D") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).doubleValue());
                                    } else if (descriptorField.equals("F") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).floatValue());
                                    } else if (descriptorField.equals("J") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).longValue());
                                    } else if (descriptorField.equals("S") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).shortValue());
                                    } else if (descriptorField.equals("B") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).byteValue());
                                    } else if (descriptorField.equals("C") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn((char) ((Integer) value).intValue());
                                    } else if (value instanceof Integer) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Long) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Float) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Double) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Boolean) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Character) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof String) {
                                        mv.visitLdcInsn(value);
                                    }
                                }
                                
                                
                                String fieldName = fieldDecl.getFieldName();
                                String descriptorField = fieldDecl.getType().getDescriptor();
                                mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), fieldName, descriptorField);
                            }
                        }
                    }
                    
                    mv.visitInsn(RETURN);
                    
                    
                    int maxStack = 1 + paramTypes.length;
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == long.class || paramType == double.class) {
                            maxStack += 1;
                        }
                    }
                    
                    if (fields != null && !fields.isEmpty()) {
                        maxStack += 2;
                    }
                    
                    
                    int localVarCount = 1; 
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == long.class || paramType == double.class) {
                            localVarCount += 2;
                        } else {
                            localVarCount += 1;
                        }
                    }
                    
                    mv.visitMaxs(maxStack, localVarCount);
                    mv.visitEnd();
                } else {
                    
                    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
                    
                    
                    if (fields != null) {
                        for (FieldDeclarationNode fieldDecl : fields) {
                            ASTNode initialValue = fieldDecl.getInitialValue();
                            if (initialValue != null) {
                                
                                mv.visitVarInsn(ALOAD, 0);
                                
                                
                                if (initialValue instanceof LiteralNode literal) {
                                    Object value = literal.getValue();
                                    String descriptorField = fieldDecl.getType().getDescriptor();
                                    
                                    
                                    if (descriptorField.equals("D") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).doubleValue());
                                    } else if (descriptorField.equals("F") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).floatValue());
                                    } else if (descriptorField.equals("J") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).longValue());
                                    } else if (descriptorField.equals("S") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).shortValue());
                                    } else if (descriptorField.equals("B") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn(((Integer) value).byteValue());
                                    } else if (descriptorField.equals("C") && value instanceof Integer) {
                                        
                                        mv.visitLdcInsn((char) ((Integer) value).intValue());
                                    } else if (value instanceof Integer) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Long) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Float) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Double) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Boolean) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof Character) {
                                        mv.visitLdcInsn(value);
                                    } else if (value instanceof String) {
                                        mv.visitLdcInsn(value);
                                    }
                                }
                                
                                
                                String fieldName = fieldDecl.getFieldName();
                                String descriptorField = fieldDecl.getType().getDescriptor();
                                mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), fieldName, descriptorField);
                            }
                        }
                    }
                    
                    mv.visitInsn(RETURN);
                    
                    int maxStack = 1;
                    
                    if (fields != null && !fields.isEmpty()) {
                        maxStack += 2;
                    }
                    mv.visitMaxs(maxStack, 1);
                    mv.visitEnd();
                }
            } catch (Exception ex) {
                
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
                
                
                if (fields != null) {
                    for (FieldDeclarationNode fieldDecl : fields) {
                        ASTNode initialValue = fieldDecl.getInitialValue();
                        if (initialValue != null) {
                            
                            mv.visitVarInsn(ALOAD, 0);
                            
                            
                            if (initialValue instanceof LiteralNode literal) {
                                Object value = literal.getValue();
                                if (value instanceof Integer) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof Long) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof Float) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof Double) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof Boolean) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof Character) {
                                    mv.visitLdcInsn(value);
                                } else if (value instanceof String) {
                                    mv.visitLdcInsn(value);
                                }
                            }
                            
                            
                            String fieldName = fieldDecl.getFieldName();
                            String descriptorField = fieldDecl.getType().getDescriptor();
                            mv.visitFieldInsn(PUTFIELD, className.replace('.', '/'), fieldName, descriptorField);
                        }
                    }
                }
                
                mv.visitInsn(RETURN);
                
                int maxStack = 1;
                
                if (fields != null && !fields.isEmpty()) {
                    maxStack += 2;
                }
                mv.visitMaxs(maxStack, 1);
                mv.visitEnd();
            }
        }
    }

    private void addConstructor(ClassWriter cw, ConstructorDeclarationNode constructorDecl, 
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
        String internalClassName = className.replace('.', '/');
        
        MethodVisitor mv = cw.visitMethod(modifiers, "<init>", descriptor.toString(), null, null);
        
        for (AnnotationNode annotation : constructorDecl.getAnnotations()) {
            addMethodAnnotation(mv, annotation);
        }
        
        mv.visitCode();
        
        generateSuperConstructorCall(mv, superInternalName, constructorDecl.getParameters());
        
        
        int paramIndex = 1; 
        for (int i = 0; i < constructorDecl.getParameters().size(); i++) {
            ParameterNode param = constructorDecl.getParameters().get(i);
            String paramName = param.getParameterName();
            
            
            mv.visitVarInsn(ALOAD, 0);
            
            int loadOpcode = param.getType().getLoadOpcode();
            mv.visitVarInsn(loadOpcode, paramIndex);
            
            
            for (FieldDeclarationNode fieldDecl : fields) {
                if (fieldDecl.getFieldName().equals(paramName)) {
                    mv.visitFieldInsn(PUTFIELD, internalClassName, paramName, 
                        param.getType().getDescriptor());
                    break;
                }
            }
            
            
            try {
                Class<?> superClass = Class.forName(superInternalName.replace('/', '.'));
                Field field = superClass.getDeclaredField(paramName);
                field.setAccessible(true);
                mv.visitFieldInsn(PUTFIELD, internalClassName, paramName,
                    param.getType().getDescriptor());
            } catch (Exception ignored) {
                
            }
            
            
            if (param.getType().getTypeName().equals("double") || param.getType().getTypeName().equals("long")) {
                paramIndex += 2; 
            } else {
                paramIndex += 1; 
            }
        }
        
        
        if (constructorDecl.getBody() != null) {
            generateConstructorBodyCall(mv, constructorDecl, className);
        }
        
        mv.visitInsn(RETURN);
        
        
        
        int maxStack = 2;
        for (ParameterNode param : constructorDecl.getParameters()) {
            if (param.getType().getTypeName().equals("double") || param.getType().getTypeName().equals("long")) {
                maxStack += 2;
            } else {
                maxStack += 1;
            }
        }
        
        if (constructorDecl.getBody() != null) {
            maxStack += 5; 
        }
        
        
        int localVarCount = 1; 
        for (ParameterNode param : constructorDecl.getParameters()) {
            if (param.getType().getTypeName().equals("double") || param.getType().getTypeName().equals("long")) {
                localVarCount += 2;
            } else {
                localVarCount += 1;
            }
        }
        mv.visitMaxs(maxStack, localVarCount);
        mv.visitEnd();
        
        
        registerConstructorBody(className, constructorDecl, fields);
    }
    
    private void generateConstructorBodyCall(MethodVisitor mv, ConstructorDeclarationNode constructorDecl, String className) {
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
                loadAndBoxParameter(mv, param.getType(), paramIndex);
                mv.visitInsn(AASTORE);
                
                
                if (param.getType().getTypeName().equals("double") || param.getType().getTypeName().equals("long")) {
                    paramIndex += 2; 
                } else {
                    paramIndex += 1; 
                }
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
    
    private void registerConstructorBody(String className, ConstructorDeclarationNode constructorDecl, List<FieldDeclarationNode> fields) {
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
                for (ASTNode stmt : ((BlockNode) bodyNode).getStatements()) {
                    allStatements.add(stmt);
                }
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
    
    private void registerDefaultConstructorBody(String className, List<FieldDeclarationNode> fields) {
        if (fields == null || fields.isEmpty()) return;
        
        boolean hasNonLiteralInit = false;
        for (FieldDeclarationNode field : fields) {
            if (field.getInitialValue() != null && !(field.getInitialValue() instanceof LiteralNode)) {
                hasNonLiteralInit = true;
                break;
            }
        }
        if (!hasNonLiteralInit) return;
        
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
    
    private void addMethod(ClassWriter cw, MethodDeclarationNode methodDecl, String className, boolean isInterface) {
        String methodName = methodDecl.getMethodName();
        String returnDescriptor = methodDecl.getReturnType().getDescriptor();
        
        StringBuilder descriptor = new StringBuilder("(");
        for (ParameterNode param : methodDecl.getParameters()) {
            descriptor.append(param.getType().getDescriptor());
        }
        descriptor.append(")").append(returnDescriptor);
        
        int modifiers = methodDecl.getModifiers().toAccessFlags();
        
        if (isInterface) {
            modifiers = modifiers | ACC_PUBLIC;
        }
        
        if (methodDecl.getBody() == null) {
            if (isInterface) {
                int abstractModifiers = ACC_PUBLIC | ACC_ABSTRACT;
                MethodVisitor mv = cw.visitMethod(abstractModifiers, methodName, descriptor.toString(), null, null);
                mv.visitEnd();
            }
            return;
        }
        
        MethodVisitor mv = cw.visitMethod(modifiers, methodName, descriptor.toString(), null, null);
        
        for (AnnotationNode annotation : methodDecl.getAnnotations()) {
            addMethodAnnotation(mv, annotation);
        }
        
        mv.visitCode();
        
        generateMethodBodyCall(mv, methodDecl, className);
        
        int maxLocals = methodDecl.getParameters().size() + (methodDecl.getModifiers().isStatic() ? 0 : 1);
        mv.visitMaxs(10, maxLocals);
        mv.visitEnd();
        
        MethodBodyExecutor.registerMethod(className, methodName, methodDecl, context);
    }
    
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
                loadAndBoxParameter(mv, param.getType(), i + slotOffset);
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
        
        unboxAndReturn(mv, methodDecl.getReturnType());
    }
    
    private void generateSuperConstructorCall(MethodVisitor mv, String superInternalName, List<ParameterNode> constructorParams) {
        Class<?> superClass = null;
        java.lang.reflect.Constructor<?> matchedConstructor = null;
        
        try {
            superClass = Class.forName(superInternalName.replace('/', '.'));
            matchedConstructor = findBestMatchingConstructor(superClass, constructorParams);
        } catch (ClassNotFoundException cnfe) {
            try {
                superClass = classLoader.loadClass(superInternalName.replace('/', '.'));
                matchedConstructor = findBestMatchingConstructor(superClass, constructorParams);
            } catch (Exception ignored) {
                
            }
        } catch (Exception ignored) {
            
        }
        
        if (matchedConstructor != null) {
            Class<?>[] superParamTypes = matchedConstructor.getParameterTypes();
            StringBuilder superDesc = new StringBuilder("(");
            for (Class<?> pt : superParamTypes) {
                superDesc.append(getFieldDescriptor(pt));
            }
            superDesc.append(")V");
            
            mv.visitVarInsn(ALOAD, 0);
            int loadIdx = 1;
            for (int i = 0; i < superParamTypes.length && i < constructorParams.size(); i++) {
                ParameterNode param = constructorParams.get(i);
                int loadOpcode = param.getType().getLoadOpcode();
                mv.visitVarInsn(loadOpcode, loadIdx);
                if (param.getType().getTypeName().equals("double") || param.getType().getTypeName().equals("long")) {
                    loadIdx += 2;
                } else {
                    loadIdx += 1;
                }
            }
            for (int i = constructorParams.size(); i < superParamTypes.length; i++) {
                Class<?> pt = superParamTypes[i];
                if (pt == boolean.class) {
                    mv.visitInsn(ICONST_0);
                } else if (pt == byte.class || pt == short.class || pt == int.class) {
                    mv.visitInsn(ICONST_0);
                } else if (pt == long.class) {
                    mv.visitInsn(LCONST_0);
                } else if (pt == float.class) {
                    mv.visitInsn(FCONST_0);
                } else if (pt == double.class) {
                    mv.visitInsn(DCONST_0);
                } else if (pt == char.class) {
                    mv.visitInsn(ICONST_0);
                } else {
                    mv.visitInsn(ACONST_NULL);
                }
            }
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDesc.toString(), false);
        } else if (superClass != null) {
            java.lang.reflect.Constructor<?>[] constructors = superClass.getDeclaredConstructors();
            if (constructors.length > 0) {
                java.lang.reflect.Constructor<?> anyConstructor = constructors[0];
                Class<?>[] superParamTypes = anyConstructor.getParameterTypes();
                StringBuilder superDesc = new StringBuilder("(");
                for (Class<?> pt : superParamTypes) {
                    superDesc.append(getFieldDescriptor(pt));
                }
                superDesc.append(")V");
                
                mv.visitVarInsn(ALOAD, 0);
                for (Class<?> pt : superParamTypes) {
                    if (pt == boolean.class) {
                        mv.visitInsn(ICONST_0);
                    } else if (pt == byte.class || pt == short.class || pt == int.class) {
                        mv.visitInsn(ICONST_0);
                    } else if (pt == long.class) {
                        mv.visitInsn(LCONST_0);
                    } else if (pt == float.class) {
                        mv.visitInsn(FCONST_0);
                    } else if (pt == double.class) {
                        mv.visitInsn(DCONST_0);
                    } else if (pt == char.class) {
                        mv.visitInsn(ICONST_0);
                    } else {
                        mv.visitInsn(ACONST_NULL);
                    }
                }
                mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", superDesc.toString(), false);
            } else {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
            }
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
        }
    }
    
    private java.lang.reflect.Constructor<?> findBestMatchingConstructor(Class<?> superClass, List<ParameterNode> params) {
        java.lang.reflect.Constructor<?> bestMatch = null;
        int bestScore = -1;
        int paramCount = params != null ? params.size() : 0;
        
        for (java.lang.reflect.Constructor<?> constructor : superClass.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            
            int score = 0;
            boolean compatible = true;
            for (int i = 0; i < paramTypes.length && i < paramCount; i++) {
                Class<?> expectedType = paramTypes[i];
                Class<?> actualType = params.get(i).getType().getResolvedClass();
                if (actualType == null) {
                    score += 1;
                    continue;
                }
                Class<?> unwrappedActual = unwrapPrimitive(actualType);
                Class<?> unwrappedExpected = unwrapPrimitive(expectedType);
                if (unwrappedExpected.isAssignableFrom(unwrappedActual)) {
                    score += unwrappedExpected == unwrappedActual ? 2 : 1;
                } else if (isPrimitiveAssignable(unwrappedExpected, unwrappedActual)) {
                    score += 1;
                } else {
                    compatible = false;
                    break;
                }
            }
            
            if (compatible && score > bestScore) {
                bestScore = score;
                bestMatch = constructor;
            }
        }
        
        return bestMatch;
    }
    
    private Class<?> unwrapPrimitive(Class<?> type) {
        if (type == Integer.class) return int.class;
        if (type == Long.class) return long.class;
        if (type == Float.class) return float.class;
        if (type == Double.class) return double.class;
        if (type == Boolean.class) return boolean.class;
        if (type == Byte.class) return byte.class;
        if (type == Character.class) return char.class;
        if (type == Short.class) return short.class;
        return type;
    }
    
    private boolean isPrimitiveAssignable(Class<?> target, Class<?> source) {
         if (target == int.class) return source == int.class || source == short.class || source == byte.class || source == char.class;
         if (target == long.class) return source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
         if (target == float.class) return source == float.class || source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
         if (target == double.class) return source == double.class || source == float.class || source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
         return false;
     }
     
     
    private String getFieldDescriptor(Class<?> type) {
        if (type == int.class) {
            return "I";
        } else if (type == long.class) {
            return "J";
        } else if (type == float.class) {
            return "F";
        } else if (type == double.class) {
            return "D";
        } else if (type == boolean.class) {
            return "Z";
        } else if (type == byte.class) {
            return "B";
        } else if (type == char.class) {
            return "C";
        } else if (type == short.class) {
            return "S";
        } else if (type == void.class) {
            return "V";
        } else {
            if (type.isArray()) {
                return type.getName().replace('.', '/');
            }
            return "L" + type.getName().replace('.', '/') + ";";
        }
    }
    
    private void loadAndBoxParameter(MethodVisitor mv, com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode type, int index) {
        if (type.isPrimitive()) {
            switch (type.getResolvedClass().getName()) {
                case "int" -> {
                    mv.visitVarInsn(ILOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                }
                case "long" -> {
                    mv.visitVarInsn(LLOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                }
                case "float" -> {
                    mv.visitVarInsn(FLOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                }
                case "double" -> {
                    mv.visitVarInsn(DLOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                }
                case "boolean" -> {
                    mv.visitVarInsn(ILOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                }
                case "char" -> {
                    mv.visitVarInsn(ILOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                }
                case "byte" -> {
                    mv.visitVarInsn(ILOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                }
                case "short" -> {
                    mv.visitVarInsn(ILOAD, index);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                }
                default -> mv.visitVarInsn(ALOAD, index);
            }
        } else {
            mv.visitVarInsn(ALOAD, index);
        }
    }
    
    private void unboxAndReturn(MethodVisitor mv, com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode returnType) {
        if (returnType.isPrimitive()) {
            String typeName = returnType.getResolvedClass().getName();
            switch (typeName) {
                case "void" -> {
                    mv.visitInsn(POP);
                    mv.visitInsn(RETURN);
                }
                case "int" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                    mv.visitInsn(IRETURN);
                }
                case "long" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
                    mv.visitInsn(LRETURN);
                }
                case "float" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
                    mv.visitInsn(FRETURN);
                }
                case "double" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                    mv.visitInsn(DRETURN);
                }
                case "boolean" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                    mv.visitInsn(IRETURN);
                }
                case "char" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                    mv.visitInsn(IRETURN);
                }
                case "byte" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
                    mv.visitInsn(IRETURN);
                }
                case "short" -> {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
                    mv.visitInsn(IRETURN);
                }
                default -> mv.visitInsn(ARETURN);
            }
        } else {
            mv.visitTypeInsn(CHECKCAST, returnType.getInternalName());
            mv.visitInsn(ARETURN);
        }
    }

    public Class<?> getGeneratedClass(String className) {
        return generatedClasses.get(className);
    }

    public boolean hasGeneratedClass(String className) {
        return generatedClasses.containsKey(className);
    }
    
    private void addClassAnnotation(ClassWriter cw, AnnotationNode annotation) {
        AnnotationVisitor av = cw.visitAnnotation("L" + annotation.getAnnotationName().replace('.', '/') + ";", true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }
    
    private void addFieldAnnotation(FieldVisitor fv, AnnotationNode annotation) {
        AnnotationVisitor av = fv.visitAnnotation("L" + annotation.getAnnotationName().replace('.', '/') + ";", true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }
    
    private void addMethodAnnotation(MethodVisitor mv, AnnotationNode annotation) {
        AnnotationVisitor av = mv.visitAnnotation("L" + annotation.getAnnotationName().replace('.', '/') + ";", true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }
    
    private void addAnnotationValues(AnnotationVisitor av, AnnotationNode annotation) {
        if (annotation.hasSingleValue()) {
            Object value = annotation.getValue();
            addAnnotationValue(av, "value", value);
        } else {
            for (Map.Entry<String, Object> entry : annotation.getValues().entrySet()) {
                addAnnotationValue(av, entry.getKey(), entry.getValue());
            }
        }
    }
    
    private void addAnnotationValue(AnnotationVisitor av, String name, Object value) {
        if (value == null) {
            av.visit(name, null);
        } else if (value instanceof String) {
            av.visit(name, value);
        } else if (value instanceof Integer) {
            av.visit(name, value);
        } else if (value instanceof Long) {
            av.visit(name, value);
        } else if (value instanceof Float) {
            av.visit(name, value);
        } else if (value instanceof Double) {
            av.visit(name, value);
        } else if (value instanceof Boolean) {
            av.visit(name, value);
        } else if (value instanceof Character) {
            av.visit(name, value);
        } else if (value instanceof Short) {
            av.visit(name, (int) (Short) value);
        } else if (value instanceof Byte) {
            av.visit(name, (int) (Byte) value);
        } else if (value instanceof Class<?>) {
            av.visit(name, Type.getType((Class<?>) value));
        } else if (value instanceof Object[]) {
            AnnotationVisitor arrayAv = av.visitArray(name);
            for (Object element : (Object[]) value) {
                addAnnotationValue(arrayAv, null, element);
            }
            arrayAv.visitEnd();
        } else if (value instanceof Enum<?>) {
            Enum<?> enumValue = (Enum<?>) value;
            av.visitEnum(name, Type.getDescriptor(enumValue.getDeclaringClass()), enumValue.name());
        } else {
            av.visit(name, value.toString());
        }
    }
    
    private boolean isClassAvailable(String className) {
        Class<?> clazz = ClassResolver.findClassWithImports(className, classLoader, context.getImports());
        if (clazz != null) {
            return true;
        }
        return generatedClasses.containsKey(className);
    }

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
                c = Class.forName(type1.replace('/', '.'), false, classLoader);
                d = Class.forName(type2.replace('/', '.'), false, classLoader);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (c.isAssignableFrom(d)) return type1;
            if (d.isAssignableFrom(c)) return type2;
            if (c.isInterface() || d.isInterface()) return "java/lang/Object";
            do { c = c.getSuperclass(); } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
}