package com.justnothing.testmodule.command.functions.performance.systrace;

import android.util.Log;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystraceRunner {
    private static final String TAG = "SystraceRunner";
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Process systraceProcess;
    private Future<?> outputFuture;
    private String outputFile;
    private final String outputDir;
    private long startTime;
    private int duration;

    public SystraceRunner(String outputDir) {
        this.outputDir = outputDir;
    }

    public void start(int duration, String[] categories) {
        if (running.get()) {
            throw new IllegalStateException("Systrace 已在运行");
        }

        try {
            startTime = System.currentTimeMillis();
            this.duration = duration;
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            outputFile = outputDir + File.separator + "systrace_" + timestamp + ".html";
            
            File dir = new File(outputDir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("无法创建输出目录: " + outputDir);
                }
            }

            StringBuilder command = new StringBuilder();
            command.append("python systrace.py");
            command.append(" --time=").append(duration);
            command.append(" -o ").append(outputFile);
            
            if (categories != null) {
                for (String category : categories) {
                    command.append(" ").append(category);
                }
            }

            Log.i(TAG, "启动 Systrace: " + command.toString());
            
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command.toString());
            processBuilder.redirectErrorStream(true);
            
            systraceProcess = processBuilder.start();
            running.set(true);

            outputFuture = ThreadPoolManager.submitIORunnable(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(systraceProcess.getInputStream()));
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d(TAG, "Systrace: " + line);
                    }
                    
                    int timeout = duration + 10;
                    boolean completed = systraceProcess.waitFor(timeout, TimeUnit.SECONDS);
                    
                    if (completed) {
                        int exitCode = systraceProcess.exitValue();
                        Log.i(TAG, "Systrace 完成，退出码: " + exitCode);
                        
                        if (exitCode == 0) {
                            Log.i(TAG, "Systrace 输出文件: " + outputFile);
                        } else {
                            Log.w(TAG, "Systrace 退出码非零: " + exitCode);
                        }
                    } else {
                        Log.w(TAG, "Systrace 超时（" + timeout + " 秒）");
                    }
                    
                } catch (InterruptedException e) {
                    Log.w(TAG, "Systrace 线程被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.e(TAG, "Systrace 执行错误", e);
                } finally {
                    running.set(false);
                }
            });
            
        } catch (IOException e) {
            Log.e(TAG, "启动 Systrace 失败: IO 错误", e);
            running.set(false);
            throw new RuntimeException("启动 Systrace 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "启动 Systrace 失败", e);
            running.set(false);
            throw new RuntimeException("启动 Systrace 失败: " + e.getMessage(), e);
        }
    }

    public void stop() {
        if (!running.get()) {
            Log.w(TAG, "Systrace 未运行");
            return;
        }

        try {
            if (systraceProcess != null) {
                systraceProcess.destroy();
                
                try {
                    systraceProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.w(TAG, "等待 Systrace 进程结束被中断");
                }
                
                if (systraceProcess.isAlive()) {
                    systraceProcess.destroyForcibly();
                }
            }
            
            running.set(false);
            Log.i(TAG, "Systrace 已停止");
            
        } catch (Exception e) {
            Log.e(TAG, "停止 Systrace 失败", e);
            throw new RuntimeException("停止 Systrace 失败: " + e.getMessage(), e);
        }
    }

    public String getOutputFile() {
        return outputFile;
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
}
