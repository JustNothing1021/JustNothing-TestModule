package com.justnothing.testmodule.command.functions.threads.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.util.ProfileManager;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileShowRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileShowResult;

@SubCommandInfo(
    description = ThreadsTexts.SUB_THREADS_PROFILE_SHOW_DESC,
    usage = "threads profile show",
    examples = {"threads profile show"}
)
public class ThreadProfileShowCommand extends AbstractThreadsCommand<ThreadProfileShowRequest, ThreadProfileShowResult> {

    public ThreadProfileShowCommand() {
        super("threads profile show", ThreadProfileShowRequest.class, ThreadProfileShowResult.class);
    }

    @Override
    protected ThreadProfileShowResult executeThreadsCommand(CommandExecutor.CmdExecContext<ThreadProfileShowRequest> context) throws Exception {
        ProfileManager manager = ProfileManager.getInstance();

        String report = manager.getProfileReport();

        if (ThreadsTexts.NO_PROFILE_DATA.text().equals(report)) {
            context.println(Text.zhEn("暂无分析结果，请先执行 'threads profile start'",
                    "No profiling results yet; run 'threads profile start' first").text(), Colors.YELLOW);
            
            ThreadProfileShowResult result = new ThreadProfileShowResult();
            result.setProfiling(false);
            return result;
        }

        context.println(report, Colors.WHITE);

        ThreadProfileShowResult result = new ThreadProfileShowResult();
        result.setProfiling(true);
        result.setProfileData(report);

        return result;
    }
}
