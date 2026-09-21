package com.justnothing.testmodule.command.functions.watch.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.watch.util.WatchManager;
import com.justnothing.testmodule.command.functions.watch.request.WatchAddRequest;
import com.justnothing.testmodule.command.functions.watch.response.WatchAddResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

import java.util.HashMap;
import java.util.Map;

@SubCommandInfo(
    description = WatchTexts.SUB_WATCH_ADD_DESC,
    usage = "watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval: ms]",
    examples = {
        "watch add field java.lang.System out 1000",
        "watch add method com.example.MyClass myMethod signature String 500"
    }
)
public class WatchAddCommand extends AbstractWatchCommand<WatchAddRequest, WatchAddResult> {

    public WatchAddCommand() {
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
            context.println(CliMessages.ERROR_PREFIX.text() + CliMessages.ERR_NOT_ENOUGH_ARGS.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text()
                    + "watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval]", Colors.GRAY);
            return createErrorResult(CliMessages.ERR_NOT_ENOUGH_ARGS.text());
        }

        if (!"field".equals(targetType) && !"method".equals(targetType)) {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn(
                    "未知类型: %s，必须是 'field' 或 'method'",
                    "unknown type: %s; must be 'field' or 'method'").format(targetType), Colors.RED);
            return createErrorResult(CliMessages.ERR_UNKNOWN_TYPE.format(targetType));
        }

        if (interval != null && interval < 10) {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn(
                    "间隔过小，最小10ms",
                    "interval too small; minimum is 10ms").text(), Colors.RED);
            context.println(Text.zhEn(
                    "(指定的是%sms, 频率过高容易炸掉系统)",
                    "(specified %sms - such a high rate can easily blow up the system)").format(interval), Colors.YELLOW);
            return createErrorResult(Text.zhEn(
                    "间隔过小: %sms",
                    "interval too small: %sms").format(interval));
        }

        try {
            int id;
            if ("field".equals(targetType)) {
                id = manager.addFieldWatch(classLoader, className, memberName, interval != null ? interval : 1000L);
                
                context.println(Text.zhEn("字段watch任务已添加", "Field watch task added").text(), Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print(CliMessages.LABEL_CLASS.text(), Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print(CliMessages.LABEL_FIELD.text(), Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                context.print(WatchTexts.LABEL_INTERVAL.text(), Colors.CYAN);
                context.println((interval != null ? interval : 1000L) + "ms", Colors.YELLOW);
                context.println(WatchTexts.HINT_VIEW_OUTPUT.format(id), Colors.GRAY);

                WatchAddResult result = new WatchAddResult();
                result.setTaskId(id);
                result.setTargetType(targetType);
                result.setClassName(className);
                result.setMemberName(memberName);
                result.setInterval(interval != null ? interval : 1000L);
                return result;
            } else {
                id = manager.addMethodWatch(classLoader, className, memberName, signature, interval != null ? interval : 1000L);

                context.println(Text.zhEn("方法watch任务已添加", "Method watch task added").text(), Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print(CliMessages.LABEL_CLASS.text(), Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print(CliMessages.LABEL_METHOD.text(), Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                if (signature != null) {
                    context.print(Text.zhEn("签名: ", "Signature: ").text(), Colors.CYAN);
                    context.println(signature, Colors.GRAY);
                }
                context.print(WatchTexts.LABEL_INTERVAL.text(), Colors.CYAN);
                context.println((interval != null ? interval : 1000L) + "ms", Colors.YELLOW);
                context.println(WatchTexts.HINT_VIEW_OUTPUT.format(id), Colors.GRAY);

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
            errorContext.put(Text.zhEn("类型", "Type").text(), targetType);
            errorContext.put(CliMessages.CONTEXT_CLASS_NAME.text(), className);
            errorContext.put(Text.zhEn("成员名", "Member").text(), memberName);
            errorContext.put(CliMessages.CONTEXT_SIGNATURE.text(),
                    signature != null ? signature : CliMessages.VALUE_NONE.text());
            errorContext.put(Text.zhEn("间隔", "Interval").text(), interval + "ms");
            
            CommandExceptionHandler.handleException("watch add", e, context, errorContext,
                    Text.zhEn("添加watch任务失败", "Failed to add watch task").text());
            throw e;
        }
    }
}
