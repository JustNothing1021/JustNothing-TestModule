package com.justnothing.testmodule.command.functions.memory.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.memory.DumpRequest;
import com.justnothing.testmodule.command.functions.memory.DumpResult;
import com.justnothing.testmodule.command.functions.memory.MemoryUtils;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SubCommandInfo(
    description = "?????????????????",
    usage = "memory dump [options] [file]",
    examples = {
        "memory dump",
        "memory dump /sdcard/heap_dump.txt",
        "memory dump --heap /sdcard/heap_only.txt",
        "memory dump --full /sdcard/full_dump.txt"
    },
    optionsDesc = """
            ??:
              --heap            - ??????
              --threads         - ???????
              --full            - ?????? (??)
              
            ??:
              file              - ?????? (???????????)
            """
)
public class DumpCommand extends MemorySubCommand<DumpRequest, DumpResult> {

    public DumpCommand() {
        super("memory dump", DumpRequest.class, DumpResult.class);
    }

    @Override
    protected DumpResult executeMemoryCommand(DumpRequest request) throws Exception {
        DumpResult result = new DumpResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        StringBuilder output = new StringBuilder();

        output.append("=== ????? ===\n");
        output.append("??: ").append(new Date()).append("\n");

        if (request.getFilePath() != null) {
            output.append("??: ").append(request.getFilePath()).append("\n\n");
        } else {
            output.append("\n");
        }

        if (request.isHeapOnly() || request.isFullDump()) {
            appendHeapInfo(output);
        }

        if (request.isThreadsOnly() || request.isFullDump()) {
            appendThreadInfo(output);
        }

        if (request.isFullDump()) {
            appendSystemInfo(output);
        }

        result.setDumpContent(output.toString());

        if (request.getFilePath() != null) {
            File outputFile = new File(request.getFilePath());
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                IOManager.createDirectory(parentDir.getAbsolutePath());
            }

            try {
                logger.info("????????: " + request.getFilePath());
                IOManager.writeFile(outputFile.getAbsolutePath(), output.toString());
                logger.info("???????");

                context.print("???????: ", Colors.LIGHT_GREEN);
                context.println(request.getFilePath(), Colors.CYAN);

                result.setFilePath(request.getFilePath());
            } catch (IOException e) {
                logger.error("???????", e);
                context.print("??: ", Colors.RED);
                context.println(e.getMessage() != null ? e.getMessage() : "????", Colors.YELLOW);
                result.setSuccess(false);
                return result;
            }
        } else {
            context.println("=== ????? ===", Colors.CYAN);
            context.print("??: ", Colors.GRAY);
            context.println(new Date().toString(), Colors.YELLOW);
            context.println("");

            if (request.isHeapOnly() || request.isFullDump()) {
                dumpHeapInfoColored();
            }

            if (request.isThreadsOnly() || request.isFullDump()) {
                dumpThreadInfoColored();
            }

            if (request.isFullDump()) {
                dumpSystemInfoColored();
            }
        }

        result.setSuccess(true);
        return result;
    }

    private void appendHeapInfo(StringBuilder output) {
        output.append("=== ????? ===\n\n");

        Runtime runtime = Runtime.getRuntime();
        output.append("Java?????:\n");
        output.append("  ??: ").append(MemoryUtils.formatBytes(runtime.maxMemory())).append("\n");
        output.append("  ???: ").append(MemoryUtils.formatBytes(runtime.totalMemory())).append("\n");
        output.append("  ??: ").append(MemoryUtils.formatBytes(runtime.freeMemory())).append("\n");
        output.append("  ??: ").append(MemoryUtils.formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n\n");

        output.append("?????:\n");
        output.append("  ???: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapAllocatedSize())).append("\n");
        output.append("  ??: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapSize())).append("\n");
        output.append("  ??: ").append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapFreeSize())).append("\n\n");

        output.append("=== ?????? ===\n\n");
        output.append(readMeminfo());
    }

    private void appendThreadInfo(StringBuilder output) {
        output.append("=== ???? ===\n\n");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        output.append("????: ").append(allStackTraces.size()).append("\n\n");

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();

            output.append("??: ").append(thread.getName()).append("\n");
            output.append("  ID: ").append(thread.getId()).append("\n");
            output.append("  ??: ").append(thread.getState()).append("\n\n");
        }
    }

    private void appendSystemInfo(StringBuilder output) {
        output.append("=== ???? ===\n\n");

        output.append("????: ").append(System.getProperty("os.name")).append("\n");
        output.append("????: ").append(System.getProperty("os.version")).append("\n");
        output.append("??: ").append(System.getProperty("os.arch")).append("\n\n");
    }
}
