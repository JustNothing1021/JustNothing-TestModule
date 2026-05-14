package com.justnothing.testmodule.command.base.validator;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;

/**
 * 参数验证器接口 - 用于 @PositionalParam/@KeywordParam/@FlagParam 注解
 *
 * 使用示例:
 * <pre>
 * // 方式1: 直接指定 Validator 类
 * &#64;PositionalParam(name = "action", order = 1,
 *     validator = &#64;ParamValidatorDef(validator = EnumValidator.class, allowedValues = {"start", "stop"}))
 *
 * // 方式2: 使用便捷注解 (推荐)
 * &#64;AllowedValues({"start", "stop", "report", "export"})
 * &#64;PositionalParam(name = "action", order = 1)
 * </pre>
 */
public interface ParamValidator {

    void validate(String paramName, String value) throws IllegalCommandLineArgumentException;

    String getErrorMessage(String paramName, String value);
}
