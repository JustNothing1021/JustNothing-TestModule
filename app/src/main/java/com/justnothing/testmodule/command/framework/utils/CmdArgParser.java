package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 命令行参数解析核心（标准 @CmdParam 声明式解析）。
 */
public class CmdArgParser {

    private static final Logger logger = Logger.getLoggerForName("CmdArgParser");

    /**
     * 预处理：合并反引号包裹的参数
     * 支持带空格的表达式（如 `new ArrayList()` 或 `new String("hello world")`）
     * <p>
     * 示例:
     *   输入: ["-i", "`new", "ArrayList()`"]
     *   输出: ["-i", "new ArrayList()"]
     * <p>
     *   输入: ["-i", "`new", "String(\"hello\",", "\"world\")`"]
     *   输出: ["-i", "new String(\"hello\", \"world\")"]
     *
     * @param args 原始参数数组
     * @return 处理后的参数数组（反引号已移除，内容已合并）
     */
    private static String[] preprocessBacktickArgs(String[] args) {
        if (args == null || args.length == 0) return args;

        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < args.length) {
            String arg = args[i];

            if (arg.startsWith("`")) {
                // 单参数自闭合反引号：如 `java.lang.String`，直接去掉首尾反引号
                if (arg.endsWith("`") && arg.length() > 1) {
                    result.add(arg.substring(1, arg.length() - 1));
                    i++;
                    continue;
                }
                // 发现反引号开始
                StringBuilder merged = new StringBuilder();
                
                // 移除开头的 `
                if (arg.length() > 1) {
                    merged.append(arg.substring(1));
                }
                i++;

                // 向后查找配对的 closing `
                boolean foundClosing = false;
                while (i < args.length) {
                    String nextArg = args[i];
                    
                    if (nextArg.endsWith("`")) {
                        // 找到配对的反引号
                        if (nextArg.length() > 1) {
                            merged.append(" ").append(nextArg, 0, nextArg.length() - 1);
                        }
                        foundClosing = true;
                        i++;
                        break;
                    } else {
                        // 还没找到，继续拼接
                        if (merged.length() > 0) merged.append(" ");
                        merged.append(nextArg);
                        i++;
                    }
                }

                if (!foundClosing) {
                    logger.warn("未闭合的反引号表达式，保留原样: `" + merged);
                    result.add("`" + merged);
                } else {
                    result.add(merged.toString());
                    logger.debug("反引号合并: → \"" + merged + "\"");
                }
            } else {
                // 普通参数，直接添加
                result.add(arg);
                i++;
            }
        }

