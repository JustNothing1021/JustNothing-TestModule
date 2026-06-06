package com.justnothing.testmodule.command.base.validator;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ValidatorFactory {

    public static List<ParamValidator> createValidators(Field field) {
        List<ParamValidator> validators = new ArrayList<>();

        AllowedValues allowedValues = field.getAnnotation(AllowedValues.class);
        if (allowedValues != null) {
            validators.add(new EnumValidator(allowedValues.caseSensitive(), allowedValues.value()));
        }

        Range range = field.getAnnotation(Range.class);
        if (range != null) {
            double min = range.min();
            double max = range.max();
            
            if (min != Double.NEGATIVE_INFINITY || max != Double.POSITIVE_INFINITY) {
                Number minNum = min == Double.NEGATIVE_INFINITY ? null : min;
                Number maxNum = max == Double.POSITIVE_INFINITY ? null : max;
                validators.add(new RangeValidator(minNum, maxNum, range.inclusive()));
            }
        }

        Pattern pattern = field.getAnnotation(Pattern.class);
        if (pattern != null) {
            String desc = pattern.description().isEmpty() ? pattern.regex() : pattern.description();
            validators.add(new RegexValidator(pattern.regex(), desc));
        }

        return validators;
    }

    public static void validateField(String paramName, String value, Field field) 
            throws IllegalCommandLineArgumentException {
        
        List<ParamValidator> validators = createValidators(field);
        for (ParamValidator validator : validators) {
            validator.validate(paramName, value);
        }
    }
}