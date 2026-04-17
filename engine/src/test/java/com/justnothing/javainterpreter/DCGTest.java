package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.evaluator.DynamicClassGenerator;
import com.justnothing.javainterpreter.evaluator.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 直接测试 DynamicClassGenerator 的 defineClass 能力
 */
public class DCGTest {

    private static final SourceLocation LOC = new SourceLocation(1, 1);

    private ExecutionContext context;
    private DynamicClassGenerator generator;

    @Before
    public void setUp() {
        context = new ExecutionContext();
        generator = new DynamicClassGenerator(context);
    }

    @Test
    public void test_anonymousObject_emptyBody() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "Object__Anonymous1", "java.lang.Object", null
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull("类应该被成功定义", clazz);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        assertNotNull("实例应该能创建", instance);
    }

    @Test
    public void test_anonymousObject_withField() throws Exception {
        List<FieldDeclarationNode> fields = new ArrayList<>();
        fields.add(new FieldDeclarationNode(
            "value",
            ClassReferenceNode.of("int", int.class, true, LOC),
            new LiteralNode(42, int.class, LOC),
            new ClassModifiers(),
            LOC
        ));
        ClassDeclarationNode classDecl = createAnonymousClass(
            "Object__Anonymous2", "java.lang.Object", fields
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull(clazz);
        clazz.getDeclaredConstructor().newInstance();
    }

    @Test
    public void test_inheritRuntimeException() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "RuntimeException__Anon", "java.lang.RuntimeException", null
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull(clazz);
        assertTrue(java.lang.RuntimeException.class.isInstance(clazz.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void test_inheritThread() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "Thread__Anon", "java.lang.Thread", null
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull(clazz);
        assertTrue(java.lang.Thread.class.isInstance(clazz.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void test_inheritArrayList() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "ArrayList__Anon", "java.util.ArrayList", null
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull(clazz);
        assertTrue(java.util.ArrayList.class.isInstance(clazz.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void test_inheritHashMap() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "HashMap__Anon", "java.util.HashMap", null
        );
        Class<?> clazz = generator.generateClass(classDecl);
        assertNotNull(clazz);
        assertTrue(java.util.HashMap.class.isInstance(clazz.getDeclaredConstructor().newInstance()));
    }

    @Test(expected = Throwable.class)
    public void test_finalClass_ProcessBuilder() throws Exception {
        ClassDeclarationNode classDecl = createAnonymousClass(
            "PB__Anon", "java.lang.ProcessBuilder", null
        );
        generator.generateClass(classDecl);
    }

    // ========== 辅助方法 ==========

    private ClassDeclarationNode createAnonymousClass(
            String className,
            String superClassName,
            List<FieldDeclarationNode> fields) {

        ClassReferenceNode superClassRef = null;
        if (superClassName != null) {
            String simpleName = superClassName.substring(superClassName.lastIndexOf('.') + 1);
            try {
                Class<?> resolved = Class.forName(superClassName);
                superClassRef = ClassReferenceNode.of(simpleName, resolved, false, LOC);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        ClassDeclarationNode classDecl = new ClassDeclarationNode(
            className,
            superClassRef,
            new ArrayList<>(),
            LOC
        );

        if (fields != null) {
            for (FieldDeclarationNode field : fields) {
                classDecl.addField(field);
            }
        }

        return classDecl;
    }
}
