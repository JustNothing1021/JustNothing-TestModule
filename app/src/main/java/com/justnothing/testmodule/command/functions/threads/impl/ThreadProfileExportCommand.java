package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.util.ProfileManager;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileExportRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileExportResult;

@SubCommandInfo(
    description = ThreadsTexts.SUB_THREADS_PROFILE_EXPORT_DESC,
    usage = "threads profile export <file>",
    examples = {"threads profile export /sdcard/profile_report.txt"}
)
public class ThreadProfileExportCommand extends AbstractThreadsCommand<ThreadProfileExportRequest, ThreadProfileExportResult> {

    public ThreadProfileExportCommand() {
        super("threads profile export", ThreadProfileExportRequest.class, ThreadProfileExportResult.class);
    }

    @Override
    protected ThreadProfileExportResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadProfileExportRequest> context) throws Exception {
        ThreadProfileExportRequest request = context.getCommandRequest();
        String filePath = request.getFilePath();

        if (filePath == null || filePath.isEmpty()) {
            context.println("错误: 需要指定文件路径", Colors.RED);
            context.println("用法: threads profile export <file>", Colors.GRAY);
            return createErrorResult("需要指定文件路径");
        }

        ProfileManager manager = ProfileManager.getInstance();

        String report = manager.getProfileReport();
        
        if ("暂无性能分析数据".equals(report)) {
            context.println("暂无分析结果可导出", Colors.YELLOW);
            
            ThreadProfileExportResult result = new ThreadProfileExportResult();
            result.setFilePath(filePath);
            result.setSuccess(false);
            return result;
        }

        try {
            boolean success = manager.exportToFile(filePath);

            if (success) {
                context.println("分析结果已导出到: " + filePath, Colors.GREEN);
                
                ThreadProfileExportResult result = new ThreadProfileExportResult();
                result.setFilePath(filePath);
                result.setSuccess(true);
                return result;
            } else {
                context.println("导出失败", Colors.RED);
                
                ThreadProfileExportResult result = new ThreadProfileExportResult();
                result.setFilePath(filePath);
                result.setSuccess(false);
                return result;
            }
        } catch (Exception e) {
            context.println("导出失败: " + e.getMessage(), Colors.RED);
            
            ThreadProfileExportResult result = new ThreadProfileExportResult();
            result.setFilePath(filePath);
            result.setSuccess(false);
            return result;
        }
    }
}
