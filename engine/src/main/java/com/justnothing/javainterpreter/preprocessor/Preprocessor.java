package com.justnothing.javainterpreter.preprocessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
    
    private final Map<String, Macro> macros = new HashMap<>();
    private final Stack<ConditionState> conditionStack = new Stack<>();
    private final List<Path> includePaths = new ArrayList<>();
    private String currentFile = "<unknown>";
    private int currentLine = 0;
    
    private static class Macro {
        final String name;
        final List<String> params;
        final String body;
        final boolean isFunction;
        
        Macro(String name, String body) {
            this.name = name;
            this.params = null;
            this.body = body;
            this.isFunction = false;
        }
        
        Macro(String name, List<String> params, String body) {
            this.name = name;
            this.params = params;
            this.body = body;
            this.isFunction = true;
        }
    }
    
    private static class ConditionState {
        boolean wasActive;
        boolean isActive;
        boolean elseSeen;
        
        ConditionState(boolean active) {
            this.wasActive = active;
            this.isActive = active;
            this.elseSeen = false;
        }
    }
    
    private static final Pattern DEFINE_PATTERN = Pattern.compile("^#define\\s+(\\w+)(?:\\s+(.*))?$");
    private static final Pattern DEFINE_FUNC_PATTERN = Pattern.compile("^#define\\s+(\\w+)\\s*\\(([^)]*)\\)\\s+(.*)$");
    private static final Pattern UNDEF_PATTERN = Pattern.compile("^#undef\\s+(\\w+)$");
    private static final Pattern IFDEF_PATTERN = Pattern.compile("^#ifdef\\s+(\\w+)$");
    private static final Pattern IFNDEF_PATTERN = Pattern.compile("^#ifndef\\s+(\\w+)$");
    private static final Pattern IF_PATTERN = Pattern.compile("^#if\\s+(.+)$");
    private static final Pattern ELIF_PATTERN = Pattern.compile("^#elif\\s+(.+)$");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^#else$");
    private static final Pattern ENDIF_PATTERN = Pattern.compile("^#endif$");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^#include\\s+[\"<]([^\">]+)[\">]$");
    
    public Preprocessor() {
        macros.put("true", new Macro("true", "true"));
        macros.put("false", new Macro("false", "false"));
        macros.put("__JN__", new Macro("__JN__", "1"));
    }
    
    public void addIncludePath(String path) {
        includePaths.add(Paths.get(path));
    }
    
    public String process(String source) {
        return process(source, "<input>");
    }
    
    public String process(String source, String fileName) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        
        String oldFile = currentFile;
        currentFile = fileName;
        
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n", -1);
        
        for (int i = 0; i < lines.length; i++) {
            currentLine = i + 1;
            String line = lines[i];
            String processedLine = processLine(line.trim(), line);
            if (processedLine != null) {
                result.append(processedLine).append("\n");
            }
        }
        
        currentFile = oldFile;
        return result.toString();
    }
    
    private String processLine(String trimmedLine, String originalLine) {
        if (trimmedLine.startsWith("#")) {
            return processDirective(trimmedLine);
        }
        
        if (!isInActiveBlock()) {
            return null;
        }
        
        return expandMacros(originalLine);
    }
    
    private String processDirective(String line) {
        Matcher matcher;
        
        matcher = INCLUDE_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (isInActiveBlock()) {
                String fileName = matcher.group(1);
                return processInclude(fileName);
            }
            return null;
        }
        
        matcher = DEFINE_FUNC_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (isInActiveBlock()) {
                String name = matcher.group(1);
                String paramsStr = matcher.group(2).trim();
                String body = matcher.group(3).trim();
                List<String> params = parseParams(paramsStr);
                macros.put(name, new Macro(name, params, body));
            }
            return null;
        }
        
        matcher = DEFINE_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (isInActiveBlock()) {
                String name = matcher.group(1);
                String value = matcher.group(2) != null ? matcher.group(2).trim() : "";
                macros.put(name, new Macro(name, value));
            }
            return null;
        }
        
        matcher = UNDEF_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (isInActiveBlock()) {
                String name = matcher.group(1);
                macros.remove(name);
            }
            return null;
        }
        
        matcher = IFDEF_PATTERN.matcher(line);
        if (matcher.matches()) {
            String name = matcher.group(1);
            boolean defined = macros.containsKey(name);
            pushCondition(defined);
            return null;
        }
        
        matcher = IFNDEF_PATTERN.matcher(line);
        if (matcher.matches()) {
            String name = matcher.group(1);
            boolean notDefined = !macros.containsKey(name);
            pushCondition(notDefined);
            return null;
        }
        
        matcher = IF_PATTERN.matcher(line);
        if (matcher.matches()) {
            String expr = matcher.group(1);
            boolean result = evaluateCondition(expr);
            pushCondition(result);
            return null;
        }
        
        matcher = ELIF_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (conditionStack.isEmpty()) {
                throw new PreprocessorException("#elif without #if/#ifdef/#ifndef");
            }
            ConditionState state = conditionStack.peek();
            if (state.elseSeen) {
                throw new PreprocessorException("#elif after #else");
            }
            if (state.wasActive) {
                state.isActive = false;
            } else {
                String expr = matcher.group(1);
                boolean result = evaluateCondition(expr);
                state.isActive = result && isParentActive();
                if (result) {
                    state.wasActive = true;
                }
            }
            return null;
        }
        
        matcher = ELSE_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (conditionStack.isEmpty()) {
                throw new PreprocessorException("#else without #if/#ifdef/#ifndef");
            }
            ConditionState state = conditionStack.peek();
            if (state.elseSeen) {
                throw new PreprocessorException("duplicate #else");
            }
            state.elseSeen = true;
            state.isActive = !state.wasActive && isParentActive();
            return null;
        }
        
        matcher = ENDIF_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (conditionStack.isEmpty()) {
                throw new PreprocessorException("#endif without #if/#ifdef/#ifndef");
            }
            conditionStack.pop();
            return null;
        }
        
        if (isInActiveBlock()) {
            throw new PreprocessorException("Unknown preprocessor directive: " + line);
        }
        
        return null;
    }
    
    private List<String> parseParams(String paramsStr) {
        List<String> params = new ArrayList<>();
        if (paramsStr.isEmpty()) {
            return params;
        }
        for (String param : paramsStr.split(",")) {
            params.add(param.trim());
        }
        return params;
    }
    
    private void pushCondition(boolean active) {
        boolean parentActive = isInActiveBlock();
        conditionStack.push(new ConditionState(active && parentActive));
    }
    
    private boolean isInActiveBlock() {
        if (conditionStack.isEmpty()) {
            return true;
        }
        return conditionStack.peek().isActive;
    }
    
    private boolean isParentActive() {
        if (conditionStack.size() <= 1) {
            return true;
        }
        ConditionState current = conditionStack.pop();
        boolean result = isInActiveBlock();
        conditionStack.push(current);
        return result;
    }
    
    private boolean evaluateCondition(String expr) {
        expr = expr.trim();
        
        if (expr.equals("1") || expr.equals("true")) {
            return true;
        }
        if (expr.equals("0") || expr.equals("false")) {
            return false;
        }
        
        if (expr.startsWith("!defined(") && expr.endsWith(")")) {
            String macroName = expr.substring(9, expr.length() - 1);
            return !macros.containsKey(macroName);
        }
        
        if (expr.startsWith("defined(") && expr.endsWith(")")) {
            String macroName = expr.substring(8, expr.length() - 1);
            return macros.containsKey(macroName);
        }
        
        expr = expandMacros(expr);
        
        Matcher comparison = Pattern.compile("(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(\\w+)").matcher(expr);
        if (comparison.matches()) {
            String left = comparison.group(1);
            String op = comparison.group(2);
            String right = comparison.group(3);
            
            if (macros.containsKey(left)) {
                left = macros.get(left).body;
            }
            if (macros.containsKey(right)) {
                right = macros.get(right).body;
            }
            
            return compareValues(left, op, right);
        }
        
        Matcher logicalNot = Pattern.compile("!\\s*(\\w+)").matcher(expr);
        if (logicalNot.matches()) {
            String name = logicalNot.group(1);
            return !macros.containsKey(name);
        }
        
        return macros.containsKey(expr);
    }
    
    private boolean compareValues(String left, String op, String right) {
        try {
            long leftNum = Long.parseLong(left);
            long rightNum = Long.parseLong(right);
            return switch (op) {
                case "==" -> leftNum == rightNum;
                case "!=" -> leftNum != rightNum;
                case ">=" -> leftNum >= rightNum;
                case "<=" -> leftNum <= rightNum;
                case ">" -> leftNum > rightNum;
                case "<" -> leftNum < rightNum;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return switch (op) {
                case "==" -> left.equals(right);
                case "!=" -> !left.equals(right);
                default -> false;
            };
        }
    }
    
    private String processInclude(String fileName) {
        Path filePath = findIncludeFile(fileName);
        if (filePath == null) {
            throw new PreprocessorException("Cannot find include file: " + fileName);
        }
        
        try {
            String content = Files.readString(filePath);
            return process(content, fileName);
        } catch (IOException e) {
            throw new PreprocessorException("Failed to read include file: " + fileName, e);
        }
    }
    
    private Path findIncludeFile(String fileName) {
        Path directPath = Paths.get(fileName);
        if (Files.exists(directPath)) {
            return directPath;
        }
        
        Path relativeToCurrent = Paths.get(currentFile).getParent().resolve(fileName);
        if (Files.exists(relativeToCurrent)) {
            return relativeToCurrent;
        }
        
        for (Path includePath : includePaths) {
            Path fullPath = includePath.resolve(fileName);
            if (Files.exists(fullPath)) {
                return fullPath;
            }
        }
        
        return null;
    }
    
    private String expandMacros(String line) {
        List<String> stringLiterals = new ArrayList<>();
        StringBuilder protectedLine = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;
        StringBuilder currentString = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (escape) {
                currentString.append(c);
                escape = false;
                continue;
            }
            
            if (c == '\\' && (inString || inChar)) {
                currentString.append(c);
                escape = true;
                continue;
            }
            
            if (c == '"' && !inChar) {
                if (!inString) {
                    inString = true;
                    currentString = new StringBuilder();
                    currentString.append(c);
                } else {
                    currentString.append(c);
                    stringLiterals.add(currentString.toString());
                    protectedLine.append("__JN_STR_").append(stringLiterals.size() - 1).append("__");
                    inString = false;
                }
                continue;
            }
            
            if (c == '\'' && !inString) {
                if (!inChar) {
                    inChar = true;
                    currentString = new StringBuilder();
                    currentString.append(c);
                } else {
                    currentString.append(c);
                    stringLiterals.add(currentString.toString());
                    protectedLine.append("__JN_STR_").append(stringLiterals.size() - 1).append("__");
                    inChar = false;
                }
                continue;
            }
            
            if (inString || inChar) {
                currentString.append(c);
                continue;
            }
            
            protectedLine.append(c);
        }
        
        String expanded = protectedLine.toString();
        
        String escapedFile = currentFile.replace("\\", "\\\\");
        expanded = expanded.replace("__FILE__", "\"" + escapedFile + "\"");
        expanded = expanded.replace("__LINE__", String.valueOf(currentLine));
        
        for (Macro macro : macros.values()) {
            if (macro.isFunction) {
                expanded = expandFunctionMacro(expanded, macro, stringLiterals);
            } else if (!macro.body.isEmpty()) {
                expanded = expanded.replaceAll("\\b" + Pattern.quote(macro.name) + "\\b", 
                    Matcher.quoteReplacement(macro.body));
            }
        }
        
        for (int i = 0; i < stringLiterals.size(); i++) {
            expanded = expanded.replace("__JN_STR_" + i + "__", stringLiterals.get(i));
        }
        
        return expanded;
    }
    
    private String expandFunctionMacro(String line, Macro macro, List<String> protectedStrings) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(macro.name) + "\\s*\\(");
        Matcher matcher = pattern.matcher(line);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        while (matcher.find()) {
            result.append(line, lastEnd, matcher.start());
            
            int start = matcher.end();
            int depth = 1;
            int end = start;
            
            while (end < line.length() && depth > 0) {
                char c = line.charAt(end);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                end++;
            }
            
            String argsStr = line.substring(start, end - 1);
            List<String> args = parseArguments(argsStr);
            
            String expanded = expandMacroBody(macro, args, protectedStrings);
            result.append(expanded);
            
            lastEnd = end;
        }
        
        result.append(line.substring(lastEnd));
        return result.toString();
    }
    
    private List<String> parseArguments(String argsStr) {
        List<String> args = new ArrayList<>();
        if (argsStr.trim().isEmpty()) {
            return args;
        }
        
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : argsStr.toCharArray()) {
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                args.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            args.add(current.toString().trim());
        }
        
        return args;
    }
    
    private String expandMacroBody(Macro macro, List<String> args, List<String> protectedStrings) {
        String body = macro.body;
        
        List<String> bodyStrings = new ArrayList<>();
        StringBuilder protectedBody = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;
        StringBuilder currentString = new StringBuilder();
        
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            
            if (escape) {
                currentString.append(c);
                escape = false;
                continue;
            }
            
            if (c == '\\' && (inString || inChar)) {
                currentString.append(c);
                escape = true;
                continue;
            }
            
            if (c == '"' && !inChar) {
                if (!inString) {
                    inString = true;
                    currentString = new StringBuilder();
                    currentString.append(c);
                } else {
                    currentString.append(c);
                    bodyStrings.add(currentString.toString());
                    protectedBody.append("__JN_STR_").append(protectedStrings.size()).append("__");
                    protectedStrings.add(currentString.toString());
                    inString = false;
                }
                continue;
            }
            
            if (c == '\'' && !inString) {
                if (!inChar) {
                    inChar = true;
                    currentString = new StringBuilder();
                    currentString.append(c);
                } else {
                    currentString.append(c);
                    bodyStrings.add(currentString.toString());
                    protectedBody.append("__JN_STR_").append(protectedStrings.size()).append("__");
                    protectedStrings.add(currentString.toString());
                    inChar = false;
                }
                continue;
            }
            
            if (inString || inChar) {
                currentString.append(c);
                continue;
            }
            
            protectedBody.append(c);
        }
        
        String result = protectedBody.toString();
        
        if (macro.params != null) {
            for (int i = 0; i < macro.params.size() && i < args.size(); i++) {
                String param = macro.params.get(i);
                String arg = args.get(i);
                result = result.replaceAll("\\b" + Pattern.quote(param) + "\\b", 
                    Matcher.quoteReplacement(arg));
            }
        }
        
        return result;
    }
    
    public void define(String name, String value) {
        macros.put(name, new Macro(name, value != null ? value : ""));
    }
    
    public void define(String name) {
        macros.put(name, new Macro(name, ""));
    }
    
    public void undefine(String name) {
        macros.remove(name);
    }
    
    public boolean isDefined(String name) {
        return macros.containsKey(name);
    }
    
    public void clearMacros() {
        macros.clear();
        macros.put("true", new Macro("true", "true"));
        macros.put("false", new Macro("false", "false"));
        macros.put("__JN__", new Macro("__JN__", "1"));
    }
    
    public Map<String, String> getMacros() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Macro> entry : macros.entrySet()) {
            result.put(entry.getKey(), entry.getValue().body);
        }
        return result;
    }
}
