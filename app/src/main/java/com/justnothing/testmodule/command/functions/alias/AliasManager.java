package com.justnothing.testmodule.command.functions.alias;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AliasManager {
    private static final Logger logger = Logger.getLoggerForName("AliasManager");
    
    private static final int MAX_ALIAS_COUNT = 100;
    
    private static AliasManager instance;
    
    private final File aliasFile;
    private final Map<String, String> aliases;
    
    private AliasManager(File dataDir) {
        this.aliasFile = new File(dataDir, "command_aliases.txt");
        this.aliases = new ConcurrentHashMap<>();
        loadAliases();
        
        initDefaultAliases();
    }
    
    public static synchronized AliasManager getInstance(File dataDir) {
        if (instance == null) {
            instance = new AliasManager(dataDir);
        }
        return instance;
    }
    
    private void initDefaultAliases() {
        addDefaultAlias("h", "help");
        addDefaultAlias("?", "help");
        addDefaultAlias("pm", "performance");
        addDefaultAlias("sc", "script");
        addDefaultAlias("tr", "trace");
        addDefaultAlias("wt", "watch");
        addDefaultAlias("bp", "breakpoint");
        addDefaultAlias("cls", "clear");
    }
    
    private void addDefaultAlias(String name, String command) {
        if (!aliases.containsKey(name)) {
            aliases.put(name, command);
        }
    }
    
    public boolean addAlias(String name, String command) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        name = name.trim();
        command = command.trim();
        
        if (name.contains(" ") || name.contains("\t")) {
            return false;
        }
        
        if (aliases.size() >= MAX_ALIAS_COUNT && !aliases.containsKey(name)) {
            logger.warn("别名数量已达上限: " + MAX_ALIAS_COUNT);
            return false;
        }
        
        aliases.put(name, command);
        saveAliasesAsync();
        logger.info("添加别名: " + name + " -> " + command);
        return true;
    }
    
    public boolean removeAlias(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        name = name.trim();
        
        if (aliases.remove(name) != null) {
            saveAliasesAsync();
            logger.info("删除别名: " + name);
            return true;
        }
        
        return false;
    }
    
    public String getAlias(String name) {
        return aliases.get(name);
    }

    @SuppressWarnings("unused")
    public boolean hasAlias(String name) {
        return aliases.containsKey(name);
    }
    
    public String resolveAlias(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String trimmed = input.trim();
        int spaceIndex = trimmed.indexOf(' ');
        
        String firstWord;
        String rest = "";
        
        if (spaceIndex > 0) {
            firstWord = trimmed.substring(0, spaceIndex);
            rest = trimmed.substring(spaceIndex);
        } else {
            firstWord = trimmed;
        }
        
        String resolved = aliases.get(firstWord);
        if (resolved != null) {
            return resolved + rest;
        }
        
        return input;
    }

    @SuppressWarnings("unused")
    public Map<String, String> getAllAliases() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
    }

    @SuppressWarnings("unused")
    public List<AliasEntry> getAliasList() {
        List<AliasEntry> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            list.add(new AliasEntry(entry.getKey(), entry.getValue()));
        }
        return list;
    }
    
    public void clearAliases() {
        aliases.clear();
        initDefaultAliases();
        saveAliasesAsync();
        logger.info("清除所有别名");
    }
    
    public int size() {
        return aliases.size();
    }
    
    private void loadAliases() {
        if (!aliasFile.exists()) {
            logger.info("别名文件不存在，跳过加载");
            return;
        }
        
        try {
            String content = IOManager.readFile(aliasFile.getAbsolutePath());
            if (content == null || content.isEmpty()) {
                return;
            }
            
            int count = 0;
            for (String line : content.split("\n")) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String name = line.substring(0, eqIndex).trim();
                    String command = line.substring(eqIndex + 1).trim();
                    
                    if (!name.isEmpty() && !command.isEmpty()) {
                        aliases.put(name, command);
                        count++;
                    }
                }
            }
            
            logger.info("加载了 " + count + " 个别名");
            
        } catch (IOException e) {
            logger.error("加载别名文件失败", e);
        }
    }
    
    private void saveAliasesAsync() {
        new Thread(() -> {
            synchronized (aliases) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("# Command Aliases\n");
                    sb.append("# Format: name=command\n");
                    sb.append("# Example: pm=performance\n\n");
                    
                    for (Map.Entry<String, String> entry : aliases.entrySet()) {
                        sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                    }
                    
                    IOManager.writeFile(aliasFile.getAbsolutePath(), sb.toString());
                } catch (IOException e) {
                    logger.error("保存别名文件失败", e);
                }
            }
        }, "AliasSaver").start();
    }
    
    public String formatAliasList() {
        if (aliases.isEmpty()) {
            return "没有定义别名";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("命令别名列表:\n");
        sb.append("────────────────────────────────────────\n");
        
        List<String> names = new ArrayList<>(aliases.keySet());
        Collections.sort(names);
        
        int maxNameLen = 0;
        for (String name : names) {
            maxNameLen = Math.max(maxNameLen, name.length());
        }
        
        for (String name : names) {
            String command = aliases.get(name);
            sb.append(String.format(Locale.getDefault(), "  %-" + maxNameLen + "s  ->  %s\n", name, command));
        }
        
        sb.append("────────────────────────────────────────\n");
        sb.append("共 ").append(aliases.size()).append(" 个别名");
        
        return sb.toString();
    }

    public record AliasEntry(String name, String command) {

        @NonNull
            @Override
            public String toString() {
                return name + " -> " + command;
            }
        }
}
