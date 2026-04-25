package com.justnothing.testmodule.ui.analysis.memory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

public record MemorySnapshot(
        long timestamp,
        HeapInfo javaHeap,
        HeapInfo nativeHeap,
        ProcessMemory processMemory,
        SystemMemory systemMemory,
        VmDetails vmDetails
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static MemorySnapshot fromResult(com.justnothing.testmodule.protocol.json.response.MemoryInfoResult result) {
        return new MemorySnapshot(
                result.getTimestamp(),
                new HeapInfo(result.getJavaMaxMemory(), result.getJavaTotalMemory(),
                        result.getJavaFreeMemory(), result.getJavaUsedMemory(), result.getJavaUsagePercent()),
                new HeapInfo(0, result.getNativeHeapSize(),
                        result.getNativeFreeSize(), result.getNativeAllocatedSize(), 0),
                new ProcessMemory(result.getProcessPss(), result.getProcessUss(), result.getProcessRss()),
                new SystemMemory(result.getSystemAvailMem(), result.getTotalMem(),
                        result.getSystemThreshold(), result.isSystemLowMemory()),
                new VmDetails(result.getThreadCount(), result.getThreadStackTotalBytes(),
                        result.getLoadedClassCount(), result.getTotalClassCount(),
                        result.getBitmapTotalBytes(), result.getBitmapCount(),
                        result.getDatabaseTotalBytes())
        );
    }

    public record HeapInfo(long maxBytes, long totalBytes, long freeBytes, long usedBytes, double usagePercent) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public String formatMax() { return formatBytes(maxBytes); }
        public String formatTotal() { return formatBytes(totalBytes); }
        public String formatUsed() { return formatBytes(usedBytes); }
        public String formatFree() { return formatBytes(freeBytes); }
        public String usageText() { return String.format(Locale.US, "%.1f%%", usagePercent); }
    }

    public record ProcessMemory(long pssKb, long ussKb, long rssKb) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public boolean hasData() { return pssKb > 0 || ussKb > 0 || rssKb > 0; }
        public String formatPss() { return formatBytes(pssKb); }
        public String formatUss() { return formatBytes(ussKb); }
        public String formatRss() { return formatBytes(rssKb); }
    }

    public record SystemMemory(long availBytes, long totalBytes, long threshold, boolean lowMemory) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public boolean hasData() { return totalBytes > 0; }
        public String formatAvail() { return formatBytes(availBytes); }
        public String formatTotal() { return formatBytes(totalBytes); }
        public double availPercent() { return totalBytes > 0 ? (double) (totalBytes - availBytes) / totalBytes * 100 : 0; }
    }

    public record VmDetails(int threadCount, long threadStackTotalBytes,
                             int loadedClassCount, int totalClassCount,
                             long bitmapTotalBytes, int bitmapCount,
                             long databaseTotalBytes) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public boolean hasData() {
            return threadCount > 0 || loadedClassCount > 0 || bitmapTotalBytes > 0 || databaseTotalBytes > 0;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "N/A";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) return (long) size + " " + units[unitIndex];
        return String.format(Locale.US, "%.2f %s", size, units[unitIndex]);
    }
}
