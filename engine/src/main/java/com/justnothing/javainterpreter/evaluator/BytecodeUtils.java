package com.justnothing.javainterpreter.evaluator;

import org.objectweb.asm.MethodVisitor;

import com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode;
import com.justnothing.javainterpreter.ast.nodes.ParameterNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * 字节码生成工具类。
 * 
 * <h3>ASM字节码生成基础</h3>
 * <p>ASM是一个Java字节码操作和分析框架，它可以直接生成或修改Java类文件。
 * 在运行时动态生成类时，我们需要使用ASM来生成符合JVM规范的字节码。</p>
 * 
 * <h3>JVM字节码基础概念</h3>
 * <ul>
 *   <li><b>描述符(Descriptor)</b>：用于描述类型、方法签名，如 "I" 表示int，"Ljava/lang/String;" 表示String</li>
 *   <li><b>内部名(Internal Name)</b>：类名在字节码中的表示，如 "java/lang/Object" 而非 "java.lang.Object"</li>
 *   <li><b>操作数栈(Operand Stack)</b>：JVM执行指令时使用的栈结构</li>
 *   <li><b>局部变量表(Local Variable Table)</b>：存储方法参数和局部变量的表</li>
 * </ul>
 * 
 * <h3>类型描述符对照表</h3>
 * <pre>
 * Java类型      描述符
 * ----------   --------
 * boolean       Z
 * byte          B
 * char          C
 * short         S
 * int           I
 * long          J
 * float         F
 * double        D
 * void          V
 * 对象类型      L包名/类名;
 * 数组类型      [元素类型描述符
 * </pre>
 * 
 * @author JustNothing
 * @see org.objectweb.asm.ClassWriter
 * @see org.objectweb.asm.MethodVisitor
 */
public final class BytecodeUtils {
    
    private BytecodeUtils() {
    }

    // ==================== 类型描述符相关 ====================
    
    /**
     * 获取类型的字节码描述符。
     * @param type Java类型
     * @return 该类型的字节码描述符
     */
    public static String getDescriptor(Class<?> type) {
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == void.class) return "V";
        
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        }
        
        return "L" + type.getName().replace('.', '/') + ";";
    }
    
    /**
     * 获取方法描述符。
     * @param paramTypes 参数类型数组
     * @param returnType 返回值类型
     * @return 方法描述符
     */
    public static String getMethodDescriptor(Class<?>[] paramTypes, Class<?> returnType) {
        StringBuilder sb = new StringBuilder("(");
        if (paramTypes != null) {
            for (Class<?> paramType : paramTypes) {
                sb.append(getDescriptor(paramType));
            }
        }
        sb.append(")").append(getDescriptor(returnType));
        return sb.toString();
    }
    
    /**
     * 获取构造函数描述符。
     * 
     * <p>构造函数在字节码中名称为 "&lt;init&gt;"，返回值类型为void。</p>
     * 
     * @param paramTypes 参数类型数组
     * @return 构造函数描述符
     */
    public static String getConstructorDescriptor(Class<?>[] paramTypes) {
        return getMethodDescriptor(paramTypes, void.class);
    }

    // ==================== 变量加载指令 ====================
    
    /**
     * 生成加载局部变量到操作数栈的指令。
     * 
     * <p>JVM使用不同的加载指令来加载不同类型的变量：
     * <ul>
     *   <li>ILOAD - 加载int, short, byte, char, boolean</li>
     *   <li>LLOAD - 加载long</li>
     *   <li>FLOAD - 加载float</li>
     *   <li>DLOAD - 加载double</li>
     *   <li>ALOAD - 加载对象引用</li>
     * </ul>
     * </p>
     * 
     * <p><b>注意</b>：long和double占用两个局部变量槽位(slot)。</p>
     * 
     * @param mv 方法访问器
     * @param type 变量类型
     * @param index 局部变量表中的索引
     */
    public static void loadVariable(MethodVisitor mv, Class<?> type, int index) {
        if (type == int.class || type == boolean.class || type == byte.class 
            || type == char.class || type == short.class) {
            mv.visitVarInsn(ILOAD, index);
        } else if (type == long.class) {
            mv.visitVarInsn(LLOAD, index);
        } else if (type == float.class) {
            mv.visitVarInsn(FLOAD, index);
        } else if (type == double.class) {
            mv.visitVarInsn(DLOAD, index);
        } else {
            mv.visitVarInsn(ALOAD, index);
        }
    }
    
    /**
     * 生成加载参数到操作数栈的指令（使用ClassReferenceNode）。
     * 
     * @param mv 方法访问器
     * @param type 类型引用节点
     * @param index 局部变量索引
     */
    public static void loadVariable(MethodVisitor mv, ClassReferenceNode type, int index) {
        int loadOpcode = type.getLoadOpcode();
        mv.visitVarInsn(loadOpcode, index);
    }
    
    /**
     * 生成将操作数栈顶值存储到局部变量的指令。
     * 
     * <p>存储指令与加载指令对应：
     * <ul>
     *   <li>ISTORE - 存储int, short, byte, char, boolean</li>
     *   <li>LSTORE - 存储long</li>
     *   <li>FSTORE - 存储float</li>
     *   <li>DSTORE - 存储double</li>
     *   <li>ASTORE - 存储对象引用</li>
     * </ul>
     * </p>
     * 
     * @param mv 方法访问器
     * @param type 变量类型
     * @param index 局部变量表中的索引
     */
    public static void storeVariable(MethodVisitor mv, Class<?> type, int index) {
        if (type == int.class || type == boolean.class || type == byte.class 
            || type == char.class || type == short.class) {
            mv.visitVarInsn(ISTORE, index);
        } else if (type == long.class) {
            mv.visitVarInsn(LSTORE, index);
        } else if (type == float.class) {
            mv.visitVarInsn(FSTORE, index);
        } else if (type == double.class) {
            mv.visitVarInsn(DSTORE, index);
        } else {
            mv.visitVarInsn(ASTORE, index);
        }
    }

    // ==================== 类型装箱/拆箱 ====================
    
    /**
     * 加载参数并将其装箱为Object类型。
     * 
     * <p>JVM中基本类型不是对象，需要装箱才能作为Object处理。
     * 例如：int -> Integer.valueOf(int)</p>
     * 
     * <p>装箱过程：
     * <ol>
     *   <li>从局部变量表加载基本类型值到栈</li>
     *   <li>调用对应包装类的valueOf方法</li>
     * </ol>
     * </p>
     * 
     * @param mv 方法访问器
     * @param type 类型引用节点
     * @param index 局部变量索引
     */
    public static void loadAndBoxParameter(MethodVisitor mv, ClassReferenceNode type, int index) {
        if (type.isPrimitive()) {
            String typeName = type.getResolvedClass().getName();
            switch (typeName) {
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
    
    /**
     * 从Object拆箱并生成返回指令。
     * 
     * <p>拆箱是装箱的逆过程，将包装类型转换回基本类型。
     * 例如：((Integer)obj).intValue()</p>
     * 
     * <p>拆箱返回过程：
     * <ol>
     *   <li>CHECKCAST - 检查栈顶对象是否为期望类型</li>
     *   <li>调用xxxValue()方法获取基本类型值</li>
     *   <li>生成对应类型的返回指令</li>
     * </ol>
     * </p>
     * 
     * @param mv 方法访问器
     * @param returnType 返回值类型引用节点
     */
    public static void unboxAndReturn(MethodVisitor mv, 
            com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode returnType) {
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

    // ==================== 字面量加载 ====================
    
    /**
     * 加载字面量值到操作数栈。
     * 
     * <p>JVM提供了多种加载常量的方式：
     * <ul>
     *   <li>ICONST_0~ICONST_5, ICONST_M1 - 加载小整数(-1到5)</li>
     *   <li>LCONST_0, LCONST_1 - 加载long常量0和1</li>
     *   <li>FCONST_0, FCONST_1, FCONST_2 - 加载float常量</li>
     *   <li>DCONST_0, DCONST_1 - 加载double常量</li>
     *   <li>BIPUSH - 加载-128到127之间的byte</li>
     *   <li>SIPUSH - 加载short范围整数</li>
     *   <li>LDC - 加载任意常量（包括字符串、类等）</li>
     * </ul>
     * </p>
     * 
     * @param mv 方法访问器
     * @param value 字面量值
     * @param targetType 目标类型描述符（用于类型转换）
     */
    public static void loadLiteralValue(MethodVisitor mv, Object value, String targetType) {
        if (value instanceof Integer intValue) {
            if (targetType != null) {
                switch (targetType) {
                    case "D" -> mv.visitLdcInsn(intValue.doubleValue());
                    case "F" -> mv.visitLdcInsn(intValue.floatValue());
                    case "J" -> mv.visitLdcInsn(intValue.longValue());
                    case "S" -> mv.visitLdcInsn(intValue.shortValue());
                    case "B" -> mv.visitLdcInsn(intValue.byteValue());
                    case "C" -> mv.visitLdcInsn((char) intValue.intValue());
                    default -> mv.visitLdcInsn(intValue);
                }
            } else {
                mv.visitLdcInsn(intValue);
            }
        } else if (value instanceof Long longValue) {
            mv.visitLdcInsn(longValue);
        } else if (value instanceof Float floatValue) {
            mv.visitLdcInsn(floatValue);
        } else if (value instanceof Double doubleValue) {
            mv.visitLdcInsn(doubleValue);
        } else if (value instanceof Boolean boolValue) {
            mv.visitLdcInsn(boolValue);
        } else if (value instanceof Character charValue) {
            mv.visitLdcInsn(charValue);
        } else if (value instanceof String strValue) {
            mv.visitLdcInsn(strValue);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    // ==================== 默认值加载 ====================
    
    /**
     * 加载类型的默认值到操作数栈。
     * 
     * <p>Java中各类型的默认值：
     * <ul>
     *   <li>boolean: false (0)</li>
     *   <li>byte, short, int, char: 0</li>
     *   <li>long: 0L</li>
     *   <li>float: 0.0f</li>
     *   <li>double: 0.0d</li>
     *   <li>对象引用: null</li>
     * </ul>
     * </p>
     * 
     * @param mv 方法访问器
     * @param type 类型
     */
    public static void loadDefaultValue(MethodVisitor mv, Class<?> type) {
        if (type == boolean.class) {
            mv.visitInsn(ICONST_0);
        } else if (type == byte.class || type == short.class || type == int.class || type == char.class) {
            mv.visitInsn(ICONST_0);
        } else if (type == long.class) {
            mv.visitInsn(LCONST_0);
        } else if (type == float.class) {
            mv.visitInsn(FCONST_0);
        } else if (type == double.class) {
            mv.visitInsn(DCONST_0);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
    }

    // ==================== 类型大小计算 ====================
    
    /**
     * 计算类型在局部变量表中占用的槽位数。
     * 
     * <p>JVM局部变量表中：
     * <ul>
     *   <li>long和double占用2个槽位</li>
     *   <li>其他所有类型占用1个槽位</li>
     * </ul>
     * </p>
     * 
     * @param type 类型
     * @return 槽位数（1或2）
     */
    public static int getSlotSize(Class<?> type) {
        return (type == long.class || type == double.class) ? 2 : 1;
    }
    
    /**
     * 计算类型在操作数栈中占用的槽位数。
     * 
     * <p>与局部变量表相同，long和double占用2个槽位。</p>
     * 
     * @param type 类型
     * @return 槽位数（1或2）
     */
    public static int getStackSize(Class<?> type) {
        return getSlotSize(type);
    }
    
    /**
     * 计算参数列表占用的总槽位数。
     * 
     * @param paramTypes 参数类型数组
     * @return 总槽位数
     */
    public static int calculateParamSlotCount(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return 0;
        }
        int count = 0;
        for (Class<?> type : paramTypes) {
            count += getSlotSize(type);
        }
        return count;
    }

    // ==================== 类型转换辅助 ====================
    
    /**
     * 将包装类型拆箱为基本类型。
     * 
     * <p>例如：Integer.class -> int.class</p>
     * 
     * @param type 可能是包装类型
     * @return 对应的基本类型，如果不是包装类型则返回原类型
     */
    public static Class<?> unwrapPrimitive(Class<?> type) {
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
    
    /**
     * 检查基本类型之间的赋值兼容性。
     * 
     * <p>Java支持自动类型拓宽转换：
     * byte -> short -> int -> long -> float -> double
     * char -> int -> long -> float -> double
     * </p>
     * 
     * @param target 目标类型
     * @param source 源类型
     * @return 是否可以赋值
     */
    public static boolean isPrimitiveAssignable(Class<?> target, Class<?> source) {
        if (target == int.class) 
            return source == int.class || source == short.class || source == byte.class || source == char.class;
        if (target == long.class) 
            return source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
        if (target == float.class) 
            return source == float.class || source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
        if (target == double.class) 
            return source == double.class || source == float.class || source == long.class || source == int.class || source == short.class || source == byte.class || source == char.class;
        return false;
    }

    // ==================== 构造函数匹配 ====================
    
    /**
     * 查找最佳匹配的构造函数。
     * 
     * <p>使用评分机制选择最匹配的构造函数：
     * <ul>
     *   <li>完全匹配：2分</li>
     *   <li>兼容匹配（父类/接口）：1分</li>
     *   <li>不兼容：排除</li>
     * </ul>
     * </p>
     * 
     * @param superClass 父类
     * @param params 参数节点列表
     * @return 最佳匹配的构造函数，如果没有匹配则返回null
     */
    public static Constructor<?> findBestMatchingConstructor(Class<?> superClass, List<ParameterNode> params) {
        Constructor<?> bestMatch = null;
        int bestScore = -1;
        int paramCount = params != null ? params.size() : 0;
        
        for (Constructor<?> constructor : superClass.getDeclaredConstructors()) {
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

    // ==================== 访问权限检查 ====================
    
    /**
     * 检查构造函数是否可访问（非private）。
     * 
     * @param constructor 构造函数
     * @return 如果可访问返回true
     */
    public static boolean isConstructorAccessible(Constructor<?> constructor) {
        if (constructor == null) return false;
        int mods = constructor.getModifiers();
        return !Modifier.isPrivate(mods);
    }
    
    /**
     * 检查字段是否可访问（非private）。
     * 
     * @param field 字段
     * @return 如果可访问返回true
     */
    public static boolean isFieldAccessible(Field field) {
        if (field == null) return false;
        int mods = field.getModifiers();
        return !Modifier.isPrivate(mods);
    }

    // ==================== 名称转换 ====================
    
    /**
     * 将类名转换为内部名格式。
     * @param className 点分隔的类名
     * @return 斜杠分隔的内部名
     */
    public static String toInternalName(String className) {
        return className.replace('.', '/');
    }
    
    /**
     * 将内部名转换为类名格式。
     * @param internalName 斜杠分隔的内部名
     * @return 点分隔的类名
     */
    public static String toClassName(String internalName) {
        return internalName.replace('/', '.');
    }
}
