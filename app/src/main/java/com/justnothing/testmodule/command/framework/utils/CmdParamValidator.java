package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 参数校验：取值约束、必填、互斥/依赖。
 */
public class CmdParamValidator {

    private static final Logger logger = Logger.getLoggerForName("CmdParamValidator");

    public static void validateFieldValue(CmdParam param, String fieldName, Object value) throws IllegalArgumentException {
        if (!param.pattern().isEmpty()) {
            try {
                Pattern p = Pattern.compile(param.pattern());
                if (!p.matcher(value.toString()).matches()) {
                    throw new IllegalArgumentException("参数 " + fieldName + " 值 '" + value + "' 不匹配模式: " + param.pattern());
                }
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("无效的正则表达式: " + param.pattern(), e);
            }
        }

        if (param.allowedValues().length > 0) {
            boolean found = Arrays.stream(param.allowedValues())
                .anyMatch(allowed -> allowed.equals(value.toString()));
            if (!found) {
                throw new IllegalArgumentException(
                    "参数 " + fieldName + " 值 '" + value + "' 不在允许列表中: " + Arrays.toString(param.allowedValues()));
            }
        }

        boolean hasRange = param.min() > Double.NEGATIVE_INFINITY
                || param.max() < Double.POSITIVE_INFINITY;
        if (hasRange) {
            if (!(value instanceof Number)) {
                // 声明错误：min/max 只能约束数值型参数。字符串等类型无法比较大小，
                // 若静默跳过会让约束形同虚设，因此这里直接快速失败暴露问题。
                throw new IllegalStateException(
                    "参数 " + fieldName + " 声明了 min/max，但其值 '" + value
                    + "' (" + value.getClass().getSimpleName() + ") 不是数值类型，无法比较大小");
            }
            double numValue = ((Number) value).doubleValue();
            if (numValue < param.min()) {
                throw new IllegalArgumentException(
                    "参数 " + fieldName + " 值 " + numValue + " 小于最小值 " + param.min());
            }
            if (numValue > param.max()) {
                throw new IllegalArgumentException(
                    "参数 " + fieldName + " 值 " + numValue + " 大于最大值 " + param.max());
            }
        }
    }

    public static void validateRequiredFields(CommandRequest<?> request, List<CmdParamProcessor.FieldInfo> fields,
                                                Set<String> explicitlySet) throws IllegalArgumentException {
        for (CmdParamProcessor.FieldInfo fi : fields) {
            if (!fi.param().required()) continue;

            try {
                fi.field().setAccessible(true);
                Object value = fi.field().get(request);

                if (value == null ||
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof List && ((List<?>) value).isEmpty())) {

                    String defaultVal = fi.param().defaultValue();
                    if (!defaultVal.isEmpty()) {
                        Object converted = CmdValueConverter.convertValue(defaultVal, fi.field().getType());
                        fi.field().set(request, converted);
                        logger.debug("使用默认值: " + fi.param().name() + " = " + defaultVal);
                    } else {
                        throw new IllegalArgumentException("缺少必填参数: " + fi.param().name());
                    }
                } else if (isPrimitiveDefaultValue(value) && !explicitlySet.contains(fi.field().getName())) {
                    String defaultVal = fi.param().defaultValue();
                    if (!defaultVal.isEmpty()) {
                        Object converted = CmdValueConverter.convertValue(defaultVal, fi.field().getType());
                        fi.field().set(request, converted);
                        logger.debug("使用默认值: " + fi.param().name() + " = " + defaultVal);
                    } else {
                        throw new IllegalArgumentException("缺少必填参数: " + fi.param().name());
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + fi.field().getName(), e);
            }
        }
    }

    public static boolean isPrimitiveDefaultValue(Object value) {
        if (value == null) return false;
        Class<?> type = value.getClass();
        if (type == Integer.class) return ((Integer) value) == 0;
        if (type == Long.class) return ((Long) value) == 0L;
        if (type == Double.class) return ((Double) value) == 0.0;
        if (type == Float.class) return ((Float) value) == 0.0f;
        if (type == Short.class) return ((Short) value) == (short) 0;
        if (type == Byte.class) return ((Byte) value) == (byte) 0;
        if (type == Character.class) return ((Character) value) == '\0';
        if (type == Boolean.class) return !((Boolean) value);
        return false;
    }

    /**
     *  验证互斥约束（Mutex）
     *  <p>
     *  只要 A.mutexWith 中任一目标参数也被显式使用 → 一律报错，不再区分
     *  "单向互斥" 与 "双向互斥组"。此前双向声明（互斥组）会被静默放行，
     *  导致互斥语义形同虚设（例如同时给出 --get 与 --set 时不报错）。
     */
    public static void validateMutexConstraints(List<CmdParamProcessor.FieldInfo> fields,
                                                 Set<String> explicitlySet) throws IllegalArgumentException {
        Map<String, Boolean> paramUsage = new HashMap<>();

        // 第一遍：收集哪些参数被"显式设置"过。
        // 依据 explicitlySet（解析过程中真实命中的字段），而非字段值启发式——后者会把
        // int/boolean 等原始类型的默认零值误判为"已使用"，导致 requires 依赖校验恒失效。
        for (CmdParamProcessor.FieldInfo fi : fields) {
            if (!explicitlySet.contains(fi.field().getName())) continue;

            paramUsage.put(fi.param().name().toLowerCase(), true);
            // 也记录别名
            for (String alias : fi.param().aliases()) {
                paramUsage.put(alias.toLowerCase(), true);
            }
        }

        // 第二遍：检测互斥约束（无论单向还是双向声明，同时出现即报错）
        for (CmdParamProcessor.FieldInfo fi : fields) {
            String[] mutexWith = fi.param().mutexWith();
            if (mutexWith.length == 0) continue;
            if (!explicitlySet.contains(fi.field().getName())) continue;

            for (String mutexTarget : mutexWith) {
                if (!paramUsage.containsKey(mutexTarget.toLowerCase())) continue;

                throw new IllegalArgumentException(
                    "参数 '" + fi.param().name() + "' 与 '" + mutexTarget + "' 互斥，不能同时使用。\n" +
                    "提示: 请选择其中之一");
            }
        }

        // 第三遍：检查依赖约束（requires）
        for (CmdParamProcessor.FieldInfo fi : fields) {
            String[] requires = fi.param().requires();
            if (requires.length == 0) continue;
            if (!explicitlySet.contains(fi.field().getName())) continue;

            for (String requiredParam : requires) {
                if (!paramUsage.containsKey(requiredParam.toLowerCase())) {
                    throw new IllegalArgumentException(
                        "参数 '" + fi.param().name() + "' 需要同时指定 '" + requiredParam + "'。\n" +
                        "提示: 请添加 --" + requiredParam + " 参数");
                }
            }
        }
    }
}
