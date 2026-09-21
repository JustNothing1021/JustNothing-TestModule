package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
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
            context.println(CliMessages.ERROR_PREFIX.text() + ThreadsTexts.ERR_FILE_PATH_REQUIRED.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "threads profile export <file>", Colors.GRAY);
            return createErrorResult(ThreadsTexts.ERR_FILE_PATH_REQUIRED.text());
        }

        ProfileManager manager = ProfileManager.getInstance();

        String report = manager.getProfileReport();
        
        if (ThreadsTexts.NO_PROFILE_DATA.text().equals(report)) {
            context.println(Text.zhEn("暂无分析结果可导出", "No profiling results to export").text(), Colors.YELLOW);
            
            ThreadProfileExportResult result = new ThreadProfileExportResult();
            result.setFilePath(filePath);
            result.setSuccess(false);
            return result;
        }

        try {
            boolean success = manager.exportToFile(filePath);

            if (success) {
                context.println(Text.zhEn("分析结果已导出到: %s", "Profiling results exported to: %s").format(filePath), Colors.GREEN);
                
                ThreadProfileExportResult result = new ThreadProfileExportResult();
                result.setFilePath(filePath);
                result.setSuccess(true);
                return result;
            } else {
                context.println(Text.zhEn("导出失败", "Export failed").text(), Colors.RED);
                
                ThreadProfileExportResult result = new ThreadProfileExportResult();
                result.setFilePath(filePath);
                result.setSuccess(false);
                return result;
            }
        } catch (Exception e) {
            context.println(Text.zhEn("导出失败: %s", "Export failed: %s").format(e.getMessage()), Colors.RED);
            
            ThreadProfileExportResult result = new ThreadProfileExportResult();
            result.setFilePath(filePath);
            result.setSuccess(false);
            return result;
        }
    }
}
