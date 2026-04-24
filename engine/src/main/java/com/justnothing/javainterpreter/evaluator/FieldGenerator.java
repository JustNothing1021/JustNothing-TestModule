package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.nodes.AnnotationNode;
import com.justnothing.javainterpreter.ast.nodes.FieldDeclarationNode;
import com.justnothing.javainterpreter.ast.nodes.LiteralNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * 字段生成器。
 * 
 * <h3>字段在字节码中的表示</h3>
 * <p>在Java字节码中，字段由以下要素组成：
 * <ul>
 *   <li><b>访问标志</b>：public/private/protected, static, final, transient, volatile等</li>
 *   <li><b>名称</b>：字段名</li>
 *   <li><b>描述符</b>：类型描述符</li>
 *   <li><b>属性</b>：ConstantValue属性（final静态常量）、Signature属性（泛型签名）等</li>
 * </ul>
 * </p>
 * 
 * <h3>字段访问标志</h3>
 * <pre>
 * 标志名          值      说明
 * ------------   ----    ------------------
 * ACC_PUBLIC     0x0001  公有
 * ACC_PRIVATE    0x0002  私有
 * ACC_PROTECTED  0x0004  保护
 * ACC_STATIC     0x0008  静态
 * ACC_FINAL      0x0010  最终
 * ACC_VOLATILE   0x0040  易变
 * ACC_TRANSIENT  0x0080  瞬态
 * </pre>
 * 
 * <h3>字段初始化</h3>
 * <p>字段初始化有两种方式：
 * <ol>
 *   <li><b>编译时常量</b>：static final字段如果是字面量，会作为ConstantValue属性存储</li>
 *   <li><b>运行时初始化</b>：在构造函数或类初始化器中赋值</li>
 * </ol>
 * </p>
 * 
 * <h3>字段描述符示例</h3>
 * <pre>
 * Java类型          描述符
 * --------------   --------
 * int               I
 * long              J
 * String            Ljava/lang/String;
 * int[]             [I
 * String[][]        [[Ljava/lang/String;
 * </pre>
 * 
 * @author JustNothing
 * @see DynamicClassGenerator
 */
public class FieldGenerator {

    /**
     * 添加字段到类中。
     * 
     * <p>字段生成流程：
     * <ol>
     *   <li>确定访问标志</li>
     *   <li>确定常量值（如果是static final且初始值为字面量）</li>
     *   <li>创建FieldVisitor</li>
     *   <li>添加注解（如果有）</li>
     *   <li>结束字段访问</li>
     * </ol>
     * </p>
     * 
     * @param cw 类写入器
     * @param fieldDecl 字段声明节点
     */
    public static void addField(ClassWriter cw, FieldDeclarationNode fieldDecl) {
        String fieldName = fieldDecl.getFieldName();
        String descriptor = fieldDecl.getType().getDescriptor();
        
        int modifiers = fieldDecl.getModifiers().toAccessFlags();
        if (modifiers == 0) {
            modifiers = ACC_PUBLIC;
        }
        
        Object constantValue = extractConstantValue(fieldDecl);
        
        FieldVisitor fv = cw.visitField(modifiers, fieldName, descriptor, null, constantValue);
        
        addAnnotations(fv, fieldDecl);
        
        fv.visitEnd();
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 提取常量值。
     * 
     * <p>只有static final字段且初始值为字面量时，才能作为ConstantValue属性。
     * 这样JVM在类加载时就会直接设置该值，无需执行初始化代码。</p>
     * 
     * @param fieldDecl 字段声明节点
     * @return 常量值，如果不是常量则返回null
     */
    private static Object extractConstantValue(FieldDeclarationNode fieldDecl) {
        if (fieldDecl.getInitialValue() instanceof LiteralNode literal) {
            return literal.getValue();
        }
        return null;
    }
    
    /**
     * 添加字段注解。
     * 
     * @param fv 字段访问器
     * @param fieldDecl 字段声明节点
     */
    private static void addAnnotations(FieldVisitor fv, FieldDeclarationNode fieldDecl) {
        for (AnnotationNode annotation : fieldDecl.getAnnotations()) {
            AnnotationGenerator.addFieldAnnotation(fv, annotation);
        }
    }
}
