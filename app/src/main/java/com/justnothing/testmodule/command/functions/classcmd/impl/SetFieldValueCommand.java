package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.DirectCommand;
import com.justnothing.testmodule.command.functions.classcmd.request.SetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.SetFieldValueResult;
import com.justnothing.testmodule.command.functions.classcmd.util.ExpressionParser;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 轻量级字段值设置命令。
 *
 * <p>通过 {@link DirectCommand} 包装，将原 {@code SetFieldValueRequestHandler}
 * 的逻辑迁移到统一的 Command 架构中。
 */
public class SetFieldValueCommand extends DirectCommand<SetFieldValueRequest, SetFieldValueResult> {

    private static final Logger logger = Logger.getLoggerForName("SetFieldValueCommand");

    public SetFieldValueCommand() {
        super("setfield",
            SetFieldValueRequest.class,
            SetFieldValueResult.class,
                SetFieldValueCommand::executeSetField);
    }

    private static SetFieldValueResult executeSetField(ClassCommandContext<SetFieldValueRequest> context,
                                                       SetFieldValueRequest request) {
        String className = request.getClassName();
        String fieldName = request.getFieldName();
        String targetInstanceExpr = request.getTargetInstance();
        String valueExpr = request.getValueExpression();
        String valueTypeHint = request.getValueTypeHint();
        boolean isStatic = request.isStatic();

        logger.debug("设置字段值: " + className + "." + fieldName);

        SetFieldValueResult result = new SetFieldValueResult();

        if (className == null || className.isEmpty()) {
            return error(result, "INVALID_REQUEST", "类名不能为空");
        }
        if (fieldName == null || fieldName.isEmpty()) {
            return error(result, "INVALID_REQUEST", "字段名不能为空");
        }
        if (valueExpr == null || valueExpr.isEmpty()) {
            return error(result, "INVALID_REQUEST", "值表达式不能为空");
        }

        ClassLoader classLoader = null;
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className);
            classLoader = targetClass.getClassLoader();

            Field field = ReflectionUtils.findField(targetClass, fieldName);
            if (field == null) {
                return error(result, "FIELD_NOT_FOUND", "字段未找到: " + fieldName);
            }

            field.setAccessible(true);

            Object targetInstance = null;
            if (!Modifier.isStatic(field.getModifiers())) {
                if (targetInstanceExpr == null || targetInstanceExpr.isEmpty()) {
                    return error(result, "INVALID_REQUEST", "实例字段需要提供目标实例表达式");
                }
                try {
                    ExpressionParser.ParseResult instanceResult =
                        ExpressionParser.parse(targetInstanceExpr, classLoader);
                    targetInstance = instanceResult.value();
                    if (targetInstance == null) {
                        return error(result, "NULL_INSTANCE", "目标实例为 null");
                    }
                    logger.debug("目标实例: " + targetInstance);
                } catch (Exception e) {
                    return error(result, "PARSE_ERROR",
                        "解析目标实例失败: " + e.getMessage());
                }
            }

            Object value;
            try {
                ExpressionParser.ParseResult parseResult;
                if (valueTypeHint != null && !valueTypeHint.isEmpty()) {
                    Class<?> hintClass = resolveType(valueTypeHint, classLoader);
                    parseResult = ExpressionParser.parse(valueExpr, classLoader, hintClass);
                } else {
                    parseResult = ExpressionParser.parse(valueExpr, classLoader, field.getType());
                }
                value = parseResult.value();
                logger.debug("解析值: " + value + " (类型: " + parseResult.type().getName() + ")");
            } catch (Exception e) {
                return error(result, "PARSE_ERROR",
                    "解析值表达式失败: " + e.getMessage());
            }

            field.set(targetInstance, value);

            result.setSuccess(true);
            result.setClassName(className);
            result.setFieldName(fieldName);
            result.setValue(value != null ? value.toString() : "null");

            context.execContext().print("? 字段值已设置: ", Colors.GREEN);
            context.execContext().print(className + "." + fieldName, Colors.CYAN);
            context.execContext().println(" = " + value, Colors.YELLOW);

            logger.info("字段值设置成功: " + fieldName + " = " + value);

        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            return error(result, "CLASS_NOT_FOUND", "类未找到: " + className);
        } catch (Exception e) {
            logger.error("字段值设置失败: " + className + "." + fieldName, e);
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage();
            return error(result, "SET_ERROR", "设置失败: " + errorMsg);
        } finally {
            if (classLoader != null) {
                ExpressionParser.clearVariables(classLoader);
            }
        }

        return result;
    }

    private static SetFieldValueResult error(SetFieldValueResult result, String code, String message) {
        result.setError(new CommandResult.ErrorInfo(code, message));
        result.setSuccess(false);
        return result;
    }

    private static Class<?> resolveType(String typeName, ClassLoader classLoader)
            throws ClassNotFoundException {
        switch (typeName) {
            case "int": return int.class;
            case "long": return long.class;
            case "double": return double.class;
            case "float": return float.class;
            case "boolean": return boolean.class;
            case "short": return short.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "void": return void.class;
            default:
                if (typeName.endsWith("[]")) {
                    String componentType = typeName.substring(0, typeName.length() - 2);
                    Class<?> componentClass = resolveType(componentType, classLoader);
                    return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
                }
                return Class.forName(typeName, true, classLoader);
        }
    }
}
