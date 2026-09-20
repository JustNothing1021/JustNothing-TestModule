package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;

import java.util.List;
import java.util.Map;

/**
 * 自定义命令行解析器接口，用于处理无法用声明式注解表达的复杂参数解析场景。
 */
public interface CustomCommandLineParser {
    
    CommandRequest<?> customParse(ParseContext context) throws IllegalCommandLineArgumentException;

    /**
         * 解析上下文 (传递给自定义解析器)
         */
        record ParseContext(String[] originalArgs, List<String> remainingArgs,
                            Map<String, Object> parsedValues) {
    }
}