        return result.toArray(new String[0]);
    }

    public static void parse(CommandRequest<?> request, String[] args) throws IllegalArgumentException {
        // 预处理：合并反引号包裹的参数（支持带空格的表达式）
        args = preprocessBacktickArgs(args);

        List<CmdParamProcessor.FieldInfo> fields = CmdParamProcessor.getCmdParamFields(request.getClass());
        Map<String, CmdParamProcessor.FieldInfo> paramIndex = buildParamIndex(fields);
        Set<String> explicitlySet = new HashSet<>();

        // 初始化所有字段的默认值（包括 required=false 的可选字段）
        initializeDefaultValues(request, fields);

        // 第一遍：按原始顺序处理关键字参数，收集未消费的候选位置参数
        List<String> positionalCandidates = new ArrayList<>();

        int i = 0;
        while (i < args.length) {
            String arg = args[i];

            if (arg.startsWith("-") && !arg.startsWith("--")) {
                // 可能是关键字参数（单 - 开头）或负数（如 -1, -3.14）
                CmdParamProcessor.FieldInfo fieldInfo = findMatchingParam(arg, paramIndex);

                if (fieldInfo != null && fieldInfo.param().isOperator()) {
                    // 操作符（-g / --get / get 三种写法均支持）：消费后续参数
                    i = handleOperator(request, fields, fieldInfo, args, i, explicitlySet);
                } else if (fieldInfo != null) {
                    // 已知的关键字参数：正常处理
                    i = setFieldValue(request, fieldInfo, args, i, explicitlySet);
                } else if (looksLikeNegativeNumber(arg)) {
                    // 看起来像负数：当作位置参数候选
                    logger.debug(" 识别为数值参数: " + arg + " (不是flag)");
                    positionalCandidates.add(arg);
                    i++;
                } else {
                    // 未知的 flag 参数：报错或忽略
                    logger.warn("未知参数: " + arg + ", 忽略");
                    i++;
                }
            } else if (arg.startsWith("--")) {
                // 双横线关键字参数（如 --class, --verbose, --timeout=1000）
                String lookupKey = arg;
                
                // 处理 --key=value 格式：只使用 key 部分进行查找
                if (arg.contains("=")) {
                    lookupKey = arg.substring(0, arg.indexOf('='));
                }
                
                CmdParamProcessor.FieldInfo fieldInfo = findMatchingParam(lookupKey, paramIndex);

                if (fieldInfo != null && fieldInfo.param().isOperator()) {
                    // 操作符（--get / --set 等）：消费后续参数
                    i = handleOperator(request, fields, fieldInfo, args, i, explicitlySet);
                } else if (fieldInfo != null) {
                    i = setFieldValue(request, fieldInfo, args, i, explicitlySet);
                } else {
                    logger.warn("未知参数: " + arg + ", 忽略");
                    i++;
                }
            } else {
                // 非关键字参数：检查是否为操作符（Git风格：get, set 等）
                CmdParamProcessor.FieldInfo operatorInfo = findMatchingParam(arg, paramIndex);

                if (operatorInfo != null && operatorInfo.param().isOperator()) {
                    // 发现操作符！特殊处理（传入 allFields 支持分离模式）
                    i = handleOperator(request, fields, operatorInfo, args, i, explicitlySet);
                } else {
                    // 普通位置参数候选
                    positionalCandidates.add(arg);
                    i++;
                }
            }
        }

        // 第二遍：将候选按 position 顺序严格填充位置参数，剩余的全部交给 varArgs。
        //
        // 这里刻意不使用"候选数是否恰好等于必需数（或 +1）"的启发式去猜测用户是否省略了
        // 某个可选位置参数：该启发式只对"带 defaultValue 的可选参数"生效，会让 varArgs 的
        // 起点随传入的参数个数漂移，行为不可预测。改为严格顺序填充：用户想跳过中间的可选
        // 位置参数时，应改用 --name 显式传值，或依赖其默认值。
        if (!positionalCandidates.isEmpty()) {
            List<CmdParamProcessor.FieldInfo> positionalFields = fields.stream()
                .filter(fi -> fi.param().position() > 0)
                .sorted(Comparator.comparingInt(a -> a.param().position()))
                .collect(Collectors.toList());

            // varArgs 字段必须是 position 最大的那个（由参数声明校验测试兜底）
            List<CmdParamProcessor.FieldInfo> normalPositionalFields = new ArrayList<>();
            CmdParamProcessor.FieldInfo varArgsField = null;

            for (CmdParamProcessor.FieldInfo fi : positionalFields) {
                if (fi.param().varArgs()) {
                    varArgsField = fi;
                } else {
                    normalPositionalFields.add(fi);
                }
            }

            int argIndex = 0;

            // Phase 1: 按 position 升序依次消费候选，不做任何跳过
            for (CmdParamProcessor.FieldInfo fieldInfo : normalPositionalFields) {
                if (argIndex >= positionalCandidates.size()) break;

                String valueStr = positionalCandidates.get(argIndex++);

                try {
                    fieldInfo.field().setAccessible(true);
                    Object value = CmdValueConverter.convertValue(valueStr, fieldInfo.field().getType(), fieldInfo.param().readMode());
                    CmdParamValidator.validateFieldValue(fieldInfo.param(), fieldInfo.field().getName(), value);
                    fieldInfo.field().set(request, value);
                    invokeSetterIfPresent(request, fieldInfo.field(), value);
                    explicitlySet.add(fieldInfo.field().getName());
                    logger.debug(" 位置参数[" + fieldInfo.param().position() + "] " +
                               fieldInfo.field().getName() + " = " + valueStr +
                               " [mode=" + fieldInfo.param().readMode() + "]");
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "设置位置参数 " + fieldInfo.field().getName() + " 失败: " + e.getMessage(), e);
                }
            }

            // Phase 2: 剩余候选全部归 varArgs
            if (argIndex < positionalCandidates.size()) {
                if (varArgsField == null) {
                    throw new IllegalArgumentException(
                        "位置参数过多: 提供了 " + positionalCandidates.size() +
                        " 个, 最多支持 " + normalPositionalFields.size() + " 个（没有 varArgs 字段接收剩余参数）");
                }

                List<String> remainingArgs = positionalCandidates.subList(argIndex, positionalCandidates.size());
                String varArgsValue = String.join(" ", remainingArgs);

                try {
                    varArgsField.field().setAccessible(true);
                    Object value = CmdValueConverter.convertValue(varArgsValue, varArgsField.field().getType(), varArgsField.param().readMode());
                    CmdParamValidator.validateFieldValue(varArgsField.param(), varArgsField.field().getName(), value);
                    varArgsField.field().set(request, value);
                    invokeSetterIfPresent(request, varArgsField.field(), value);
                    explicitlySet.add(varArgsField.field().getName());
                    logger.debug(" varArgs参数[" + varArgsField.param().position() + "] " +
                               varArgsField.field().getName() + " = " + varArgsValue +
                               " [mode=" + varArgsField.param().readMode() + "] (" +
                               remainingArgs.size() + " 个元素)");
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "设置varArgs参数 " + varArgsField.field().getName() + " 失败: " + e.getMessage(), e);
                }
            }
        }

        CmdParamValidator.validateRequiredFields(request, fields, explicitlySet);
        CmdParamValidator.validateMutexConstraints(fields, explicitlySet); // 互斥/依赖参数验证
    }

    /**
     *  初始化所有字段的默认值（包括 required=false 的可选字段）
     * <p>
     * 这确保了即使命令行没有提供某个可选参数，它也会有正确的默认值
     * 而不是保持 Java 的零值（null, 0, false 等）
     */
    private static void initializeDefaultValues(CommandRequest<?> request, List<CmdParamProcessor.FieldInfo> fields) {
        for (CmdParamProcessor.FieldInfo fi : fields) {
            String defaultVal = fi.param().defaultValue();
            if (defaultVal == null || defaultVal.isEmpty()) continue;

            try {
                fi.field().setAccessible(true);
                Object currentValue = fi.field().get(request);

                // 当前值为 null / 空串 / 原始类型零值时，视为未设置，套用默认值。
                // 注意：原始类型（int/boolean 等）字段默认不是 null，若不判零值，
                // 声明了 defaultValue 的可选原始类型参数将永远拿不到默认值。
                // 此处套用的默认值会被后续参数解析覆盖，因此顺序安全。
                boolean shouldInit = currentValue == null
                        || (currentValue instanceof String && ((String) currentValue).isEmpty())
                        || CmdParamValidator.isPrimitiveDefaultValue(currentValue);

                if (shouldInit) {
                    Object converted = CmdValueConverter.convertValue(defaultVal, fi.field().getType());
                    fi.field().set(request, converted);
                    logger.debug("初始化默认值: " + fi.param().name() + " = " + defaultVal +
                               " (type=" + fi.field().getType().getSimpleName() + ")");
                }
            } catch (Exception e) {
                logger.warn("无法初始化默认值 " + fi.param().name() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 判断一个 token 是否是以负号开头的数值字面量（如 -1、-3.14、-.5、-1e10、-0x1A）。
     *
     * <p>实现会先去掉首个字符再解析，因此仅对以 '-' 开头的字符串有意义。
     * 调用方必须先确认 {@code str.startsWith("-")}，否则正数（如 "123"）也会返回 true。</p>
     */
    private static boolean looksLikeNegativeNumber(String str) {
        if (str == null || str.length() < 2) return false;

        String content = str.substring(1); // 去掉开头的 "-"

        // 支持十六进制
        if (content.startsWith("0x") || content.startsWith("0X")) {
            try {
                Long.parseLong(content.substring(2), 16);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 支持十进制/浮点数/科学计数法
        try {
            Double.parseDouble(content);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     *  判断是否为 negated 格式的布尔标志
     * 支持格式:
     *   --no-verbose → true (表示设为false)
     *   --noverbose  → true (紧凑格式)
     *
     * @param param 命令行参数（如 "--no-verbose"）
     * @return 如果是 negated 格式返回 true，否则返回 false
     */
    private static boolean isNegatedFlag(CmdParam param) {
        return param.isNegated();
    }

    private static Map<String, CmdParamProcessor.FieldInfo> buildParamIndex(List<CmdParamProcessor.FieldInfo> fields) {
        Map<String, CmdParamProcessor.FieldInfo> index = new HashMap<>();
        for (CmdParamProcessor.FieldInfo fi : fields) {
            index.put(fi.param().name().toLowerCase(), fi);
            for (String alias : fi.param().aliases()) {
                index.put(alias.toLowerCase(), fi);
                // 支持无前缀别名, get, set 等
                if (!alias.startsWith("-")) {
                    index.put(alias, fi);
                }
            }
        }
        return index;
    }

    private static CmdParamProcessor.FieldInfo findMatchingParam(String arg, Map<String, CmdParamProcessor.FieldInfo> index) {
        String normalized = arg.toLowerCase();
        return index.get(normalized);
    }

    private static int setFieldValue(CommandRequest<?> request, CmdParamProcessor.FieldInfo fieldInfo, String[] args, int currentIndex,
                                       Set<String> explicitlySet) {
        Field field = fieldInfo.field();
        CmdParam param = fieldInfo.param();

        try {
            field.setAccessible(true);

            // 处理 --key=value 格式（内联值）
            String currentArg = args[currentIndex];
            boolean isFlagField = field.getType() == boolean.class || field.getType() == Boolean.class;
            if (currentArg.contains("=")) {
                String inlineValue = currentArg.substring(currentArg.indexOf('=') + 1);

                if (isFlagField) {
                    // --key=true/false 格式的布尔值
                    boolean valueToSet = Boolean.parseBoolean(inlineValue);
                    if (isNegatedFlag(param)) {
                        valueToSet = !valueToSet;
                    }
                    field.set(request, valueToSet);
                    invokeSetterIfPresent(request, field, valueToSet);
                    explicitlySet.add(field.getName());
                } else {
                    // 普通类型的内联值
                    Object value = CmdValueConverter.convertValue(inlineValue, field.getType(), param.readMode());
                    CmdParamValidator.validateFieldValue(param, field.getName(), value);
                    field.set(request, value);
                    invokeSetterIfPresent(request, field, value);
                    explicitlySet.add(field.getName());
                }
                return currentIndex + 1;
            }

            if (param.varArgs()) {
                // varArgs 的关键字形式（如 --ids 1 2 3）：贪婪收集后续「非选项」token。
                // 终止条件：遇到形如选项的 token（以 '-' 开头且不是负数）。
                // 注意：此前用 !looksLikeNumeric 判断「非数值」，而该函数假设入参以 '-' 开头
                // 并会去掉首字符再解析，导致正数（如 "123"）被误判为数值而提前终止收集。
                List<Object> values = new ArrayList<>();
                int i = currentIndex + 1;
                while (i < args.length && (!args[i].startsWith("-") || looksLikeNegativeNumber(args[i]))) {
                    values.add(CmdValueConverter.convertValue(args[i], field.getType(), param.readMode()));
                    i++;
                }
                field.set(request, values);
                invokeSetterIfPresent(request, field, values);
                explicitlySet.add(field.getName());
                return i;
            } else if (isFlagField) {
                // 支持 negated 模式：--no-xxx → false, --xxx → true
                boolean valueToSet = !isNegatedFlag(param);
                field.set(request, valueToSet);
                invokeSetterIfPresent(request, field, valueToSet);
                explicitlySet.add(field.getName());
                logger.debug(" Boolean参数 " + field.getName() + " = " + valueToSet +
                           (valueToSet ? "" : " [negated]"));
                return currentIndex + 1;
            } else if (currentIndex + 1 < args.length
                    && (!args[currentIndex + 1].startsWith("-") || looksLikeNegativeNumber(args[currentIndex + 1]))) {
                // 非布尔类型且需要值：消费下一个参数（负数也视为合法值，如 --count -5）
                String valueStr = args[currentIndex + 1];
                Object value = CmdValueConverter.convertValue(valueStr, field.getType(), param.readMode());
                CmdParamValidator.validateFieldValue(param, field.getName(), value);
                field.set(request, value);
                invokeSetterIfPresent(request, field, value);
                explicitlySet.add(field.getName());
                return currentIndex + 2;
            } else {
                throw new IllegalArgumentException("参数 " + param.name() + " 需要值");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("设置字段 " + field.getName() + " 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在 field.set() 之后尝试调用 setter 方法，触发副作用逻辑
     * <p>
     * 例如 ClassInfoRequest.setShowConstructors(false) 会联动设置 showAll = false，
     * 如果只用反射 set 字段则跳过了这个逻辑。
     */
    private static void invokeSetterIfPresent(Object target, Field field, Object value) {
        try {
            String fieldName = field.getName();
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            java.lang.reflect.Method setter = target.getClass().getMethod(setterName, field.getType());
            setter.invoke(target, value);
            logger.debug("  [setter] 调用 " + setterName + "(" + value + ")");
        } catch (NoSuchMethodException ignored) {
            // 没有 setter，正常（大多数 Request 类没有自定义 setter）
        } catch (Exception e) {
            logger.warn("  [setter] 调用 setter 失败 (非致命): " + e.getMessage());
        }
    }

    /**
     *  处理操作符（isOperator）及其参数消费
     * 支持标准格式（--get, --set）和 Git 风格（get, set）
     *  支持分离存储模式：自动将消费的参数分配到 operatorIndex 对应的字段
     *
     * @param request 请求对象
     * @param allFields 所有字段的 FieldInfo 列表（用于查找同组子字段）
     * @param operatorInfo 操作符的 FieldInfo
     * @param args 完整参数数组
     * @param currentIndex 当前索引
     * @return 消费后的下一个索引
     */
    private static int handleOperator(CommandRequest<?> request, List<CmdParamProcessor.FieldInfo> allFields,
                                       CmdParamProcessor.FieldInfo operatorInfo,
                                       String[] args, int currentIndex,
                                       Set<String> explicitlySet) {
        Field field = operatorInfo.field();
        CmdParam param = operatorInfo.param();
        String operatorGroup = param.belongsToOperator();
        int argsToConsume = param.operatorArgs();

        try {
            field.setAccessible(true);

            // ========== Step 1: 设置操作符标志 (index=0) ==========
            if (argsToConsume == 0) {
                // 纯标志操作符（如 --verbose）：设置为 true
                field.set(request, true);
                explicitlySet.add(field.getName());
                recordOperator(request, param);
                return currentIndex + 1;
            }

            // 对于有参数的操作符：设置标志为 true（分离模式）
            field.set(request, true);
            explicitlySet.add(field.getName());

            // ========== Step 2: 检查参数数量 ==========
            if (currentIndex + argsToConsume >= args.length) {
                throw new IllegalArgumentException(
                    "操作符 " + param.name() + " 需要 " + argsToConsume + " 个参数");
            }

            // ========== Step 3: 分离模式 - 查找同组子字段 ==========
            Map<Integer, CmdParamProcessor.FieldInfo> subFieldsMap = findSubFieldsForOperator(allFields, operatorGroup);

            if (!subFieldsMap.isEmpty()) {
                // 分离模式：按 operatorIndex 分配参数到独立字段
                for (int j = 1; j <= argsToConsume; j++) {
                    CmdParamProcessor.FieldInfo subFieldInfo = subFieldsMap.get(j);
                    String valueStr = args[currentIndex + j];

                    if (subFieldInfo != null) {
                        // 找到了对应的子字段 → 直接赋值
                        Field subField = subFieldInfo.field();
                        CmdParam subParam = subFieldInfo.param();
                        subField.setAccessible(true);
                        Object value = CmdValueConverter.convertValue(valueStr, subField.getType(), subParam.readMode());
                        CmdParamValidator.validateFieldValue(subParam, subField.getName(), value);
                        subField.set(request, value);
                        invokeSetterIfPresent(request, subField, value);
                        explicitlySet.add(subField.getName());
                        logger.debug(" 操作符[" + param.name() + "] 子参数[" + j + "] " +
                                   subField.getName() + " = " + valueStr);
                    } else {
                        // 没找到对应的子字段 → 记录警告，跳过
                        logger.warn("操作符 " + param.name() + " 的第" + j +
                                  "个参数没有对应的字段定义 (operatorIndex=" + j + ")");
                    }
                }
            } else {
                // 兼容旧聚合模式：如果没有子字段定义，回退到将所有参数存入操作符字段本身
                if (argsToConsume == 1) {
                    String valueStr = args[currentIndex + 1];
                    Object value = CmdValueConverter.convertValue(valueStr, field.getType(), param.readMode());
                    CmdParamValidator.validateFieldValue(param, field.getName(), value);
                    field.set(request, value);  // 覆盖之前的 true
                    invokeSetterIfPresent(request, field, value);
                    explicitlySet.add(field.getName());
                    logger.debug(" 操作符[" + param.name() + "] (聚合模式) " +
                               field.getName() + " = " + valueStr);
                } else {
                    StringBuilder consumed = new StringBuilder();
                    for (int j = 1; j <= argsToConsume; j++) {
                        if (j > 1) consumed.append(",");
                        consumed.append(args[currentIndex + j]);
                    }
                    field.set(request, consumed.toString());  // 覆盖之前的 true
                    explicitlySet.add(field.getName());
                    logger.debug(" 操作符[" + param.name() + "] (聚合模式) " +
                               field.getName() + " = " + consumed);
                }
            }

            // ========== Step 4: 记录操作符 ==========
            recordOperator(request, param);

            return currentIndex + 1 + argsToConsume;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "处理操作符 " + param.name() + " 失败: " + e.getMessage(), e);
        }
    }

    /**
     *  查找属于指定操作符组的所有子字段（operatorIndex > 0）
     * @return Map<operatorIndex, FieldInfo>
     */
    private static Map<Integer, CmdParamProcessor.FieldInfo> findSubFieldsForOperator(List<CmdParamProcessor.FieldInfo> allFields, String operatorGroup) {
        Map<Integer, CmdParamProcessor.FieldInfo> result = new HashMap<>();

        if (operatorGroup == null || operatorGroup.isEmpty()) {
            return result;
        }

        for (CmdParamProcessor.FieldInfo fi : allFields) {
            CmdParam p = fi.param();
            if (operatorGroup.equals(p.belongsToOperator()) && p.operatorIndex() > 0) {
                result.put(p.operatorIndex(), fi);
            }
        }

        return result;
    }

    /**
     *  记录操作符到 CommandRequest 基类的追踪列表
     * 支持 belongsToOperator 属性和自动推断
     */
    private static void recordOperator(CommandRequest<?> request, CmdParam param) {
        String operatorName = param.belongsToOperator();
        
        if (operatorName == null || operatorName.isEmpty()) {
            // 如果没有显式指定归属，使用参数名本身
            operatorName = param.name().replaceAll("^-+", "");
        }
        
        request.addReceivedOperator(operatorName);
        logger.debug("已记录操作符: " + operatorName + " (来源: " + param.name() + ")");
    }
}
