package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.nodes.AnnotationNode;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Map;

/**
 * 注解生成器。
 * 
 * <h3>注解在字节码中的表示</h3>
 * <p>在Java字节码中，注解以RuntimeVisibleAnnotations或RuntimeInvisibleAnnotations属性存储：
 * <ul>
 *   <li><b>RuntimeVisibleAnnotations</b>：运行时可见注解（@Retention(RUNTIME)）</li>
 *   <li><b>RuntimeInvisibleAnnotations</b>：运行时不可见注解（@Retention(CLASS)）</li>
 * </ul>
 * </p>
 * 
 * <h3>注解元素类型</h3>
 * <p>注解可以包含以下类型的元素：
 * <ul>
 *   <li>基本类型：byte, short, int, long, float, double, char, boolean</li>
 *   <li>String</li>
 *   <li>Class</li>
 *   <li>枚举常量</li>
 *   <li>注解（嵌套注解）</li>
 *   <li>上述类型的数组</li>
 * </ul>
 * </p>
 * 
 * <h3>注解描述符格式</h3>
 * <p>注解在字节码中使用类型描述符表示，例如：
 * <ul>
 *   <li>@Override -> Ljava/lang/Override;</li>
 *   <li>@Deprecated -> Ljava/lang/Deprecated;</li>
 *   <li>@SuppressWarnings("unchecked") -> Ljava/lang/SuppressWarnings;</li>
 * </ul>
 * </p>
 * 
 * <h3>注解生成示例</h3>
 * <pre>
 * // Java代码
 * {@literal @}SuppressWarnings("unchecked")
 * public void foo() {}
 * 
 * // 字节码生成
 * MethodVisitor mv = ...;
 * AnnotationVisitor av = mv.visitAnnotation("Ljava/lang/SuppressWarnings;", true);
 * av.visit("value", "unchecked");
 * av.visitEnd();
 * </pre>
 * 
 * @author JustNothing
 * @see DynamicClassGenerator
 */
public class AnnotationGenerator {

    /**
     * 添加类注解。
     * 
     * @param cw 类写入器
     * @param annotation 注解节点
     */
    public static void addClassAnnotation(ClassWriter cw, AnnotationNode annotation) {
        String descriptor = "L" + annotation.getAnnotationName().replace('.', '/') + ";";
        AnnotationVisitor av = cw.visitAnnotation(descriptor, true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }
    
    /**
     * 添加字段注解。
     * 
     * @param fv 字段访问器
     * @param annotation 注解节点
     */
    public static void addFieldAnnotation(FieldVisitor fv, AnnotationNode annotation) {
        String descriptor = "L" + annotation.getAnnotationName().replace('.', '/') + ";";
        AnnotationVisitor av = fv.visitAnnotation(descriptor, true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }
    
    /**
     * 添加方法注解。
     * 
     * @param mv 方法访问器
     * @param annotation 注解节点
     */
    public static void addMethodAnnotation(MethodVisitor mv, AnnotationNode annotation) {
        String descriptor = "L" + annotation.getAnnotationName().replace('.', '/') + ";";
        AnnotationVisitor av = mv.visitAnnotation(descriptor, true);
        addAnnotationValues(av, annotation);
        av.visitEnd();
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 添加注解值。
     * 
     * <p>处理注解的单值和多值情况：
     * <ul>
     *   <li>单值：使用 visit("value", value)</li>
     *   <li>多值：遍历每个键值对，使用 visit(key, value)</li>
     * </ul>
     * </p>
     * 
     * @param av 注解访问器
     * @param annotation 注解节点
     */
    private static void addAnnotationValues(AnnotationVisitor av, AnnotationNode annotation) {
        if (annotation.hasSingleValue()) {
            Object value = annotation.getValue();
            addAnnotationValue(av, "value", value);
        } else {
            for (Map.Entry<String, Object> entry : annotation.getValues().entrySet()) {
                addAnnotationValue(av, entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * 添加单个注解值。
     * 
     * <p>根据值的类型选择正确的visit方法：
     * <ul>
     *   <li>基本类型包装类：直接传递</li>
     *   <li>String：直接传递</li>
     *   <li>Class：转换为Type</li>
     *   <li>枚举：使用visitEnum</li>
     *   <li>数组：使用visitArray</li>
     * </ul>
     * </p>
     * 
     * @param av 注解访问器
     * @param name 属性名（可以为null，用于数组元素）
     * @param value 属性值
     */
    private static void addAnnotationValue(AnnotationVisitor av, String name, Object value) {
        if (value == null) {
            av.visit(name, null);
        } else if (value instanceof String strValue) {
            av.visit(name, strValue);
        } else if (value instanceof Integer intValue) {
            av.visit(name, intValue);
        } else if (value instanceof Long longValue) {
            av.visit(name, longValue);
        } else if (value instanceof Float floatValue) {
            av.visit(name, floatValue);
        } else if (value instanceof Double doubleValue) {
            av.visit(name, doubleValue);
        } else if (value instanceof Boolean boolValue) {
            av.visit(name, boolValue);
        } else if (value instanceof Character charValue) {
            av.visit(name, charValue);
        } else if (value instanceof Short shortValue) {
            av.visit(name, (int) shortValue);
        } else if (value instanceof Byte byteValue) {
            av.visit(name, (int) byteValue);
        } else if (value instanceof Class<?> clazz) {
            av.visit(name, Type.getType(clazz));
        } else if (value instanceof Object[] arrayValue) {
            AnnotationVisitor arrayAv = av.visitArray(name);
            for (Object element : arrayValue) {
                addAnnotationValue(arrayAv, null, element);
            }
            arrayAv.visitEnd();
        } else if (value instanceof Enum<?> enumValue) {
            av.visitEnum(name, Type.getDescriptor(enumValue.getDeclaringClass()), enumValue.name());
        } else {
            av.visit(name, value.toString());
        }
    }
}
