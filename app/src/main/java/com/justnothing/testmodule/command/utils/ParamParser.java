package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.util.*;

/**
 * ParamParser v2.1 — 声明式命令行参数解析器 (增强版)
 * <p>
 * 架构设计 (5-Phase Pipeline):
 * <p>
 *   Phase 1: 初始化默认值
 *     → 根据 @FlagParam.defaultValue / @PositionalParam.defaultValue 初始化字段
 * <p>
 *   Phase 2: 提取选项参数 (Flags + Keywords) [支持negated + --key value!]
 *     → 遍历args，识别 -x / --xxx / --key=value / --key value
 *     → 支持 negated 模式 (--no-xxx → 设为false)
 *     → ★ 新增: 支持空格格式的关键字参数 (--depth 10)
 *     → 记录显式设置的flags (用于互斥检测)
 *     → 从args中移除已处理的选项
 * <p>
 *   Phase 3: 解析位置参数
 *     → 按 @PositionalParam.order 排序
 *     → 支持标准位置参数 + varArgs
 *     → 必填检查 + 默认值回退
 * <p>
 *   Phase 4: 互斥组解析 [基于mutexId]
 *     → 基于 mutexId 检测冲突
 *     → 策略: 同组内最后一个显式设置的flag生效
 *     → 自动重置同组其他flags为默认值
     
 *   Phase 5: 自定义解析 [新增!]
 *     → 如果Request实现了CustomCommandLineParser接口
 *     → 调用customParse()处理复杂逻辑
 *     → 传递ParseContext (包含remainingArgs + parsedValues)
 */
public class ParamParser {

    private static final Logger logger = Logger.getLoggerForName("ParamParser.v2.1");

    public static <T extends CommandRequest> T parse(Class<T> requestClass, String[] args) throws IllegalCommandLineArgumentException {
        try {
            logger.debug("开始解析参数: " + Arrays.toString(args));
            
            T request = requestClass.getDeclaredConstructor().newInstance();
            List<String> remainingArgs = new ArrayList<>(Arrays.asList(args));
            Set<String> explicitlySetFlags = new HashSet<>();
            Map<String, Object> parsedValues = new LinkedHashMap<>();
            
            phase1_initializeDefaults(request, parsedValues);
            phase2_extractOptions(request, remainingArgs, explicitlySetFlags, parsedValues);
            phase3_parsePositionalParams(request, remainingArgs, parsedValues);
            phase4_resolveMutexGroups(request, explicitlySetFlags);
            phase5_customParse(request, args, remainingArgs, parsedValues);
            
            logger.debug("参数解析完成");
            return request;
            
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String detail = cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : e.getMessage();
            throw new IllegalCommandLineArgumentException("参数解析失败: " + e.getClass().getSimpleName() + " → " + detail);
        }
    }

    private static void phase1_initializeDefaults(CommandRequest request, Map<String, Object> parsedValues) throws Exception {
        for (Field field : request.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            
            FlagParam flagAnnotation = field.getAnnotation(FlagParam.class);
            if (flagAnnotation != null && field.getType() == boolean.class) {
                field.setBoolean(request, flagAnnotation.defaultValue());
                parsedValues.put(field.getName(), flagAnnotation.defaultValue());
            }
            
            PositionalParam posAnnotation = field.getAnnotation(PositionalParam.class);
            if (posAnnotation != null && !posAnnotation.defaultValue().isEmpty()) {
                setFieldValue(request, field, posAnnotation.defaultValue());
                parsedValues.put(field.getName(), posAnnotation.defaultValue());
            }
            
            KeywordParam kwAnnotation = field.getAnnotation(KeywordParam.class);
            if (kwAnnotation != null && !kwAnnotation.defaultValue().isEmpty()) {
                setFieldValue(request, field, kwAnnotation.defaultValue());
                parsedValues.put(field.getName(), kwAnnotation.defaultValue());
            }
        }
    }

