package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.Colors;

import java.util.Locale;

public final class MemoryUtils {

    private MemoryUtils() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024));
        }
    }

    public static void printBytes(CommandExecutor.CmdExecContext ctx, long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        String formatted;
        if (unitIndex == 0) {
            formatted = String.valueOf((long) size);
        } else {
            formatted = String.format(Locale.US, "%.2f", size);
        }

        ctx.print(formatted, Colors.YELLOW);
        ctx.print(" " + units[unitIndex], Colors.CYAN);
    }

    public static void printMemoryValue(CommandExecutor.CmdExecContext ctx, String label, long bytes) {
        ctx.print(label, Colors.GRAY);
        printBytes(ctx, bytes);
        ctx.println("", Colors.DEFAULT);
    }

    public static byte getPercentColor(double percent) {
        if (percent < 50) {
            return Colors.LIGHT_GREEN;
        } else if (percent < 80) {
            return Colors.YELLOW;
        } else {
            return Colors.RED;
        }
    }
}
