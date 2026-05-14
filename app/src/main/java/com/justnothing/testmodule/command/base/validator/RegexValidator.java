package com.justnothing.testmodule.command.base.validator;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexValidator implements ParamValidator {

    private final Pattern pattern;
    private final String patternDescription;

    public RegexValidator(String regex) {
        this(regex, null);
    }

    public RegexValidator(String regex, String patternDescription) {
        try {
            this.pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("无效的正则表达式: " + regex, e);
        }
        this.patternDescription = patternDescription != null ? patternDescription : regex;
    }

    @Override
    public void validate(String paramName, String value) throws IllegalCommandLineArgumentException {
        if (value == null || value.isEmpty()) return;

        if (!pattern.matcher(value).matches()) {
            throw new IllegalCommandLineArgumentException(
                getErrorMessage(paramName, value)
            );
        }
    }

    @Override
    public String getErrorMessage(String paramName, String value) {
        return String.format("参数 '%s' 的值 '%s' 不匹配格式: %s",
            paramName, value, patternDescription);
    }
}
