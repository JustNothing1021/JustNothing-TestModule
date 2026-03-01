package com.justnothing.testmodule.utils.functions;

import androidx.annotation.NonNull;
import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.RootProcessPool;

public class CmdUtils {

    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final long THREAD_JOIN_TIMEOUT_MS = 5000;


    public record CommandOutput(int stat, String stdout, String stderr, String finalOutput) {

        public boolean succeed() {
                return stat == 0;
            }

            @NonNull
            @Override
            public String toString() {
                return "CommandOutput(stat=" + stat +
                        ", stdout=" + stdout +
                        ", stderr=" + stderr +
                        ", mixed=" + finalOutput + ")";
            }
        }


    private static class OutputCollector {
        private final StringBuilder stdoutBuf = new StringBuilder();
        private final StringBuilder stderrBuf = new StringBuilder();
        private final StringBuilder mixedBuf = new StringBuilder();
        private final Object mixedLock = new Object();


        public void appendStdout(String line) {
            if (line == null) return;
            stdoutBuf.append(line).append('\n');
            synchronized (mixedLock) {
                mixedBuf.append(line).append('\n');
            }
        }


        public void appendStderr(String line) {
            if (line == null) return;
            stderrBuf.append(line).append('\n');
            synchronized (mixedLock) {
                mixedBuf.append(line).append('\n');
            }
        }

        public String getStdout() { return stdoutBuf.toString().trim(); }
        public String getStderr() { return stderrBuf.toString().trim(); }
        public String getMixedOutput() { return mixedBuf.toString().trim(); }
    }

    private static Thread readStreamToHandler(final InputStream is,
                                              final LineHandler handler,
                                              final String streamType) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("stdout".equals(streamType)) {
                        handler.onStdoutLine(line);
                    } else {
                        handler.onStderrLine(line);
                    }
                }
            } catch (IOException e) {
                handler.onStderrLine("读取" + streamType + "流时发生IO异常: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }


    public interface LineHandler {
        default void onStdoutLine(String line) {}
        default void onStderrLine(String line) {}
    }


    public static CommandOutput runCommand(String command) throws IOException, InterruptedException {
        return runCommandInternal(command, null, DEFAULT_TIMEOUT_MS);
    }

    public static CommandOutput runCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        return runCommandInternal(command, null, timeoutMs);
    }


    public static CommandOutput runRootCommand(String command) throws IOException, InterruptedException {
        return runRootCommand(command, DEFAULT_TIMEOUT_MS);
    }

    public static CommandOutput runRootCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        IOManager.ProcessResult result = RootProcessPool.executeCommand(command, timeoutMs, true);
        return new CommandOutput(result.exitCode(), result.stdout(), result.stderr(), result.stdout() + result.stderr());
    }

    /**
     * 内部实现, 支持通过Process启动或命令行启动。
     */
    private static CommandOutput runCommandInternal(String cmdArray, Process existingProcess, long timeoutMs)
            throws IOException, InterruptedException {
        Process process = existingProcess;
        if (process == null) {
            IOManager.ProcessResult result = RootProcessPool.executeCommand(cmdArray, timeoutMs, false);
            return new CommandOutput(result.exitCode(), result.stdout(), result.stderr(), result.stdout() + result.stderr());
        }

        final OutputCollector collector = new OutputCollector();

        LineHandler handler = new LineHandler() {
            @Override
            public void onStdoutLine(String line) {
                collector.appendStdout(line);
            }
            @Override
            public void onStderrLine(String line) {
                collector.appendStderr(line);
            }
        };

        Thread stdoutThread = readStreamToHandler(process.getInputStream(), handler, "stdout");
        Thread stderrThread = readStreamToHandler(process.getErrorStream(), handler, "stderr");

        int exitCode;
        Process finalProcess = process;
        try {
            if (timeoutMs > 0) {
                Future<Integer> exitCodeFuture = ThreadPoolManager.submitIOCallable(() -> {
                    try {
                        return finalProcess.waitFor();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                });
                
                try {
                    assert exitCodeFuture != null;
                    exitCode = exitCodeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    exitCodeFuture.cancel(true);
                    process.destroyForcibly();
                    throw new InterruptedException("命令执行超时 (" + timeoutMs + "ms)");
                } catch (ExecutionException e) {
                    exitCodeFuture.cancel(true);
                    process.destroyForcibly();
                    throw new InterruptedException("命令执行异常: " + e.getMessage());
                }
            } else {
                exitCode = process.waitFor();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }

        joinThreadQuietly(stdoutThread, THREAD_JOIN_TIMEOUT_MS);
        joinThreadQuietly(stderrThread, THREAD_JOIN_TIMEOUT_MS);

        process.destroy();

        return new CommandOutput(
                exitCode,
                collector.getStdout(),
                collector.getStderr(),
                collector.getMixedOutput()
        );
    }

    private static void joinThreadQuietly(Thread thread, long timeoutMs) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join(timeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static int runCommandWithCallback(String command, LineHandler lineHandler)
            throws IOException, InterruptedException {
        return runCommandWithCallback(command, lineHandler, DEFAULT_TIMEOUT_MS);
    }

    public static int runCommandWithCallback(String command, LineHandler lineHandler, long timeoutMs)
            throws IOException, InterruptedException {
        IOManager.ProcessResult result = RootProcessPool.executeCommand(command, timeoutMs, false);
        
        String[] stdoutLines = result.stdout().split("\n");
        String[] stderrLines = result.stderr().split("\n");
        
        for (String line : stdoutLines) {
            if (line.length() > 0) {
                lineHandler.onStdoutLine(line);
            }
        }
        
        for (String line : stderrLines) {
            if (line.length() > 0) {
                lineHandler.onStderrLine(line);
            }
        }
        
        return result.exitCode();
    }

    public static String quoted(String src) {
        return "\"" + src.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}