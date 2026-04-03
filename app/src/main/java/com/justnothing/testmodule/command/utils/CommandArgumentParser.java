package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.utils.functions.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandArgumentParser {

    public record ParseResult(String commandLine, String classLoader) {
    }
    
    public static ParseResult parseOptions(String cmdline, Logger logger) {
        String classLoader = null;
        
        while (true) {
            cmdline = cmdline.trim();
            if (cmdline.startsWith("-")) {
                int index = cmdline.indexOf(' ');
                if (index == -1) {
                    return new ParseResult(cmdline, classLoader);
                } else {
                    String option = cmdline.substring(0, index);
                    if (option.equals("-cl") || option.equals("-classloader")) {
                        cmdline = cmdline.substring(index).trim();
                        int nextIndex = cmdline.indexOf(' ');
                        if (nextIndex == -1) {
                            logger.warn("指定了类加载器参数，但没有指定类加载器名称");
                            classLoader = cmdline;
                            cmdline = "";
                        } else {
                            classLoader = cmdline.substring(0, nextIndex);
                            cmdline = cmdline.substring(nextIndex).trim();
                        }
                    } else {
                        logger.warn("无效的参数: " + option);
                        cmdline = cmdline.substring(index).trim();
                    }
                }
            } else {
                break;
            }
        }
        return new ParseResult(cmdline, classLoader);
    }
    
    public static String[] splitArguments(String cmdline) {
        if (cmdline == null || cmdline.trim().isEmpty()) {
            return new String[0];
        }
        
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inSingleQuotes = false;
        char quoteChar = '"';
        
        for (int i = 0; i < cmdline.length(); i++) {
            char c = cmdline.charAt(i);
            
            if (inQuotes || inSingleQuotes) {
                current.append(c);
                
                if (c == '\\' && i + 1 < cmdline.length()) {
                    char next = cmdline.charAt(i + 1);
                    if (next == quoteChar || next == '\\') {
                        current.append(next);
                        i++;
                        continue;
                    }
                }
                
                if (c == quoteChar) {
                    if (inQuotes) inQuotes = false;
                    else if (inSingleQuotes) inSingleQuotes = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = (c == '"');
                    inSingleQuotes = (c == '\'');
                    quoteChar = c;
                    current.append(c);
                } else if (c == ' ' || c == '\t') {
                    if (current.length() > 0) {
                        args.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }
        
        if (current.length() > 0) {
            args.add(current.toString());
        }
        
        return args.toArray(new String[0]);
    }
    
    public static String getOptionValue(String[] args, String... optionNames) {
        for (int i = 0; i < args.length - 1; i++) {
            for (String option : optionNames) {
                if (args[i].equals(option)) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }
    
    public static boolean hasOption(String[] args, String... optionNames) {
        for (String arg : args) {
            for (String option : optionNames) {
                if (arg.equals(option)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static Integer parseId(String[] args, int index) {
        if (args.length <= index) {
            return null;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public static Integer parseInt(String[] args, int index, String fieldName) {
        if (args.length <= index) {
            return null;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "%s必须是数字", fieldName)
            );
        }
    }
    
    public static Long parseLong(String[] args, int index, String fieldName) {
        if (args.length <= index) {
            return null;
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "%s必须是数字", fieldName)
            );
        }
    }
    
    public static void requireMin(long value, long min, String fieldName) {
        if (value < min) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "%s不能小于%d", fieldName, min)
            );
        }
    }
    
    public static void requireMax(long value, long max, String fieldName) {
        if (value > max) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "%s不能大于%d", fieldName, max)
            );
        }
    }
    
    public static void requireRange(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "%s必须在%d到%d之间", fieldName, min, max)
            );
        }
    }
    
    public static void requireArgsLength(String[] args, int min, String commandName) {
        if (args.length < min) {
            throw new IllegalArgumentException(
                String.format(Locale.getDefault(), "参数不足，至少需要%d个参数", min)
            );
        }
    }
}
