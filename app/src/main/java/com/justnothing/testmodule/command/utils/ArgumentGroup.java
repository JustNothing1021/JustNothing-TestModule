package com.justnothing.testmodule.command.utils;

import java.util.ArrayList;
import java.util.List;

public class ArgumentGroup {
    private final String[] rawArgs;
    private final String[] strippedArgs;
    
    public ArgumentGroup(String[] rawArgs, String[] strippedArgs) {
        this.rawArgs = rawArgs;
        this.strippedArgs = strippedArgs;
    }
    
    public String[] getRawArgs() {
        return rawArgs;
    }
    
    public String[] getStrippedArgs() {
        return strippedArgs;
    }
    
    public String getRaw(int index) {
        if (index < 0 || index >= rawArgs.length) {
            return null;
        }
        return rawArgs[index];
    }
    
    public String getStripped(int index) {
        if (index < 0 || index >= strippedArgs.length) {
            return null;
        }
        return strippedArgs[index];
    }
    
    public int length() {
        return rawArgs.length;
    }
    
    public static ArgumentGroup parse(String cmdline) {
        if (cmdline == null || cmdline.trim().isEmpty()) {
            return new ArgumentGroup(new String[0], new String[0]);
        }
        
        List<String> rawArgs = new ArrayList<>();
        List<String> strippedArgs = new ArrayList<>();
        
        StringBuilder current = new StringBuilder();
        StringBuilder stripped = new StringBuilder();
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
                        stripped.append(next);
                        current.append(next);
                        i++;
                        continue;
                    }
                }
                
                if (c == quoteChar) {
                    if (inQuotes) inQuotes = false;
                    else if (inSingleQuotes) inSingleQuotes = false;
                } else {
                    stripped.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = (c == '"');
                    inSingleQuotes = (c == '\'');
                    quoteChar = c;
                    current.append(c);
                } else if (c == ' ' || c == '\t') {
                    if (current.length() > 0) {
                        rawArgs.add(current.toString());
                        strippedArgs.add(stripped.toString());
                        current.setLength(0);
                        stripped.setLength(0);
                    }
                } else {
                    current.append(c);
                    stripped.append(c);
                }
            }
        }
        
        if (current.length() > 0) {
            rawArgs.add(current.toString());
            strippedArgs.add(stripped.toString());
        }
        
        return new ArgumentGroup(
            rawArgs.toArray(new String[0]),
            strippedArgs.toArray(new String[0])
        );
    }
}
