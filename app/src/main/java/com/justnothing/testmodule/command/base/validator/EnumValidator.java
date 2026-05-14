package com.justnothing.testmodule.command.base.validator;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EnumValidator implements ParamValidator {

    private final Set<String> allowedValues;
    private final boolean caseSensitive;

    public EnumValidator(String... allowedValues) {
        this(false, allowedValues);
    }

    public EnumValidator(boolean caseSensitive, String... allowedValues) {
        this.caseSensitive = caseSensitive;
        if (caseSensitive) {
            this.allowedValues = new HashSet<>(Arrays.asList(allowedValues));
        } else {
            this.allowedValues = new HashSet<>();
            for (String v : allowedValues) {
                this.allowedValues.add(v.toLowerCase());
            }
        }
    }

    @Override
    public void validate(String paramName, String value) throws IllegalCommandLineArgumentException {
        if (value == null) return;

        String checkValue = caseSensitive ? value : value.toLowerCase();
        if (!allowedValues.contains(checkValue)) {
            throw new IllegalCommandLineArgumentException(
                getErrorMessage(paramName, value)
            );
        }
    }

    @Override
    public String getErrorMessage(String paramName, String value) {
        return String.format("参数 '%s' 的值 '%s' 无效，允许的值: %s",
            paramName, value, String.join(", ", allowedValues));
    }

    public Set<String> getAllowedValues() {
        return new HashSet<>(allowedValues);
    }
}
