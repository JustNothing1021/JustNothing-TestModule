package com.justnothing.testmodule.command.output;

import com.justnothing.testmodule.utils.functions.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class StreamOutputWriter implements IOutputHandler {
    private final OutputStream outputStream;
    private final PrintWriter printWriter;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final BlockingQueue<String> outputQueue = new ArrayBlockingQueue<>(1024);
    private final Thread writerThread;
    private final StringBuilder outputBuilder;

    public static class StreamOutputLogger extends Logger {
        @Override
        public String getTag() {
            return "StreamOutputWriter";
        }
    }

    public static final StreamOutputLogger logger = new StreamOutputLogger();

    public StreamOutputWriter(OutputStream outputStream) {
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream不能为null");
        }
        this.outputStream = outputStream;
        this.printWriter = new PrintWriter(outputStream, true);
        this.outputBuilder = new StringBuilder();

        this.writerThread = new Thread(this::writeLoop);
        this.writerThread.setName("StreamOutputWriter-WriterThread");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
        logger.debug("StreamOutputWriter初始化完成");
    }


    private void writeLoop() {
        logger.debug("写入线程启动");
        try {
            while (!closed.get() || !outputQueue.isEmpty()) {
                try {
                    String line = outputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (line != null) {
                        synchronized (printWriter) {
                            outputBuilder.append(line);
                            printWriter.print(line);
                            printWriter.flush();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("写入线程被中断");
                    break;
                } catch (Exception e) {
                    logger.error("写入循环中出现异常", e);
                }
            }
        } catch (Exception e) {
            logger.error("写入循环严重错误", e);
        } finally {
            try {
                synchronized (printWriter) {
                    printWriter.close();
                }
                outputStream.close();
                logger.debug("输出流已关闭");
            } catch (IOException e) {
                logger.warn("关闭输出流时出错", e);
            }
            logger.debug("写入线程结束");
        }
    }

    @Override
    public void println(String line) {
        if (closed.get()) {
            throw new IllegalStateException("输出器已关闭");
        }

        if (line == null) {
            line = "null";
        }

        try {
            // 添加换行符并放入队列
            String outputLine = line + "\n";
            if (!outputQueue.offer(outputLine, 1, TimeUnit.SECONDS)) {
                // 队列已满，丢弃输出
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void print(String text) {
        if (closed.get()) {
            throw new IllegalStateException("输出器已关闭");
        }

        if (text == null) {
            text = "null";
        }

        try {
            outputQueue.offer(text, 1, TimeUnit.SECONDS);// 队列已满的话丢弃输出
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            println(sw.toString());
        }
    }

    @Override
    public void flush() {
        synchronized (printWriter) {
            printWriter.flush();
        }
        try {
            outputStream.flush();
        } catch (IOException e) {
            // 忽略刷新异常
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // 等待写入线程完成
            try {
                writerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writerThread.interrupt();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void clear() {
        outputBuilder.setLength(0);
    }

    @Override
    public String getString() {
        return outputBuilder.toString();
    }

    @Override
    public String readLineFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + "并不支持readLine...");
    }

    @Override
    public String readPasswordFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + "并不支持readPassword...");

    }

    @Override
    public boolean isInteractive() {
        return false;
    }
}