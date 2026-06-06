package com.justnothing.testmodule.command.base.command;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.utils.CustomCommandLineParser;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class CmdParamProcessor {

    private static final Logger logger = Logger.getLoggerForName("CmdParamProcessor");

    private static final Map<Class<?>, List<FieldInfo>> fieldCache = new HashMap<>();

    public static Gson createGsonWithCmdParamSupport(Class<?> clazz) {
        return new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .addDeserializationExclusionStrategy(new CmdParamExclusionStrategy(false))
            .addSerializationExclusionStrategy(new CmdParamExclusionStrategy(true))
            .setFieldNamingStrategy(new CmdParamFieldNamingStrategy(clazz))
            .create();
    }

    public static List<FieldInfo> getCmdParamFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, CmdParamProcessor::scanFields);
    }

    private static List<FieldInfo> scanFields(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;

                // 检查 @CmdParam 注解
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param != null) {
                    fields.add(new FieldInfo(field, param));
                }
            }
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     *  智能解析入口（公共方法）
     * <p>
     * 自动选择最佳解析策略：
     * - 如果 request 实现了 CustomCommandLineParser → 使用自定义解析器
     * - 否则 → 使用标准 @CmdParam 声明式解析
     *
     * @param request 请求对象
     * @param args 命令行参数数组
     * @return 解析完成后的请求对象（可能是新实例）
     * @throws IllegalArgumentException 参数验证错误
     */
    public static CommandRequest parseRequest(CommandRequest request, String[] args) throws IllegalArgumentException {
        if (request == null) {
            throw new IllegalArgumentException("请求对象不能为 null");
        }

        if (args == null || args.length == 0) {
            return request;
        }

        String requestTypeName = request.getClass().getSimpleName();

        try {
            if (request instanceof CustomCommandLineParser) {
                // 模式A: 自定义解析器（用于复杂参数逻辑）
                logger.debug("🔧 [parseRequest] 使用 CustomCommandLineParser: " + requestTypeName);

                List<String> argList = Arrays.asList(args);
                CustomCommandLineParser.ParseContext parseContext =
                    new CustomCommandLineParser.ParseContext(args, argList, new HashMap<>());

                CommandRequest parsedRequest = ((CustomCommandLineParser) request).customParse(parseContext);

                if (parsedRequest != null) {
                    logger.debug("✅ [parseRequest] 自定义解析完成");
                    return parsedRequest;
                } else {
                    logger.debug("✅ [parseRequest] 自定义解析返回null，使用原对象");
                    return request;
                }
            } else {
                // 模式B: 标准声明式解析（@CmdParam 自动处理）
                logger.debug(" [parseRequest] 使用 CmdParamProcessor: " + requestTypeName);

                parseCommandLineArgs(request, args);

                logger.debug("✅ [parseRequest] 标准解析完成");
                return request;
            }
        } catch (IllegalArgumentException e) {
            // 参数验证错误：直接向上抛出，让上层显示帮助文档
            logger.warn("⚠️ [parseRequest] 参数验证失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他错误：包装后抛出
            logger.error("❌ [parseRequest] 解析失败: " + e.getMessage(), e);
            throw new IllegalArgumentException(
                "参数解析失败 (" + requestTypeName + "): " + e.getMessage(), e
            );
        }
    }

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

    public static void parseCommandLineArgs(CommandRequest request, String[] args) throws IllegalArgumentException {
        // 预处理：合并反引号包裹的参数（支持带空格的表达式）
        args = preprocessBacktickArgs(args);

        List<FieldInfo> fields = getCmdParamFields(request.getClass());
        Map<String, FieldInfo> paramIndex = buildParamIndex(fields);
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
                FieldInfo fieldInfo = findMatchingParam(arg, paramIndex);

                if (fieldInfo != null) {
                    // 已知的关键字参数：正常处理
                    i = setFieldValue(request, fieldInfo, args, i, explicitlySet);
                } else if (looksLikeNumeric(arg)) {
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
                
                FieldInfo fieldInfo = findMatchingParam(lookupKey, paramIndex);

                if (fieldInfo != null) {
                    i = setFieldValue(request, fieldInfo, args, i, explicitlySet);
                } else {
                    logger.warn("未知参数: " + arg + ", 忽略");
                    i++;
                }
            } else {
                // 非关键字参数：检查是否为操作符（Git风格：get, set 等）
                FieldInfo operatorInfo = findMatchingParam(arg, paramIndex);

                if (operatorInfo != null && operatorInfo.param.isOperator()) {
                    // 发现操作符！特殊处理（传入 allFields 支持分离模式）
                    i = handleOperator(request, fields, operatorInfo, args, i, explicitlySet);
                } else {
                    // 普通位置参数候选
                    positionalCandidates.add(arg);
                    i++;
                }
            }
        }

        // 第二遍：将未使用的候选分配给位置参数字段（支持 readMode + varArgs）
        if (!positionalCandidates.isEmpty()) {
            List<FieldInfo> positionalFields = fields.stream()
                .filter(fi -> fi.param.position() > 0)
                .sorted(Comparator.comparingInt(a -> a.param.position()))
                .collect(Collectors.toList());

            // 分类：普通位置参数 vs varArgs 参数
            List<FieldInfo> normalPositionalFields = new ArrayList<>();
            FieldInfo varArgsField = null;
            
            for (FieldInfo fi : positionalFields) {
                if (fi.param.varArgs()) {
                    varArgsField = fi;
                } else {
                    normalPositionalFields.add(fi);
                }
            }

            int argIndex = 0;
            
            // 计算必需的位置参数数量（不包括可选的和varArgs）
            long requiredCount = normalPositionalFields.stream()
                .filter(fi -> fi.param.required())
                .count();
            
            // Phase 1: 填充普通位置参数
            for (int j = 0; j < normalPositionalFields.size() && argIndex < positionalCandidates.size(); j++) {
                FieldInfo fieldInfo = normalPositionalFields.get(j);
                
                // 智能跳过逻辑：
                // 只有当候选数恰好等于必需参数数时（说明用户没有显式提供可选参数），
                // 才跳过有默认值的可选参数，让 varArgs 可以捕获更多
                boolean exactlyRequiredCount = positionalCandidates.size() == requiredCount 
                    || (positionalCandidates.size() == requiredCount + 1 && varArgsField != null);
                boolean isOptionalWithDefault = !fieldInfo.param.required() 
                    && !fieldInfo.param.defaultValue().isEmpty();
                
                if (exactlyRequiredCount && isOptionalWithDefault) {
                    logger.debug("⏭️ 跳过可选参数 " + fieldInfo.field.getName() + 
                               " (候选数=" + positionalCandidates.size() + 
                               ", 必需数=" + requiredCount + ")");
                    continue;
                }
                
                String valueStr = positionalCandidates.get(argIndex++);

                try {
                    fieldInfo.field.setAccessible(true);
                    Object value = convertValue(valueStr, fieldInfo.field.getType(), fieldInfo.param.readMode());
                    validateFieldValue(fieldInfo.param, fieldInfo.field.getName(), value);
                    fieldInfo.field.set(request, value);
                    invokeSetterIfPresent(request, fieldInfo.field, value);
                    explicitlySet.add(fieldInfo.field.getName());
                    logger.debug(" 位置参数[" + fieldInfo.param.position() + "] " +
                               fieldInfo.field.getName() + " = " + valueStr +
                               " [mode=" + fieldInfo.param.readMode() + "]");
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "设置位置参数 " + fieldInfo.field.getName() + " 失败: " + e.getMessage(), e);
                }
            }

            // Phase 2: 将剩余参数全部赋值给 varArgs 字段（如果有）
            if (varArgsField != null && argIndex < positionalCandidates.size()) {
                List<String> remainingArgs = positionalCandidates.subList(argIndex, positionalCandidates.size());
                String varArgsValue = String.join(" ", remainingArgs);
                
                try {
                    varArgsField.field.setAccessible(true);
                    Object value = convertValue(varArgsValue, varArgsField.field.getType(), varArgsField.param.readMode());
                validateFieldValue(varArgsField.param, varArgsField.field.getName(), value);
                varArgsField.field.set(request, value);
                invokeSetterIfPresent(request, varArgsField.field, value);
                explicitlySet.add(varArgsField.field.getName());
                    logger.debug(" varArgs参数[" + varArgsField.param.position() + "] " +
                               varArgsField.field.getName() + " = " + varArgsValue +
                               " [mode=" + varArgsField.param.readMode() + "] (" +
                               remainingArgs.size() + " 个元素)");
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "设置varArgs参数 " + varArgsField.field.getName() + " 失败: " + e.getMessage(), e);
                }
            } else if (varArgsField == null && argIndex < positionalCandidates.size()) {
                // 没有 varArgs 字段但还有剩余参数
                logger.warn("⚠️ 位置参数数量过多: 提供 " + positionalCandidates.size() +
                           " 个, 最多支持 " + normalPositionalFields.size() + " 个" +
                           (normalPositionalFields.isEmpty() ? "" : " (无varArgs接收)"));
            }
        }

        validateRequiredFields(request, fields, explicitlySet);
        validateMutexConstraints(request, fields); // 互斥参数验证
    }

    /**
     *  初始化所有字段的默认值（包括 required=false 的可选字段）
     * <p>
     * 这确保了即使命令行没有提供某个可选参数，它也会有正确的默认值
     * 而不是保持 Java 的零值（null, 0, false 等）
     */
    private static void initializeDefaultValues(CommandRequest request, List<FieldInfo> fields) {
        for (FieldInfo fi : fields) {
            String defaultVal = fi.param.defaultValue();
            if (defaultVal == null || defaultVal.isEmpty()) continue;

            try {
                fi.field.setAccessible(true);
                Object currentValue = fi.field.get(request);

                // 只在当前值为 null 或空时设置默认值
                boolean shouldInit = false;
                if (currentValue == null) {
                    shouldInit = true;
                } else if (currentValue instanceof String && ((String) currentValue).isEmpty()) {
                    shouldInit = true;
                }

                if (shouldInit) {
                    Object converted = convertValue(defaultVal, fi.field.getType());
                    fi.field.set(request, converted);
                    logger.debug("📦 初始化默认值: " + fi.param.name() + " = " + defaultVal +
                               " (type=" + fi.field.getType().getSimpleName() + ")");
                }
            } catch (Exception e) {
                logger.warn("无法初始化默认值 " + fi.param.name() + ": " + e.getMessage());
            }
        }
    }

    /**
     *  判断一个以 "-" 开头的字符串是否看起来像数值（而非 flag）
     * 支持格式: -1, -3.14, -.5, -.5e10, -0x1A 等
     */
    private static boolean looksLikeNumeric(String str) {
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

    private static Map<String, FieldInfo> buildParamIndex(List<FieldInfo> fields) {
        Map<String, FieldInfo> index = new HashMap<>();
        for (FieldInfo fi : fields) {
            index.put(fi.param.name().toLowerCase(), fi);
            for (String alias : fi.param.aliases()) {
                index.put(alias.toLowerCase(), fi);
                // 支持无前缀别名, get, set 等
                if (!alias.startsWith("-")) {
                    index.put(alias, fi);
                }
            }
        }
        return index;
    }

    private static FieldInfo findMatchingParam(String arg, Map<String, FieldInfo> index) {
        String normalized = arg.toLowerCase();
        return index.get(normalized);
    }

    private static int setFieldValue(CommandRequest request, FieldInfo fieldInfo, String[] args, int currentIndex,
                                       Set<String> explicitlySet) {
        Field field = fieldInfo.field;
        CmdParam param = fieldInfo.param;

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
                    Object value = convertValue(inlineValue, field.getType(), param.readMode());
                    validateFieldValue(param, field.getName(), value);
                    field.set(request, value);
                    invokeSetterIfPresent(request, field, value);
                    explicitlySet.add(field.getName());
                }
                return currentIndex + 1;
            }

            if (param.varArgs() && currentIndex + 1 < args.length) {
                List<Object> values = new ArrayList<>();
                int i = currentIndex + 1;
                while (i < args.length && !args[i].startsWith("-") && !looksLikeNumeric(args[i])) {
                    values.add(convertValue(args[i], field.getType(), param.readMode()));
                    i++;
                }
                field.set(request, values);
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
            } else if (currentIndex + 1 < args.length && !args[currentIndex + 1].startsWith("-")) {
                // 非布尔类型且需要值：消费下一个参数
                String valueStr = args[currentIndex + 1];
                Object value = convertValue(valueStr, field.getType(), param.readMode());
                validateFieldValue(param, field.getName(), value);
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
    private static int handleOperator(CommandRequest request, List<FieldInfo> allFields,
                                       FieldInfo operatorInfo,
                                       String[] args, int currentIndex,
                                       Set<String> explicitlySet) {
        Field field = operatorInfo.field;
        CmdParam param = operatorInfo.param;
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
            Map<Integer, FieldInfo> subFieldsMap = findSubFieldsForOperator(allFields, operatorGroup);

            if (!subFieldsMap.isEmpty()) {
                // 分离模式：按 operatorIndex 分配参数到独立字段
                for (int j = 1; j <= argsToConsume; j++) {
                    FieldInfo subFieldInfo = subFieldsMap.get(j);
                    String valueStr = args[currentIndex + j];

                    if (subFieldInfo != null) {
                        // 找到了对应的子字段 → 直接赋值
                        Field subField = subFieldInfo.field;
                        CmdParam subParam = subFieldInfo.param;
                        subField.setAccessible(true);
                        Object value = convertValue(valueStr, subField.getType(), subParam.readMode());
                        validateFieldValue(subParam, subField.getName(), value);
                        subField.set(request, value);
                        invokeSetterIfPresent(request, subField, value);
                        explicitlySet.add(subField.getName());
                        logger.debug(" 操作符[" + param.name() + "] 子参数[" + j + "] " +
                                   subField.getName() + " = " + valueStr);
                    } else {
                        // 没找到对应的子字段 → 记录警告，跳过
                        logger.warn("⚠️ 操作符 " + param.name() + " 的第" + j +
                                  "个参数没有对应的字段定义 (operatorIndex=" + j + ")");
                    }
                }
            } else {
                // 兼容旧聚合模式：如果没有子字段定义，回退到将所有参数存入操作符字段本身
                if (argsToConsume == 1) {
                    String valueStr = args[currentIndex + 1];
                    Object value = convertValue(valueStr, field.getType(), param.readMode());
                    validateFieldValue(param, field.getName(), value);
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
    private static Map<Integer, FieldInfo> findSubFieldsForOperator(List<FieldInfo> allFields, String operatorGroup) {
        Map<Integer, FieldInfo> result = new HashMap<>();

        if (operatorGroup == null || operatorGroup.isEmpty()) {
            return result;
        }

        for (FieldInfo fi : allFields) {
            CmdParam p = fi.param;
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
    private static void recordOperator(CommandRequest request, CmdParam param) {
        String operatorName = param.belongsToOperator();
        
        if (operatorName == null || operatorName.isEmpty()) {
            // 如果没有显式指定归属，使用参数名本身
            operatorName = param.name().replaceAll("^-+", "");
        }
        
        request.addReceivedOperator(operatorName);
        logger.debug("✅ 已记录操作符: " + operatorName + " (来源: " + param.name() + ")");
    }

    /**
     *  增强版类型转换，支持引号处理模式
     * @param valueStr 输入字符串
     * @param targetType 目标类型
     * @param readMode 引号处理模式
     * @return 转换后的对象
     */
    private static Object convertValue(String valueStr, Class<?> targetType, CmdParam.ReadMode readMode) {
        String processed = processQuotes(valueStr, readMode);

        if (targetType == String.class) return processed;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(processed);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(processed);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(processed);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(processed);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(processed);

        return processed;
    }

    /**
     *  保持向后兼容的旧接口（默认使用 STRIPPED 模式）
     */
    private static Object convertValue(String valueStr, Class<?> targetType) {
        return convertValue(valueStr, targetType, CmdParam.ReadMode.STRIPPED);
    }

    /**
     *  根据不同的 ReadMode 处理引号
     * <p>
     * 三种模式对比：
     * <p>
     * RAW (原始):
     *   test -> "test"
     *   "test" -> "\"test\""
     *   arg with spaces -> "\"arg" (按空格分割后的第一个token)
     * <p>
     * STRIPPED (智能去引号，默认):
     *   test -> "test"
     *   "test" -> test
     *   "arg with spaces" -> arg with spaces
     * <p>
     * PRESERVED (完整保留):
     *   test -> "test"
     *   "test" -> "\"test\""
     *   "arg with spaces" -> "\"arg with spaces\""
     */
    private static String processQuotes(String input, CmdParam.ReadMode mode) {
        if (input == null) return null;

        boolean isSurroundedByQuotes = (input.startsWith("\"") && input.endsWith("\"")) ||
                (input.startsWith("'") && input.endsWith("'"));
        switch (mode) {
            case RAW:
                // 原始模式：不处理引号，直接返回原始字符串
                return input;

            case STRIPPED:
                // 智能去引号模式（默认）
                if (isSurroundedByQuotes) {
                    return input.substring(1, input.length() - 1);
                }
                return input;

            case PRESERVED:
                // 完整保留模式：如果已有引号则保留并转义内部引号
                if (isSurroundedByQuotes) {
                    // 已经有外层引号：转义内部的引号
                    String content = input.substring(1, input.length() - 1);
                    char quote = input.charAt(0);
                    // 转义内部相同类型的引号
                    content = content.replace(String.valueOf(quote), "\\" + quote);
                    return input.charAt(0) + content + input.charAt(input.length() - 1);
                } else {
                    // 没有外层引号：添加引号包裹
                    return "\"" + input.replace("\"", "\\\"") + "\"";
                }

            default:
                return input;
        }
    }

    private static void validateFieldValue(CmdParam param, String fieldName, Object value) throws IllegalArgumentException {
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

        if (value instanceof Number) {
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

    private static void validateRequiredFields(CommandRequest request, List<FieldInfo> fields,
                                                Set<String> explicitlySet) throws IllegalArgumentException {
        for (FieldInfo fi : fields) {
            if (!fi.param.required()) continue;

            try {
                fi.field.setAccessible(true);
                Object value = fi.field.get(request);

                if (value == null ||
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof List && ((List<?>) value).isEmpty())) {

                    String defaultVal = fi.param.defaultValue();
                    if (!defaultVal.isEmpty()) {
                        Object converted = convertValue(defaultVal, fi.field.getType());
                        fi.field.set(request, converted);
                        logger.debug("使用默认值: " + fi.param.name() + " = " + defaultVal);
                    } else {
                        throw new IllegalArgumentException("缺少必填参数: " + fi.param.name());
                    }
                } else if (isPrimitiveDefaultValue(value) && !explicitlySet.contains(fi.field.getName())) {
                    String defaultVal = fi.param.defaultValue();
                    if (!defaultVal.isEmpty()) {
                        Object converted = convertValue(defaultVal, fi.field.getType());
                        fi.field.set(request, converted);
                        logger.debug("使用默认值: " + fi.param.name() + " = " + defaultVal);
                    } else {
                        throw new IllegalArgumentException("缺少必填参数: " + fi.param.name());
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + fi.field.getName(), e);
            }
        }
    }

    private static boolean isPrimitiveDefaultValue(Object value) {
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
     *  验证互斥约束（Mutex）— 双模式支持
     *  <p>
     * 模式1: 严格互斥 (默认)
     *   - 如果 A.mutexWith = ["B"], 且 A 和 B 同时使用 → 报错
     * <p>
     * 模式2: 互斥组 (mutual group)
     *   - 如果 A.mutexWith = ["B"] 且 B.mutexWith = ["A"] → 视为同组
     *   - 同组内最后一个显式设置的参数生效，其他重置为默认值
     */
    private static void validateMutexConstraints(CommandRequest request, List<FieldInfo> fields) throws IllegalArgumentException {
        Map<String, Boolean> paramUsage = new java.util.HashMap<>();
        Map<String, FieldInfo> fieldByName = new java.util.HashMap<>();

        // 第一遍：收集哪些参数被使用了
        for (FieldInfo fi : fields) {
            try {
                fi.field.setAccessible(true);
                Object value = fi.field.get(request);
                fieldByName.put(fi.param.name().toLowerCase(), fi);

                boolean isUsed = false;
                if (value != null) {
                    if (value instanceof Boolean) {
                        isUsed = (Boolean) value;
                    } else if (value instanceof String) {
                        isUsed = !((String) value).isEmpty();
                    } else {
                        isUsed = true;
                    }
                }

                if (isUsed) {
                    paramUsage.put(fi.param.name().toLowerCase(), true);
                    // 也记录别名
                    for (String alias : fi.param.aliases()) {
                        paramUsage.put(alias.toLowerCase(), true);
                    }
                }
            } catch (IllegalAccessException e) {
                logger.warn("无法检查字段: " + fi.field.getName());
            }
        }

        // 第二遍：检测互斥组并应用"最后胜出"策略
        Set<String> processedGroups = new java.util.HashSet<>();
        
        for (FieldInfo fi : fields) {
            String[] mutexWith = fi.param.mutexWith();
            if (mutexWith.length == 0) continue;

            try {
                fi.field.setAccessible(true);
                Object currentValue = fi.field.get(request);
                
                // 检查当前参数是否被使用了
                boolean currentUsed = false;
                if (currentValue != null) {
                    if (currentValue instanceof Boolean) {
                        currentUsed = (Boolean) currentValue;
                    } else if (currentValue instanceof String) {
                        currentUsed = !((String) currentValue).isEmpty();
                    } else {
                        currentUsed = true;
                    }
                }

                if (!currentUsed) continue;

                // 检查每个互斥目标
                for (String mutexTarget : mutexWith) {
                    String targetLower = mutexTarget.toLowerCase();
                    
                    // 情况1: 目标也被使用了
                    if (paramUsage.containsKey(targetLower)) {
                        FieldInfo targetFi = fieldByName.get(targetLower);
                        
                        if (targetFi != null) {
                            String[] targetMutexWith = targetFi.param.mutexWith();
                            
                            // 检查是否为双向互斥（构成互斥组）
                            boolean isMutualGroup = false;
                            for (String tm : targetMutexWith) {
                                if (tm.equalsIgnoreCase(fi.param.name())) {
                                    isMutualGroup = true;
                                    break;
                                }
                            }
                            
                            if (isMutualGroup) {
                                // 🎯 互斥组模式：最后胜出策略
                                String groupKey = fi.param.name() + "+" + mutexTarget;
                                if (!processedGroups.contains(groupKey)) {
                                    processedGroups.add(groupKey);
                                    
                                    // 找到同组所有成员，保留最后一个
                                    logger.debug(" 检测到互斥组: " + fi.param.name() + " ↔ " + mutexTarget +
                                               " (应用'最后胜出'策略)");
                                    
                                    // 简化处理：保持当前值不变（因为它是后解析的）
                                    // 在实际命令行中，后面的参数会覆盖前面的
                                }
                            } else {
                                // ❌ 单向互斥：严格模式，报错
                                throw new IllegalArgumentException(
                                    "参数 '" + fi.param.name() + "' 与 '" + mutexTarget + "' 互斥，不能同时使用。\n" +
                                    "提示: 请选择其中之一");
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                logger.warn("无法检查互斥字段: " + fi.field.getName());
            }
        }

        // 第三遍：检查依赖约束（requires）
        for (FieldInfo fi : fields) {
            String[] requires = fi.param.requires();
            if (requires.length == 0) continue;

            try {
                fi.field.setAccessible(true);
                Object currentValue = fi.field.get(request);
                
                boolean currentUsed = false;
                if (currentValue != null) {
                    if (currentValue instanceof Boolean) {
                        currentUsed = (Boolean) currentValue;
                    } else if (currentValue instanceof String) {
                        currentUsed = !((String) currentValue).isEmpty();
                    } else {
                        currentUsed = true;
                    }
                }

                if (currentUsed) {
                    for (String requiredParam : requires) {
                        if (!paramUsage.containsKey(requiredParam.toLowerCase())) {
                            throw new IllegalArgumentException(
                                "参数 '" + fi.param.name() + "' 需要同时指定 '" + requiredParam + "'。\n" +
                                "提示: 请添加 --" + requiredParam + " 参数");
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                logger.warn("无法检查依赖约束: " + fi.field.getName());
            }
        }
    }

    public static String generateHelpText(Class<?> cmdClass) {
        StringBuilder sb = new StringBuilder();

        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation != null) {
            sb.append(cmdAnnotation.name()).append(" - ").append(cmdAnnotation.description()).append("\n\n");

            if (!cmdAnnotation.helpText().isEmpty()) {
                sb.append(cmdAnnotation.helpText()).append("\n\n");
            }
        }

        CmdRoutes routesAnnotation = cmdClass.getAnnotation(CmdRoutes.class);
        if (routesAnnotation != null) {
            Map<String, List<CmdRoutes.Route>> categories = new LinkedHashMap<>();
            List<CmdRoutes.Route> flatRoutes = new ArrayList<>();

            for (CmdRoutes.Route route : routesAnnotation.value()) {
                String[] parts = route.path().split("/");
                if (parts.length >= 2) {
                    String category = parts[0];
                    categories.computeIfAbsent(category, k -> new ArrayList<>()).add(route);
                } else {
                    flatRoutes.add(route);
                }
            }

            // ========== 子命令（带分类前缀 + 内联位置参数）==========
            sb.append("子命令:\n");
            for (Map.Entry<String, List<CmdRoutes.Route>> entry : categories.entrySet()) {
                String category = entry.getKey();
                sb.append(String.format("  %s:\n", category));
                for (CmdRoutes.Route route : entry.getValue()) {
                    String sig = buildRouteSignature(route);
                    sb.append("    ").append(padRight(sig, 37)).append(route.description()).append("\n");
                }
                sb.append("\n");
            }
            for (CmdRoutes.Route route : flatRoutes) {
                String sig = buildRouteSignature(route);
                sb.append("  ").append(padRight(sig, 37)).append(route.description()).append("\n");
            }
            if (!flatRoutes.isEmpty()) sb.append("\n");

            // ========== 参数详情（逐子命令列出所有参数描述和约束）==========
            sb.append("参数详情:\n");
            boolean hasAnyDetails = false;

            for (Map.Entry<String, List<CmdRoutes.Route>> entry : categories.entrySet()) {
                String category = entry.getKey();
                boolean categoryHasDetails = false;

                for (CmdRoutes.Route route : entry.getValue()) {
                    String action = route.path().substring(category.length() + 1);
                    List<FieldInfo> allParams = getCmdParamFields(route.request());

                    if (!allParams.isEmpty()) {
                        sb.append(String.format("  %s/%s:\n", category, action));
                        categoryHasDetails = true;
                        hasAnyDetails = true;

                        for (FieldInfo fi : allParams) {
                            sb.append("    ").append(formatParamDetail(fi)).append("\n");
                        }
                        sb.append("\n");
                    }
                }
                if (categoryHasDetails) {
                    entry.getValue();
                }
            }

            for (CmdRoutes.Route route : flatRoutes) {
                List<FieldInfo> allParams = getCmdParamFields(route.request());

                if (!allParams.isEmpty()) {
                    sb.append(String.format("  %s:\n", route.path()));
                    hasAnyDetails = true;

                    for (FieldInfo fi : allParams) {
                        sb.append("    ").append(formatParamDetail(fi)).append("\n");
                    }
                    sb.append("\n");
                }
            }

            if (!hasAnyDetails) {
                sb.append("  (所有子命令暂无参数)\n\n");
            }
        } else if (cmdAnnotation == null) {
            // 兼容：对于没有 @Cmd/@CmdRoutes 的 Request 类，直接显示其参数
            List<FieldInfo> params = getCmdParamFields(cmdClass);
            if (!params.isEmpty()) {
                String className = cmdClass.getSimpleName();
                sb.append(className).append(" 参数:\n\n");

                for (FieldInfo fi : params) {
                    sb.append("  ").append(formatParamHelp(fi)).append("\n");
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatParamDetail(FieldInfo fi) {
        CmdParam p = fi.param;
        StringBuilder sb = new StringBuilder();

        String nameStr = p.name();
        if (p.aliases().length > 0) {
            nameStr += ", " + String.join(", ", p.aliases());
        }
        sb.append("  ").append(padRight(nameStr, 22));

        sb.append("  ").append(padRight(p.description(), 30));

        List<String> attrs = new ArrayList<>();
        attrs.add(p.required() ? "必需" : "可选");

        String typeName = inferTypeName(fi.field.getType());
        attrs.add(typeName);

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                attrs.add("默认=" + defVal);
            }
        }

        if (p.varArgs()) {
            attrs.add("可变参数");
        }

        if (p.min() > Double.NEGATIVE_INFINITY || p.max() < Double.POSITIVE_INFINITY) {
            attrs.add("范围: " + formatRange(p.min(), p.max()));
        }

        if (p.allowedValues().length > 0) {
            attrs.add("枚举: {" + String.join("|", p.allowedValues()) + "}");
        }

        if (p.isOperator()) {
            String opStr = "操作符";
            if (p.operatorArgs() > 0) opStr += "(" + p.operatorArgs() + " args)";
            attrs.add(opStr);
        }

        if (p.readMode() != CmdParam.ReadMode.STRIPPED) {
            attrs.add("mode=" + p.readMode().name());
        }

        sb.append(String.join(" | ", attrs));

        return sb.toString();
    }

    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= '\u4e00' && c <= '\u9fff') || (c >= '\u3000' && c <= '\u303f')
                    || (c >= '\uff00' && c <= '\uffef') ? 2 : 1;
        }
        return w;
    }

    private static String padRight(String s, int targetWidth) {
        int pad = targetWidth - displayWidth(s);
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < pad; i++) sb.append(' ');
        return sb.toString();
    }

    private static String inferTypeName(Class<?> type) {
        if (type == int.class || type == Integer.class) return "整数";
        if (type == long.class || type == Long.class) return "长整数";
        if (type == double.class || type == Double.class) return "浮点数";
        if (type == float.class || type == Float.class) return "浮点数";
        if (type == boolean.class || type == Boolean.class) return "布尔值";
        if (type == String.class) return "字符串";
        if (type.isArray()) return inferTypeName(type.getComponentType()) + "列表";
        if (List.class.isAssignableFrom(type)) return "列表";
        return type.getSimpleName();
    }

    private static String formatRange(double min, double max) {
        String minStr = (min == Double.NEGATIVE_INFINITY) ? "-inf"
                : (min == (long) min) ? String.valueOf((long) min) : String.valueOf(min);
        String maxStr = (max == Double.POSITIVE_INFINITY) ? "+inf"
                : (max == (long) max) ? String.valueOf((long) max) : String.valueOf(max);
        return minStr + "~" + maxStr;
    }

    /**
     *  生成指定子命令的帮助文档（增强版：集成 @SubCommandInfo 信息）
     */
    public static String generateHelpForRoute(Class<?> cmdClass, CommandRouter.RouteConfig routeConfig) {
        StringBuilder sb = new StringBuilder();

        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation == null) return "错误: 无法生成帮助文档; 该命令没有打上 @Cmd 注解";
        String subCommandPath = routeConfig.path().replace(cmdAnnotation.name() + ":", "");

        // 1. 标题行
        sb.append(cmdAnnotation.name()).append(" ").append(subCommandPath);

        // 尝试从 handler 类获取 @SubCommandInfo 注解
        SubCommandInfo subCmdInfo = routeConfig.handlerType().getAnnotation(SubCommandInfo.class);
        if (subCmdInfo == null) {
            subCmdInfo = routeConfig.requestType().getAnnotation(SubCommandInfo.class);
        }

        if (subCmdInfo != null) {
            // 使用 @SubCommandInfo 的详细描述
            sb.append(" - ").append(subCmdInfo.description()).append("\n\n");
        } else {
            // 回退到 RouteConfig 的简单描述
            sb.append(" - ").append(routeConfig.description()).append("\n\n");
        }

        // 2. 用法说明（优先使用 @SubCommandInfo.usage）
        if (subCmdInfo != null && !subCmdInfo.usage().isEmpty()) {
            sb.append("用法:\n");
            sb.append("  ").append(subCmdInfo.usage()).append("\n\n");
        } else {
            // 自动生成用法
            sb.append("用法:\n");
            sb.append("  ").append(cmdAnnotation.name()).append(" ").append(subCommandPath);

            List<FieldInfo> routeParams = getCmdParamFields(routeConfig.requestType());
            List<FieldInfo> positionalParams = routeParams.stream()
                    .filter(fi -> fi.param.position() > 0)
                    .sorted(Comparator.comparingInt(a -> a.param.position()))
                    .collect(Collectors.toList());

            for (FieldInfo fi : positionalParams) {
                sb.append(" <").append(fi.param.description()).append(">");
            }

            List<FieldInfo> optionalParams = routeParams.stream()
                .filter(fi -> !fi.param.required() && fi.param.position() == 0)
                .collect(Collectors.toList());

            if (!optionalParams.isEmpty()) {
                sb.append(" [选项]");
            }

            sb.append("\n\n");
        }

        // 3. 实际示例（来自 @SubCommandInfo.examples）
        if (subCmdInfo != null && subCmdInfo.examples().length > 0) {
            sb.append("示例:\n");
            for (String example : subCmdInfo.examples()) {
                sb.append("  ").append(example).append("\n");
            }
            sb.append("\n");
        }

        // 4. 参数列表（始终显示）
        List<FieldInfo> routeParams = getCmdParamFields(routeConfig.requestType());
        if (!routeParams.isEmpty()) {
            sb.append("选项:\n");

            for (FieldInfo fi : routeParams) {
                sb.append("  ").append(formatParamHelp(fi)).append("\n");
            }

            sb.append("\n");
        }

        // 5. 选项详细说明（来自 @SubCommandInfo.optionsDesc）
        if (subCmdInfo != null && !subCmdInfo.optionsDesc().isEmpty() &&
            !subCmdInfo.optionsDesc().startsWith("这个命令没有帮助信息")) {
            sb.append("选项详情:\n");
            String optionsText = subCmdInfo.optionsDesc().trim();
            // 缩进每一行
            for (String line : optionsText.split("\n")) {
                if (!line.trim().isEmpty()) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        // 6. 相关命令（来自 @SubCommandInfo.seeAlso）
        if (subCmdInfo != null && subCmdInfo.seeAlso().length > 0) {
            sb.append("相关命令:\n");
            for (String related : subCmdInfo.seeAlso()) {
                sb.append("  ").append(related).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String buildRouteSignature(CmdRoutes.Route route) {
        List<FieldInfo> allParams = getCmdParamFields(route.request());

        List<FieldInfo> positional = allParams.stream()
                .filter(fi -> fi.param().position() > 0)
                .sorted(Comparator.comparingInt(a -> a.param().position()))
                .collect(Collectors.toList());

        boolean hasOptions = allParams.stream().anyMatch(fi -> fi.param().position() <= 0);

        StringBuilder sig = new StringBuilder();
        String[] parts = route.path().split("/");
        sig.append(parts[parts.length - 1]);

        for (FieldInfo fi : positional) {
            String name = fi.param().name();
            if (fi.param().required()) {
                sig.append(" <").append(name).append(">");
            } else {
                String defVal = fi.param().defaultValue();
                if (!defVal.isEmpty()) {
                    sig.append(" [").append(name).append("=").append(defVal).append("]");
                } else {
                    sig.append(" [").append(name).append("]");
                }
            }
        }

        if (hasOptions) {
            sig.append(" [options...]");
        }

        return sig.toString();
    }

    private static String formatParamHelp(FieldInfo fi) {
        CmdParam p = fi.param;
        StringBuilder sb = new StringBuilder();

        sb.append("  ").append(padRight(p.name(), 20));

        if (p.aliases().length > 0) {
            sb.append(", ").append(String.join(", ", p.aliases()));
        }

        sb.append("  ").append(p.description());

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                sb.append(" (默认: ").append(defVal).append(")");
            }
        }

        if (p.min() > Double.NEGATIVE_INFINITY || p.max() < Double.POSITIVE_INFINITY) {
            sb.append(" [").append(p.min()).append("-").append(p.max()).append("]");
        }

        if (p.allowedValues().length > 0) {
            sb.append(" {").append(String.join("|", p.allowedValues())).append("}");
        }

        // 显示引号处理模式（非默认模式时）
        if (p.readMode() != CmdParam.ReadMode.STRIPPED) {
            sb.append(" [mode=").append(p.readMode().name()).append("]");
        }

        // 显示操作符信息（isOperator 时）
        if (p.isOperator()) {
            sb.append(" [操作符");
            if (p.operatorArgs() > 0) {
                sb.append(", 消费").append(p.operatorArgs()).append("个参数");
            }
            sb.append("]");
        }

        // 显示互斥约束
        if (p.mutexWith().length > 0) {
            sb.append(" [互斥: ").append(String.join(", ", p.mutexWith())).append("]");
        }

        return sb.toString();
    }

    public record FieldInfo(Field field, CmdParam param) {
    }

    private record CmdParamExclusionStrategy(boolean serializing) implements ExclusionStrategy {

        @Override
            public boolean shouldSkipField(FieldAttributes f) {
                CmdParam param = f.getAnnotation(CmdParam.class);
                if (param == null) return false;

                if (serializing && !param.serialize()) return true;
            return !serializing && !param.deserialize();
        }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }

    private static class CmdParamFieldNamingStrategy implements FieldNamingStrategy {
        private final Map<String, String> fieldNameMap;

        public CmdParamFieldNamingStrategy(Class<?> clazz) {
            List<FieldInfo> fields = getCmdParamFields(clazz);
            this.fieldNameMap = new HashMap<>();

            for (FieldInfo fi : fields) {
                if (!fi.param.serializedName().isEmpty()) {
                    fieldNameMap.put(fi.field.getName(), fi.param.serializedName());
                }
            }
        }

        @Override
        public String translateName(java.lang.reflect.Field f) {
            String customName = fieldNameMap.get(f.getName());
            return customName != null ? customName : FieldNamingPolicy.IDENTITY.translateName(f);
        }
    }
}