    private static void phase2_extractOptions(CommandRequest request, List<String> args, 
                                              Set<String> explicitlySetFlags,
                                              Map<String, Object> parsedValues) throws Exception {
        
        Map<String, Field> flagFields = new HashMap<>();
        Map<String, Field> keywordFields = new HashMap<>();
        Map<Field, FlagParam> flagAnnotations = new HashMap<>();
        
        collectAnnotatedFields(request.getClass(), flagFields, keywordFields, flagAnnotations);
        
        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                
                if (key.contains("=")) {
                    // 格式1: --key=value
                    int eqIdx = key.indexOf('=');
                    String paramName = key.substring(0, eqIdx);
                    String paramValue = key.substring(eqIdx + 1);
                    
                    Field field = keywordFields.get(paramName);
                    if (field != null) {
                        setFieldValue(request, field, paramValue);
                        parsedValues.put(field.getName(), paramValue);
                        iterator.remove();
                    }
                } else {
                    // 格式2: --key (可能带值或flag)
                    Field flagField = flagFields.get(arg);
                    Field kwField = keywordFields.get(arg);
                    
                    if (flagField != null) {
                        // 标准flag处理
                        FlagParam annotation = flagAnnotations.get(flagField);
                        assert annotation != null;
                        boolean valueToSet = !annotation.negated();
                        flagField.setBoolean(request, valueToSet);
                        explicitlySetFlags.add(flagField.getName());
                        parsedValues.put(flagField.getName(), valueToSet);
                        iterator.remove();
                        
                    } else if (kwField != null) {
                        // ★ 新增: 关键字参数的 --key value 支持!
                        if (iterator.hasNext()) {
                            String nextArg = iterator.next();
                            
                            if (!nextArg.startsWith("-")) {
                                // 下一个参数不是选项，作为值消费
                                setFieldValue(request, kwField, nextArg);
                                parsedValues.put(kwField.getName(), nextArg);
                                iterator.remove();  // 移除被消费的值
                            } else {
                                // 下一个参数也是选项，说明这是无值的keyword标记
                                setFieldValue(request, kwField, "true");
                                parsedValues.put(kwField.getName(), "true");
                            }
                        } else {
                            // 没有后续参数，设为"true"
                            setFieldValue(request, kwField, "true");
                            parsedValues.put(kwField.getName(), "true");
                        }
                        iterator.remove();  // 移除--key本身
                        
                    } else {
                        logger.warn("未知的选项: " + arg);
                    }
                }
                
            } else if (arg.startsWith("-") && arg.length() == 2) {
                Field flagField = flagFields.get(arg);
                if (flagField != null) {
                    FlagParam annotation = flagAnnotations.get(flagField);
                    assert annotation != null;
                    boolean valueToSet = !annotation.negated();
                    flagField.setBoolean(request, valueToSet);
                    explicitlySetFlags.add(flagField.getName());
                    parsedValues.put(flagField.getName(), valueToSet);
                    iterator.remove();
                }
            }
        }
    }

    private static void phase3_parsePositionalParams(CommandRequest request, List<String> args,
                                                  Map<String, Object> parsedValues) {
        
        List<FieldPosition> positionalFields = new ArrayList<>();
        
        for (Field field : request.getClass().getDeclaredFields()) {
            PositionalParam posAnnotation = field.getAnnotation(PositionalParam.class);
            if (posAnnotation != null) {
                positionalFields.add(new FieldPosition(field, posAnnotation.order()));
            }
        }
        
        positionalFields.sort(Comparator.comparingInt(FieldPosition::order));
        
        for (FieldPosition fp : positionalFields) {
            PositionalParam annotation = fp.field.getAnnotation(PositionalParam.class);

            assert annotation != null;
            if (!args.isEmpty()) {
                if (annotation.varArgs()) {
                    String value = String.join(" ", args);
                    setFieldValue(request, fp.field, value);
                    parsedValues.put(fp.field.getName(), value);
                    args.clear();
                } else {
                    String value = args.remove(0);
                    setFieldValue(request, fp.field, value);
                    parsedValues.put(fp.field.getName(), value);
                }
            } else {
                if (annotation.required() && annotation.defaultValue().isEmpty()) {
                    throw new IllegalCommandLineArgumentException(
                        "缺少必需参数: " + annotation.name() + " (位置=" + annotation.order() + ")");
                } else if (!annotation.defaultValue().isEmpty()) {
                    setFieldValue(request, fp.field, annotation.defaultValue());
                    parsedValues.put(fp.field.getName(), annotation.defaultValue());
                }
            }
        }
    }

    private static void phase4_resolveMutexGroups(CommandRequest request, Set<String> explicitlySetFlags) 
            throws Exception {
        
        Map<String, Field> lastSetInGroup = new LinkedHashMap<>();
        
        for (String fieldName : explicitlySetFlags) {
            Field field = findField(request.getClass(), fieldName);
            if (field == null) continue;
            
            FlagParam fp = field.getAnnotation(FlagParam.class);
            if (fp == null || fp.mutexId().isEmpty()) continue;
            
            String mutexId = fp.mutexId();
            Field previous = lastSetInGroup.get(mutexId);
            
            if (previous != null) {
                FlagParam prevAnnotation = previous.getAnnotation(FlagParam.class);
                assert prevAnnotation != null;
                boolean resetValue = prevAnnotation.defaultValue();
                previous.setBoolean(request, resetValue);
            }
            
            lastSetInGroup.put(mutexId, field);
        }
    }

    /**
     * ★ Phase 5: 自定义解析 (预留接口, 暂未启用)
     * TODO: 解决CustomCommandLineParser import问题后启用
     */
