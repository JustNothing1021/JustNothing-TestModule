package com.justnothing.testmodule.command.functions.profile;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PROFILE_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class ProfileMain extends CommandBase {

    public ProfileMain() {
        super("ProfileMain");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: profile <subcmd> [args...]
                
                分析各个应用各个线程的资源占用，非阻塞执行.
                
                子命令:
                    start [duration: seconds]  - 开始性能分析，默认60秒
                    stop                        - 停止当前分析
                    show                        - 显示分析结果
                    export <file>              - 导出分析结果到文件
                
                选项:
                    duration    - 分析持续时间（秒），默认60秒
                
                示例:
                    profile start
                    profile start 120
                    profile stop
                    profile show
                    profile export /sdcard/profile_report.txt
                
                注意:
                    - 性能分析会监控CPU、内存、线程等资源使用情况
                    - 分析结果包含各个应用和线程的资源占用统计
                    - 分析任务在后台运行，不会阻塞其他命令执行
                    - 建议分析时间不要太长，避免影响系统性能

                
                (Submodule profile %s)
                """, CMD_PROFILE_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        logger.debug("执行profile命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];
        ProfileManager manager = ProfileManager.getInstance();

        try {
            switch (subCommand) {
                case "start":
                    return handleStart(args, manager);
                case "stop":
                    return handleStop(manager);
                case "show":
                    return handleShow(manager);
                case "export":
                    return handleExport(args, manager);
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("执行profile命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleStart(String[] args, ProfileManager manager) {
        int duration = 60;
        
        if (args.length > 1) {
            try {
                duration = Integer.parseInt(args[1]);
                if (duration <= 0) {
                    return "错误: 持续时间必须大于0";
                }
            } catch (NumberFormatException e) {
                return "错误: 无效的持续时间: " + args[1];
            }
        }

        try {
            manager.startProfiling(duration);
            return "开始性能分析，持续时间: " + duration + "秒";
        } catch (Exception e) {
            logger.error("开始性能分析失败", e);
            return "开始性能分析失败: " + e.getMessage();
        }
    }

    private String handleStop(ProfileManager manager) {
        try {
            manager.stopProfiling();
            return "停止性能分析成功";
        } catch (Exception e) {
            logger.error("停止性能分析失败", e);
            return "停止性能分析失败: " + e.getMessage();
        }
    }

    private String handleShow(ProfileManager manager) {
        try {
            return manager.getProfileReport();
        } catch (Exception e) {
            logger.error("获取分析结果失败", e);
            return "获取分析结果失败: " + e.getMessage();
        }
    }

    private String handleExport(String[] args, ProfileManager manager) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: profile export <file>";
        }

        try {
            String filePath = args[1];
            boolean success = manager.exportToFile(filePath);
            if (success) {
                return "导出分析结果成功: " + filePath;
            } else {
                return "导出分析结果失败";
            }
        } catch (Exception e) {
            logger.error("导出分析结果失败", e);
            return "导出分析结果失败: " + e.getMessage();
        }
    }
}
