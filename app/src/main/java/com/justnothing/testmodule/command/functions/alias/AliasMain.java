package com.justnothing.testmodule.command.functions.alias;

import static com.justnothing.testmodule.constants.CommandServer.CMD_ALIAS_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;

import java.io.File;

public class AliasMain extends CommandBase {
    
    private static AliasManager aliasManager;
    
    public AliasMain() {
        super("Alias");
    }
    
    private static synchronized AliasManager getAliasManager() {
        if (aliasManager == null) {
            String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
            if (dataDir != null) {
                aliasManager = AliasManager.getInstance(new File(dataDir));
            }
        }
        return aliasManager;
    }
    
    public static String resolveAlias(String input) {
        AliasManager manager = getAliasManager();
        if (manager != null) {
            return manager.resolveAlias(input);
        }
        return input;
    }
    
    @Override
    public String getHelpText() {
        return String.format("""
                语法: alias <子命令> [参数]
                
                管理命令别名，简化命令输入。
                
                子命令:
                    add <名称> <命令>    - 添加别名
                    remove <名称>        - 删除别名
                    list                 - 列出所有别名
                    clear                - 清除所有自定义别名
                    show <名称>          - 显示别名对应的命令
                
                示例:
                    alias add pm performance       - 添加别名: pm -> performance
                    alias add hi "help interactive" - 添加带参数的别名
                    alias add sr "script run"      - 添加子命令别名
                    alias remove pm                - 删除别名 pm
                    alias list                     - 列出所有别名
                    alias show pm                  - 显示 pm 对应的命令
                    alias clear                    - 清除所有自定义别名
                
                使用别名:
                    pm sample start        - 等同于 performance sample start
                    sc run myscript        - 等同于 script run myscript
                    hi                     - 等同于 help interactive
                
                默认别名:
                    h, ?  -> help
                    pm    -> performance
                    sc    -> script
                    tr    -> trace
                    wt    -> watch
                    bp    -> breakpoint
                    cls   -> clear
                
                注意:
                    - 别名名称不能包含空格
                    - 别名会自动保存，重启后保留
                    - 最多支持100个别名
                    - 别名可以包含子命令和参数
                
                (Submodule alias %s)
                """, CMD_ALIAS_VER);
    }
    
    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        String[] strippedArgs = context.argGroup().getStrippedArgs();
        
        AliasManager manager = getAliasManager();
        if (manager == null) {
            return "错误: 别名管理器未初始化";
        }
        
        if (args.length == 0) {
            return manager.formatAliasList();
        }
        
        String subCommand = args[0];
        
        return switch (subCommand) {
            case "add" -> handleAdd(strippedArgs, manager);
            case "remove", "rm", "delete", "del" -> handleRemove(args, manager);
            case "list", "ls" -> manager.formatAliasList();
            case "clear" -> handleClear(manager);
            case "show", "get" -> handleShow(args, manager);
            default -> {
                if (args.length == 1) {
                    yield handleShow(args, manager);
                } else {
                    yield "未知的子命令: " + subCommand + "\n使用 'alias' 查看帮助";
                }
            }
        };
    }
    
    private String handleAdd(String[] args, AliasManager manager) {
        if (args.length < 3) {
            return "用法: alias add <名称> <命令>\n示例: alias add pm performance";
        }
        
        String name = args[1];
        
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        
        String command = commandBuilder.toString();
        
        if (manager.addAlias(name, command)) {
            return "已添加别名: " + name + " -> " + command;
        } else {
            return "添加别名失败: 名称或命令无效";
        }
    }
    
    private String handleRemove(String[] args, AliasManager manager) {
        if (args.length < 2) {
            return "用法: alias remove <名称>";
        }
        
        String name = args[1];
        
        if (manager.removeAlias(name)) {
            return "已删除别名: " + name;
        } else {
            return "删除失败: 别名 '" + name + "' 不存在";
        }
    }
    
    private String handleClear(AliasManager manager) {
        manager.clearAliases();
        return "已清除所有自定义别名（默认别名已恢复）";
    }
    
    private String handleShow(String[] args, AliasManager manager) {
        if (args.length < 2) {
            return manager.formatAliasList();
        }
        
        String name = args[1];
        String command = manager.getAlias(name);
        
        if (command != null) {
            return name + " -> " + command;
        } else {
            return "别名 '" + name + "' 不存在";
        }
    }
}