//    @SuppressWarnings("unchecked")
    private static <T extends CommandRequest> void phase5_customParse(T request, 
            String[] originalArgs, List<String> remainingArgs, Map<String, Object> parsedValues) 
            throws Exception {
        // 暂时禁用，等待CustomCommandLineParser import问题解决
        // if (request instanceof CustomCommandLineParser customParser) {
        //     logger.debug("Phase 5: 调用自定义解析器");
        //     ParseContext ctx = new ParseContext(originalArgs, remainingArgs, parsedValues);
        //     CommandRequest result = customParser.customParse(ctx);
        // }
    }

    private static void collectAnnotatedFields(Class<?> clazz, 
            Map<String, Field> flagFields, 
            Map<String, Field> keywordFields,
            Map<Field, FlagParam> flagAnnotations) {
        
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            
            FlagParam flagAnnotation = field.getAnnotation(FlagParam.class);
            if (flagAnnotation != null) {
                for (String name : flagAnnotation.names()) {
                    flagFields.put(name, field);
                }
                flagAnnotations.put(field, flagAnnotation);
            }
            
            KeywordParam keywordAnnotation = field.getAnnotation(KeywordParam.class);
            if (keywordAnnotation != null) {
                keywordFields.put(keywordAnnotation.name(), field);
                for (String alias : keywordAnnotation.names()) {
                    keywordFields.put(alias, field);
                }
            }
        }
    }

    private static void setFieldValue(Object target, Field field, String value) {
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            
            if (type == String.class) {
                field.set(target, value);
            } else if (type == int.class || type == Integer.class) {
                field.set(target, Integer.parseInt(value));
            } else if (type == long.class || type == Long.class) {
                field.set(target, Long.parseLong(value));
            } else if (type == boolean.class || type == Boolean.class) {
                field.set(target, Boolean.parseBoolean(value));
            } else if (type == double.class || type == Double.class) {
                field.set(target, Double.parseDouble(value));
            } else if (type == float.class || type == Float.class) {
                field.set(target, Float.parseFloat(value));
            }
        } catch (Exception e) {
            logger.error("设置字段值失败: " + field.getName() + " = " + value + ": " + e.getMessage());
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private record FieldPosition(Field field, int order) {}
}
