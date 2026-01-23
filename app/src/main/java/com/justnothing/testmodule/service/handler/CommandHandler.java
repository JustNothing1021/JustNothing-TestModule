package com.justnothing.testmodule.service.handler;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.StreamOutputWriter;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.functions.CmdUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandHandler {
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "CommandHandler";
        }
    };

    private final CommandExecutor commandExecutor;

    public CommandHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public boolean handleExecuteFile(Parcel data, Parcel reply) {
        String commandLine = null;
        try {
            commandLine = data.readString();
            logger.info("开始处理文件命令: " + (commandLine != null ? commandLine : "null"));

            if (commandLine != null && commandLine.startsWith("FILE:")) {
                String[] parts = commandLine.substring(5).split(":", 2);
                if (parts.length == 2) {
                    String inputFile = parts[0];
                    String outputFile = parts[1];
                    logger.info("输入文件: " + inputFile);
                    logger.info("输出文件: " + outputFile);

                    int result = executeFileMode(inputFile, outputFile);
                    reply.writeNoException();
                    reply.writeInt(result);
                    logger.info("执行文件模式完成，结果码: " + result);
                    return true;
                } else {
                    logger.error("文件模式参数格式错误: " + commandLine);
                }
            } else {
                logger.error("无效的文件模式命令: " + commandLine);
            }

            reply.writeNoException();
            reply.writeInt(1);
            return true;
        } catch (Exception e) {
            logger.error("执行文件模式失败: " + commandLine, e);
            reply.writeException(e);
            return false;
        }
    }

    public boolean handleExecuteStream(Parcel data, Parcel reply) {
        String command = data.readString();

        logger.debug("接收到流式命令: " + command);

        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor readFd = pipe[0];
            ParcelFileDescriptor writeFd = pipe[1];

            OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writeFd);
            IOutputHandler outputWriter = new StreamOutputWriter(outputStream);

            new Thread(() -> {
                try {
                    commandExecutor.execute(command, outputWriter);
                } catch (Exception e) {
                    logger.error("执行命令失败: " + command, e);
                    outputWriter.println(Log.getStackTraceString(e));
                } finally {
                    try {
                        outputWriter.flush();
                    } catch (Exception ignored) {}
                    try {
                        outputWriter.close();
                    } catch (Exception ignored) {}

                    try { writeFd.close(); } catch (IOException ignored) {}
                }
            }, "ShellService-StreamExecutor").start();

            reply.writeNoException();
            reply.writeFileDescriptor(readFd.getFileDescriptor());
            readFd.close();

            return true;

        } catch (Exception e) {
            logger.error("处理流式命令失败", e);
            reply.writeException(e);
            return false;
        }
    }

    public String executeCommand(String commandLine) {
        try {
            logger.debug("执行命令: " + commandLine);
            String result = commandExecutor.executeShellCommand(commandLine);
            logger.debug("命令执行结果长度: " + result.length());
            return result;
        } catch (Exception e) {
            logger.error("执行命令异常: " + commandLine, e);
            return "执行出错: " + e.getMessage();
        }
    }

    private int executeFileMode(String inputFile, String outputFile) {
        logger.info("进入executeFileMode，输入文件: " + inputFile + ", 输出文件: " + outputFile);

        if (inputFile == null || outputFile == null ||
                inputFile.isEmpty() || outputFile.isEmpty()) {
            logger.error("文件模式参数错误");
            return 1;
        }

        File input = new File(inputFile);
        File output = new File(outputFile);

        if (!input.exists()) {
            logger.error("输入文件不存在: " + inputFile);
            return 2;
        }

        String command;
        try {
            java.nio.file.Path inputPath = java.nio.file.Paths.get(inputFile);
            command = new String(java.nio.file.Files.readAllBytes(inputPath), "UTF-8");
            logger.info("从文件读取命令: " + (command.length() > 100 ? command.substring(0, 100) + "..." : command));
        } catch (Exception e) {
            logger.error("读取输入文件时遇到错误: " + inputFile, e);
            return 2;
        }

        if (command.trim().isEmpty()) {
            logger.error("输入文件为空");
            return 1;
        }

        logger.info("开始执行命令: " + command.trim());
        logger.debug("命令长度: " + command.trim().length() + " 字符");

        String result;
        long startTime = System.currentTimeMillis();
        try {
            logger.debug("调用CommandExecutor执行命令");
            result = commandExecutor.executeShellCommand(command.trim());
            long endTime = System.currentTimeMillis();
            logger.info("命令执行完成，执行时间: " + (endTime - startTime) + "ms, 结果长度: " + result.length() + " 字符");
            logger.debug("结果内容: " + result);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            result = "命令执行时出现错误: \n" + e.getMessage();
            logger.error("执行命令时出现错误，执行时间: " + (endTime - startTime) + "ms", e);
            logger.error("错误详细信息: " + e.getClass().getName() + ": " + e.getMessage());
        }

        File outputParentDir = output.getParentFile();
        if (outputParentDir != null && !outputParentDir.exists()) {
            logger.debug("创建输出文件目录: " + outputParentDir.getAbsolutePath());
            if (!outputParentDir.mkdirs()) {
                logger.error("创建输出文件目录失败: " + outputParentDir.getAbsolutePath());
                return 2;
            }
            
            try {
                DataDirectoryManager.setFilePermissions(outputParentDir, "777", "输出文件目录");
            } catch (Exception e) {
                logger.warn("设置输出文件目录权限异常: " + outputParentDir.getAbsolutePath() + ", " + e.getMessage());
            }
        }

        try {
            logger.info("将结果写入文件: " + outputFile + ", 长度: " + result.length());
            logger.debug("结果内容: " + result);
            
            Path outputPath = Paths.get(outputFile);
            Files.write(outputPath, result.getBytes(StandardCharsets.UTF_8));
            
            logger.debug("文件写入完成，设置文件权限");
            
            DataDirectoryManager.setFilePermissions(new File(outputFile), "644", "输出文件");

            logger.info("文件写入完成，验证文件内容");
            
            long fileSize = java.nio.file.Files.size(outputPath);
            logger.info("文件验证成功，文件大小: " + fileSize + " 字节");
            
            return 0;
        } catch (Exception e) {
            logger.error("写入文件失败: " + outputFile, e);
            return 2;
        }
    }
}
