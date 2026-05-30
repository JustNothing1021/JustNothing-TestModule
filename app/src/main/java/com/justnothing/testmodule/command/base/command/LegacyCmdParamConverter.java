package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.validator.AllowedValues;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Locale;

/**
 * 旧注解 → CmdParam 转换器 (兼容层)
 * <p>
 * 将旧的 @PositionalParam, @KeywordParam, @FlagParam 注解
 * 动态转换为统一的 @CmdParam 格式
 */
public class LegacyCmdParamConverter {

    /**
     * 转换 @PositionalParam → CmdParam
     */
    public static CmdParam PositionalToCmdParam(PositionalParam posAnno, Field field) {
        AllowedValues allowedValues = field.getAnnotation(AllowedValues.class);
        
        return createCmdParamProxy(
            field.getName(),                    // name
            posAnno.name(),                     // description
            posAnno.required(),                 // required
            posAnno.defaultValue(),             // defaultValue
            new String[0],                      // aliases (位置参数无别名)
            posAnno.varArgs(),                  // varArgs
            posAnno.order(),                    // position
            CmdParam.ReadMode.STRIPPED,         // readMode
            "",                                 // serializedName
            true,                               // serialize
            true,                               // deserialize
            allowedValues != null ? allowedValues.value() : new String[0],  // allowedValues
            "",                                // pattern
            Double.NEGATIVE_INFINITY,          // min
            Double.POSITIVE_INFINITY,          // max
            new String[0],                     // mutexWith
            new String[0],                     // requires
            false,                             // isOperator
            0,                                 // operatorArgs
            "",                                // belongsToOperator
            -1                                 // operatorIndex
        );
    }

    /**
     * 转换 @KeywordParam → CmdParam
     */
    public static CmdParam KeywordToCmdParam(KeywordParam kwAnno, Field field) {
        AllowedValues allowedValues = field.getAnnotation(AllowedValues.class);
        
        String[] aliases;
        if (kwAnno.names() != null && kwAnno.names().length > 0) {
            aliases = new String[kwAnno.names().length];
            for (int i = 0; i < kwAnno.names().length; i++) {
                aliases[i] = "--" + kwAnno.names()[i]; // 添加 -- 前缀
            }
        } else {
            aliases = new String[]{"--" + kwAnno.name()};
        }
        
        return createCmdParamProxy(
            kwAnno.name(),                       // name
            kwAnno.name(),                       // description
            kwAnno.required(),                   // required
            kwAnno.defaultValue(),               // defaultValue
            aliases,                              // aliases
            false,                               // varArgs
            0,                                   // position (关键字参数无位置)
            CmdParam.ReadMode.STRIPPED,           // readMode
            "",                                  // serializedName
            true,                                // serialize
            true,                                // deserialize
            allowedValues != null ? allowedValues.value() : new String[0],
            "",                                 // pattern
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            new String[0],                       // mutexWith
            new String[0],                       // requires
            false,                               // isOperator
            0,                                   // operatorArgs
            "",                                  // belongsToOperator
            -1                                   // operatorIndex
        );
    }

    /**
     * 转换 @FlagParam → CmdParam
     */
    public static CmdParam FlagToCmdParam(FlagParam flagAnno, Field field) {
        String[] aliases = new String[flagAnno.names().length];
        for (int i = 0; i < flagAnno.names().length; i++) {
            String name = flagAnno.names()[i];
            if (!name.startsWith("-")) {
                name = name.length() == 1 ? "-" + name : "--" + name;
            }
            aliases[i] = name;
        }
        
        return createCmdParamProxy(
            flagAnno.names().length > 0 ? flagAnno.names()[0] : field.getName(),
            "Flag: " + field.getName(),
            false,             // required (flags通常是可选的)
            "false",           // defaultValue (boolean默认false)
            aliases,           // aliases (如 -v, --verbose)
            false,             // varArgs
            0,                 // position
            CmdParam.ReadMode.RAW,  // readMode (flags不需要引号处理)
            "",                // serializedName
            true,              // serialize
            true,              // deserialize
            new String[0],     // allowedValues
            "",                // pattern
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            new String[0],     // mutexWith
            new String[0],     // requires
            false,             // isOperator
            0,                 // operatorArgs
            "",                // belongsToOperator
            -1                 // operatorIndex
        );
    }

    /**
     * 动态创建 CmdParam 注解的代理实例
     * 使用 Java Proxy 机制模拟注解对象
     */
    private static CmdParam createCmdParamProxy(
            final String name,
            final String description,
            final boolean required,
            final String defaultValue,
            final String[] aliases,
            final boolean varArgs,
            final int position,
            final CmdParam.ReadMode readMode,
            final String serializedName,
            final boolean serialize,
            final boolean deserialize,
            final String[] allowedValues,
            final String pattern,
            final double min,
            final double max,
            final String[] mutexWith,
            final String[] requires,
            final boolean isOperator,
            final int operatorArgs,
            final String belongsToOperator,
            final int operatorIndex) {

        return (CmdParam) Proxy.newProxyInstance(
            CmdParam.class.getClassLoader(),
            new Class<?>[]{CmdParam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "name" -> name;
                    case "description" -> description;
                    case "required" -> required;
                    case "defaultValue" -> defaultValue;
                    case "aliases" -> aliases;
                    case "varArgs" -> varArgs;
                    case "position" -> position;
                    case "readMode" -> readMode;
                    case "serializedName" -> serializedName;
                    case "serialize" -> serialize;
                    case "deserialize" -> deserialize;
                    case "allowedValues" -> allowedValues;
                    case "pattern" -> pattern;
                    case "min" -> min;
                    case "max" -> max;
                    case "mutexWith" -> mutexWith;
                    case "requires" -> requires;
                    case "isOperator" -> isOperator;
                    case "operatorArgs" -> operatorArgs;
                    case "belongsToOperator" -> belongsToOperator;
                    case "operatorIndex" -> operatorIndex;
                    case "annotationType" -> CmdParam.class;
                    case "toString" ->
                            String.format(
                                    Locale.getDefault(),
                                    "@CmdParam(name=%s, position=%d, required=%b)",
                                    name, position, required);
                    case "hashCode" -> (name + position).hashCode();
                    case "equals" -> {
                        if (args[0] instanceof CmdParam other) {
                            yield name.equals(other.name()) && position == other.position();
                        }
                        yield false;
                    }
                    default ->
                            throw new UnsupportedOperationException("Unknown method: " + method.getName());
                }
        );
    }
}
