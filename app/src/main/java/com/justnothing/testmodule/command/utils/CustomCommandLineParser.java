package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;

import java.util.List;
import java.util.Map;

/**
 * 自定义命令行解析器接口
 * 
 * 用于处理无法用声明式注解表达的复杂参数解析场景。
 * 
 * <p>使用场景示例:</p>
 * <ul>
 *   <li>Hook命令的复合参数: {@code before code 'xxx'} (消费3个token)</li>
 *   <li>可变数量参数消费: {@code -p a b c} (消费N个token)</li>
 *   <li>复杂的互斥/依赖验证逻辑</li>
 *   <li>启发式参数解析 (如ReflectClass的兜底逻辑)</li>
 * </ul>
 */
public interface CustomCommandLineParser {
    
    CommandRequest customParse(ParseContext context) throws IllegalCommandLineArgumentException;
    
    /**
     * 解析上下文 (传递给自定义解析器)
     */
    class ParseContext {
        private final String[] originalArgs;
        private final List<String> remainingArgs;
        private final Map<String, Object> parsedValues;
        
        public ParseContext(String[] originalArgs, List<String> remainingArgs, Map<String, Object> parsedValues) {
            this.originalArgs = originalArgs;
            this.remainingArgs = remainingArgs;
            this.parsedValues = parsedValues;
        }
        
        public String[] originalArgs() { return originalArgs; }
        public List<String> remainingArgs() { return remainingArgs; }
        public Map<String, Object> parsedValues() { return parsedValues; }
    }
}
