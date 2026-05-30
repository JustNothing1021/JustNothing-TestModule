package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.watch.AbstractWatchCommand;
import com.justnothing.testmodule.command.functions.watch.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchAddRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchAddResult;

import java.util.HashMap;
import java.util.Map;

@SubCommandInfo(
    description = "添加字段或方法监控任务",
    usage = "watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval: ms]",
    examples = {
        "watch add field java.lang.System out 1000",
        "watch add method com.example.MyClass myMethod signature String 500"
    }
)
public class AddCommand extends AbstractWatchCommand<WatchAddRequest, WatchAddResult> {

    public AddCommand() {
        super("watch add", WatchAddRequest.class, WatchAddResult.class);
    }

    @Override
    protected WatchAddResult executeWatchCommand(CommandExecutor.CmdExecContext<WatchAddRequest> context) throws Exception {
        WatchAddRequest request = context.getCommandRequest();
        ClassLoader classLoader = context.classLoader();
        WatchManager manager = WatchManager.getInstance();

        String targetType = request.getTargetType();
        String className = request.getClassName();
        String memberName = request.getMemberName();
        String signature = request.getSignature();
        Long interval = request.getInterval();

        if (targetType == null || className == null || memberName == null) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval]", Colors.GRAY);
            return createErrorResult("参数不足");
        }

        if (!"field".equals(targetType) && !"method".equals(targetType)) {
            context.println("错误: 未知类型: " + targetType + "，必须是 'field' 或 'method'", Colors.RED);
            return createErrorResult("未知类型: " + targetType);
        }

        if (interval != null && interval < 10) {
            context.println("错误: 间隔过小，最小10ms", Colors.RED);
            context.println("(指定的是" + interval + "ms, 频率过高容易炸掉系统)", Colors.YELLOW);
            return createErrorResult("间隔过小: " + interval + "ms");
        }

        try {
            int id;
            if ("field".equals(targetType)) {
                id = manager.addFieldWatch(classLoader, className, memberName, interval != null ? interval : 1000L);
                
                context.println("字段watch任务已添加", Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print("类: ", Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print("字段: ", Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                context.print("间隔: ", Colors.CYAN);
                context.println((interval != null ? interval : 1000L) + "ms", Colors.YELLOW);
                context.println("提示: 使用 'watch output " + id + "' 查看输出", Colors.GRAY);

                WatchAddResult result = new WatchAddResult();
                result.setTaskId(id);
                result.setTargetType(targetType);
                result.setClassName(className);
                result.setMemberName(memberName);
                result.setInterval(interval != null ? interval : 1000L);
                return result;
            } else {
                id = manager.addMethodWatch(classLoader, className, memberName, signature, interval != null ? interval : 1000L);

                context.println("方法watch任务已添加", Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print("类: ", Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print("方法: ", Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                if (signature != null) {
                    context.print("签名: ", Colors.CYAN);
                    context.println(signature, Colors.GRAY);
                }
                context.print("间隔: ", Colors.CYAN);
                context.println((interval != null ? interval : 1000L) + "ms", Colors.YELLOW);
                context.println("提示: 使用 'watch output " + id + "' 查看输出", Colors.GRAY);

                WatchAddResult result = new WatchAddResult();
                result.setTaskId(id);
                result.setTargetType(targetType);
                result.setClassName(className);
                result.setMemberName(memberName);
                result.setSignature(signature);
                result.setInterval(interval != null ? interval : 1000L);
                return result;
            }
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("类型", targetType);
            errorContext.put("类名", className);
            errorContext.put("成员名", memberName);
            errorContext.put("签名", signature != null ? signature : "无");
            errorContext.put("间隔", interval + "ms");
            
            CommandExceptionHandler.handleException("watch add", e, context, errorContext, "添加watch任务失败");
            throw e;
        }
    }
}
