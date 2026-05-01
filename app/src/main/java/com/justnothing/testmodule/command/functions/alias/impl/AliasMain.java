package com.justnothing.testmodule.command.functions.alias.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_ALIAS_VER;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.command.handlers.alias.AliasRequestHandler;
import com.justnothing.testmodule.command.functions.alias.request.AliasRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;

import java.io.File;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("alias")
public class AliasMain extends MainCommand<AliasRequest, AliasResult> {
    
    private static AliasManager aliasManager;
    
    public AliasMain() {
        super("Alias", AliasResult.class);
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
    public AliasResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        String[] strippedArgs = context.argGroup().getStrippedArgs();
        
        AliasManager manager = getAliasManager();
        if (manager == null) {
            context.println("错误: 别名管理器未初始化", Colors.RED);
            return null;
        }

        if (args.length == 0) {
            String result = manager.formatAliasList();
            context.println(result, Colors.WHITE);
            return null;
        }
        
        String subCommand = args[0];
        
        switch (subCommand) {
            case "add" -> handleAdd(strippedArgs, manager, context);
            case "remove", "rm", "delete", "del" -> handleRemove(args, manager, context);
            case "list", "ls" -> {
                String result = manager.formatAliasList();
                context.println(result, Colors.WHITE);
            }
            case "clear" -> handleClear(manager, context);
            case "show", "get" -> handleShow(args, manager, context);
            default -> {
                if (args.length == 1) {
                    handleShow(args, manager, context);
                } else {
                    context.println("未知的子命令: " + subCommand, Colors.RED);
                    context.println("使用 'alias' 查看帮助", Colors.GRAY);
                }
            }
        }

        if (shouldReturnStructuredData(context)) {
            AliasRequestHandler handler = new AliasRequestHandler();
            AliasRequest request = new AliasRequest();
            request.setRequestId(java.util.UUID.randomUUID().toString());
            request.setAction(AliasRequest.ACTION_LIST);
            return handler.handle(request);
        }

        return null;
    }
    
    private void handleAdd(String[] args, AliasManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 3) {
            context.println("用法: alias add <名称> <命令>", Colors.GRAY);
            context.println("示例: alias add pm performance", Colors.GRAY);
            return;
        }
        
        String name = args[1];
        
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        
        String command = commandBuilder.toString();
        
        if (manager.addAlias(name, command)) {
            context.println("已添加别名", Colors.GREEN);
            context.print("名称: ", Colors.CYAN);
            context.println(name, Colors.YELLOW);
            context.print("命令: ", Colors.CYAN);
            context.println(command, Colors.GREEN);
        } else {
            context.println("添加别名失败: 名称或命令无效", Colors.RED);
        }
    }
    
    private void handleRemove(String[] args, AliasManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("用法: alias remove <名称>", Colors.GRAY);
            return;
        }
        
        String name = args[1];
        
        if (manager.removeAlias(name)) {
            context.println("已删除别名", Colors.GREEN);
            context.print("名称: ", Colors.CYAN);
            context.println(name, Colors.YELLOW);
        } else {
            context.println("删除失败: 别名 '" + name + "' 不存在", Colors.RED);
        }
    }
    
    private void handleClear(AliasManager manager, CommandExecutor.CmdExecContext context) {
        manager.clearAliases();
        context.println("已清除所有自定义别名（默认别名已恢复）", Colors.GREEN);
    }
    
    private void handleShow(String[] args, AliasManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            String result = manager.formatAliasList();
            context.println(result, Colors.WHITE);
            return;
        }
        
        String name = args[1];
        String command = manager.getAlias(name);
        
        if (command != null) {
            context.print(name, Colors.YELLOW);
            context.print(" -> ", Colors.WHITE);
            context.println(command, Colors.GREEN);
        } else {
            context.println("别名 '" + name + "' 不存在", Colors.RED);
        }
    }
}
