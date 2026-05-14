package com.justnothing.testmodule.command.base.validator;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;

public class RangeValidator implements ParamValidator {

    private final Number min;
    private final Number max;
    private final boolean inclusive;

    public RangeValidator(Number min, Number max) {
        this(min, max, true);
    }

    public RangeValidator(Number min, Number max, boolean inclusive) {
        this.min = min;
        this.max = max;
        this.inclusive = inclusive;
    }

    @Override
    public void validate(String paramName, String value) throws IllegalCommandLineArgumentException {
        if (value == null || value.isEmpty()) return;

        try {
            double numValue = Double.parseDouble(value);
            
            if (min != null) {
                if (inclusive) {
                    if (numValue < min.doubleValue()) {
                        throw new IllegalCommandLineArgumentException(getErrorMessage(paramName, value));
                    }
                } else {
                    if (numValue <= min.doubleValue()) {
                        throw new IllegalCommandLineArgumentException(getErrorMessage(paramName, value));
                    }
                }
            }

            if (max != null) {
                if (inclusive) {
                    if (numValue > max.doubleValue()) {
                        throw new IllegalCommandLineArgumentException(getErrorMessage(paramName, value));
                    }
                } else {
                    if (numValue >= max.doubleValue()) {
                        throw new IllegalCommandLineArgumentException(getErrorMessage(paramName, value));
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalCommandLineArgumentException(
                String.format("参数 '%s' 的值 '%s' 不是有效的数字", paramName, value)
            );
        }
    }

    @Override
    public String getErrorMessage(String paramName, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("参数 '%s' 的值 '%s' 超出范围", paramName, value));

        if (min != null && max != null) {
            sb.append(String.format(", 允许范围: [%s, %s]", min, max));
        } else if (min != null) {
            sb.append(String.format(", 最小值: %s", min));
        } else if (max != null) {
            sb.append(String.format(", 最大值: %s", max));
        }

        return sb.toString();
    }
}
