package com.justnothing.testmodule.ui.activity.analysis.memory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.memory.GcResult;
import com.justnothing.testmodule.ui.viewmodel.analysis.MemoryAnalysisViewModel;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MemoryAnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_TREND_TYPE = "trend_type";
    public static final String EXTRA_TREND_DATA = "trend_data";
    public static final String TREND_JAVA_HEAP = "java_heap";
    public static final String TREND_NATIVE_HEAP = "native_heap";
    public static final String TREND_SYSTEM = "system";

    private MemoryAnalysisViewModel viewModel;

    private ProgressBar progressBar;
    private TextView tvError;
    private SwitchMaterial switchAutoRefresh;
    private TextView tvLastUpdateTime;

    private ProgressBar progressJavaHeap;
    private TextView tvJavaUsed, tvJavaPercent, tvJavaDetail;

    private ProgressBar progressNativeHeap;
    private TextView tvNativeUsed, tvNativePercent, tvNativeDetail;

    private TextView tvPssValue, tvUssValue, tvRssValue;

    private ProgressBar progressSystemMem;
    private TextView tvSystemAvail, tvSystemPercent, tvSystemTotal, tvLowMemoryStatus;

    private MaterialCardView cardJavaHeap, cardNativeHeap, cardSystemMemory, cardProcessMemory;
    private MaterialCardView cardVmDetails;
    private TextView tvThreadCount, tvLoadedClasses, tvTotalClasses, tvBitmapInfo, tvDbSize;

    private MaterialButton btnGc, btnExportDump;

    private boolean hasInitialData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_analysis);

        initViews();
        initViewModel();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        switchAutoRefresh = findViewById(R.id.switch_auto_refresh);
        tvLastUpdateTime = findViewById(R.id.tv_last_update_time);

        progressJavaHeap = findViewById(R.id.progress_java_heap);
        tvJavaUsed = findViewById(R.id.tv_java_used);
        tvJavaPercent = findViewById(R.id.tv_java_percent);
        tvJavaDetail = findViewById(R.id.tv_java_detail);

        progressNativeHeap = findViewById(R.id.progress_native_heap);
        tvNativeUsed = findViewById(R.id.tv_native_used);
        tvNativePercent = findViewById(R.id.tv_native_percent);
        tvNativeDetail = findViewById(R.id.tv_native_detail);

        tvPssValue = findViewById(R.id.tv_pss_value);
        tvUssValue = findViewById(R.id.tv_uss_value);
        tvRssValue = findViewById(R.id.tv_rss_value);

        progressSystemMem = findViewById(R.id.progress_system_mem);
        tvSystemAvail = findViewById(R.id.tv_system_avail);
        tvSystemPercent = findViewById(R.id.tv_system_percent);
        tvSystemTotal = findViewById(R.id.tv_system_total);
        tvLowMemoryStatus = findViewById(R.id.tv_low_memory_status);

        cardJavaHeap = findViewById(R.id.card_java_heap);
        cardNativeHeap = findViewById(R.id.card_native_heap);
        cardSystemMemory = findViewById(R.id.card_system_memory);
        cardProcessMemory = findViewById(R.id.card_process_memory);
        cardVmDetails = findViewById(R.id.card_vm_details);

        tvThreadCount = findViewById(R.id.tv_thread_count);
        tvLoadedClasses = findViewById(R.id.tv_loaded_classes);
        tvTotalClasses = findViewById(R.id.tv_total_classes);
        tvBitmapInfo = findViewById(R.id.tv_bitmap_info);
        tvDbSize = findViewById(R.id.tv_db_size);

        btnGc = findViewById(R.id.btn_gc);
        btnExportDump = findViewById(R.id.btn_export_dump);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MemoryAnalysisViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (!hasInitialData) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(error);
            } else {
                tvError.setVisibility(View.GONE);
            }
        });

        viewModel.getLastUpdateTime().observe(this, time -> tvLastUpdateTime.setText(time != null ? time : ""));

        viewModel.isAutoRefresh().observe(this, enabled -> {
            boolean isChecked = enabled != null && enabled;
            switchAutoRefresh.setOnCheckedChangeListener(null);
            switchAutoRefresh.setChecked(isChecked);
            switchAutoRefresh.setOnCheckedChangeListener((buttonView, isOn) ->
                    viewModel.setAutoRefresh(isOn));
        });

        viewModel.getMemoryData().observe(this, this::displayMemorySnapshot);

        viewModel.getGcResult().observe(this, this::showGcResult);

        btnGc.setOnClickListener(v -> showGcConfirmationDialog());
        btnExportDump.setOnClickListener(v -> exportDump());

        cardJavaHeap.setOnClickListener(v -> openTrendChart(TREND_JAVA_HEAP));
        cardNativeHeap.setOnClickListener(v -> openTrendChart(TREND_NATIVE_HEAP));
        cardSystemMemory.setOnClickListener(v -> openTrendChart(TREND_SYSTEM));
    }

    private void openTrendChart(String trendType) {
        Intent intent = new Intent(this, MemoryTrendActivity.class);
        intent.putExtra(EXTRA_TREND_TYPE, trendType);
        java.util.ArrayList<MemorySnapshot> data = new java.util.ArrayList<>(viewModel.getHistorySamples());
        intent.putExtra(EXTRA_TREND_DATA, data);
        startActivity(intent);
    }

    private void displayMemorySnapshot(MemorySnapshot snapshot) {
        if (snapshot == null) return;

        hasInitialData = true;
        tvError.setVisibility(View.GONE);

        displayJavaHeap(snapshot.javaHeap());
        displayNativeHeap(snapshot.nativeHeap());
        displayProcessMemory(snapshot.processMemory());
        displaySystemMemory(snapshot.systemMemory());
        displayVmDetails(snapshot.vmDetails());
    }

    private void displayJavaHeap(MemorySnapshot.HeapInfo heap) {
        int percent = (int) Math.min(100, Math.max(0, heap.usagePercent()));
        progressJavaHeap.setProgress(percent);
        setProgressTint(progressJavaHeap, percent);

        tvJavaUsed.setText(heap.formatUsed());
        tvJavaPercent.setText(heap.usageText());
        tvJavaPercent.setTextColor(getUsageColor(percent));

        tvJavaDetail.setText(getString(R.string.analysis_memory_heap_detail,
                heap.formatFree(), heap.formatTotal(), heap.formatMax()));
    }

    private void displayNativeHeap(MemorySnapshot.HeapInfo heap) {
        double totalBytes = heap.totalBytes();
        int percent = totalBytes > 0 ? (int) Math.min(100, (heap.usedBytes() * 100.0 / totalBytes)) : 0;
        progressNativeHeap.setProgress(percent);
        setProgressTint(progressNativeHeap, percent);

        tvNativeUsed.setText(heap.formatUsed());
        tvNativePercent.setText(String.format(Locale.US, "%.1f%%", (double) heap.usedBytes() * 100 / Math.max(1, totalBytes)));
        tvNativePercent.setTextColor(getUsageColor(percent));

        tvNativeDetail.setText(getString(R.string.analysis_memory_native_detail,
                heap.formatFree(), heap.formatTotal()));
    }

    private void displayProcessMemory(MemorySnapshot.ProcessMemory process) {
        boolean hasData = process.hasData();
        cardProcessMemory.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (hasData) {
            tvPssValue.setText(process.formatPss());
            tvUssValue.setText(process.formatUss());
            tvRssValue.setText(process.formatRss());
        }
    }

    private void displaySystemMemory(MemorySnapshot.SystemMemory system) {
        boolean hasData = system.hasData();
        cardSystemMemory.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (!hasData) return;

        int usedPercent = (int) Math.min(100, system.availPercent());
        progressSystemMem.setProgress(usedPercent);
        setProgressTint(progressSystemMem, usedPercent);

        tvSystemAvail.setText(system.formatAvail());
        tvSystemPercent.setText(String.format(Locale.US, "%.1f%%", system.availPercent()));
        tvSystemPercent.setTextColor(getUsageColor(usedPercent));
        tvSystemTotal.setText(getString(R.string.analysis_memory_system_total, system.formatTotal()));

        if (system.lowMemory()) {
            tvLowMemoryStatus.setText(R.string.analysis_memory_low_memory_yes);
            tvLowMemoryStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            tvLowMemoryStatus.setText(R.string.analysis_memory_low_memory_no);
            tvLowMemoryStatus.setTextColor(ContextCompat.getColor(this, R.color.light_green));
        }
    }

    private void displayVmDetails(MemorySnapshot.VmDetails vm) {
        boolean hasData = vm.hasData();
        cardVmDetails.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (!hasData) return;

        if (vm.threadCount() > 0) {
            tvThreadCount.setText(String.valueOf(vm.threadCount()));
        } else {
            tvThreadCount.setText("N/A");
        }

        if (vm.loadedClassCount() > 0) {
            tvLoadedClasses.setText(String.valueOf(vm.loadedClassCount()));
        } else {
            tvLoadedClasses.setText("N/A");
        }

        if (vm.totalClassCount() > 0) {
            tvTotalClasses.setText(String.valueOf(vm.totalClassCount()));
        } else {
            tvTotalClasses.setText("N/A");
        }

        if (vm.bitmapTotalBytes() > 0) {
            String bitmapInfo = formatBytes(vm.bitmapTotalBytes()) +
                    " (" + vm.bitmapCount() + ")";
            tvBitmapInfo.setText(bitmapInfo);
        } else {
            tvBitmapInfo.setText("N/A");
        }

        if (vm.databaseTotalBytes() > 0) {
            tvDbSize.setText(formatBytes(vm.databaseTotalBytes()));
        } else {
            tvDbSize.setText("N/A");
        }
    }

    private void showGcConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_memory_gc_confirm_title)
                .setMessage(R.string.analysis_memory_gc_confirm_message)
                .setPositiveButton(R.string.analysis_memory_gc_full, (dialog, which) ->
                        viewModel.triggerGc(true))
                .setNegativeButton(R.string.analysis_memory_gc_standard, (dialog, which) ->
                        viewModel.triggerGc(false))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void showGcResult(GcResult result) {
        if (result == null || !result.isSuccess()) return;

        String freedStr = result.getFreedBytes() > 0
                ? formatBytes(result.getFreedBytes()) + " " + getString(R.string.analysis_memory_freed)
                : getString(R.string.analysis_memory_no_change);

        String message = getString(R.string.analysis_memory_gc_result_format,
                formatBytes(result.getBeforeUsedMemory()),
                result.getBeforeUsagePercent(),
                formatBytes(result.getAfterUsedMemory()),
                result.getAfterUsagePercent(),
                freedStr);

        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_memory_gc_complete_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void exportDump() {
        try {
            File exportDir = new File(
                    android.os.Environment.getExternalStorageDirectory(),
                    com.justnothing.testmodule.constants.FileDirectory.EXPORT_DIR_NAME + "/memory_dumps");
            IOManager.createDirectory(exportDir.getAbsolutePath());

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "memory_dump_" + timestamp + ".txt";
            File outputFile = new File(exportDir, fileName);

            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.analysis_memory_dump_title)).append("\n");
            sb.append(getString(R.string.analysis_memory_dump_time, new Date().toString())).append("\n\n");

            MemorySnapshot snapshot = viewModel.getMemoryData().getValue();
            if (snapshot != null) {
                sb.append(getString(R.string.analysis_memory_dump_section_java)).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_max, snapshot.javaHeap().formatMax())).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_allocated, snapshot.javaHeap().formatTotal())).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_free, snapshot.javaHeap().formatFree())).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_used,
                        snapshot.javaHeap().formatUsed(), snapshot.javaHeap().usageText())).append("\n\n");

                sb.append(getString(R.string.analysis_memory_dump_section_native)).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_allocated, snapshot.nativeHeap().formatTotal())).append("\n");
                sb.append(getString(R.string.analysis_memory_dump_free, snapshot.nativeHeap().formatFree())).append("\n\n");

                if (snapshot.processMemory().hasData()) {
                    sb.append(getString(R.string.analysis_memory_dump_section_process)).append("\n");
                    sb.append("  PSS: ").append(snapshot.processMemory().formatPss()).append("\n");
                    sb.append("  USS: ").append(snapshot.processMemory().formatUss()).append("\n");
                    sb.append("  RSS: ").append(snapshot.processMemory().formatRss()).append("\n\n");
                }

                if (snapshot.systemMemory().hasData()) {
                    sb.append(getString(R.string.analysis_memory_dump_section_system)).append("\n");
                    sb.append(getString(R.string.analysis_memory_dump_avail, snapshot.systemMemory().formatAvail())).append("\n");
                    sb.append(getString(R.string.analysis_memory_dump_total, snapshot.systemMemory().formatTotal())).append("\n");
                    sb.append(snapshot.systemMemory().lowMemory()
                            ? getString(R.string.analysis_memory_dump_low_memory_yes)
                            : getString(R.string.analysis_memory_dump_low_memory_no)).append("\n");
                }

                if (snapshot.vmDetails().hasData()) {
                    sb.append(getString(R.string.analysis_memory_dump_section_vm)).append("\n");
                    sb.append("  Threads: ").append(snapshot.vmDetails().threadCount()).append("\n");
                    sb.append("  Loaded Classes: ").append(snapshot.vmDetails().loadedClassCount()).append("\n");
                    sb.append("  Total Classes: ").append(snapshot.vmDetails().totalClassCount()).append("\n");
                    sb.append("  Bitmap: ").append(formatBytes(snapshot.vmDetails().bitmapTotalBytes()))
                            .append(" (").append(snapshot.vmDetails().bitmapCount()).append(")\n");
                    sb.append("  Database: ").append(formatBytes(snapshot.vmDetails().databaseTotalBytes())).append("\n");
                }
            }

            IOManager.writeFile(outputFile.getAbsolutePath(), sb.toString());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.analysis_memory_export_success_title)
                    .setMessage(getString(R.string.analysis_memory_export_success_msg, outputFile.getAbsolutePath()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.analysis_memory_export_failed_title)
                    .setMessage(e.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
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

    private int getUsageColor(int percent) {
        if (percent < 50) return ContextCompat.getColor(this, R.color.light_green);
        if (percent < 80) return ContextCompat.getColor(this, R.color.yellow);
        return ContextCompat.getColor(this, R.color.red);
    }

    private void setProgressTint(ProgressBar progressBar, int percent) {
        int color = getUsageColor(percent);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasInitialData = false;
        Boolean refreshing = viewModel.isAutoRefresh().getValue();
        if (refreshing != null && refreshing) {
            viewModel.setAutoRefresh(true);
        } else {
            viewModel.queryMemoryInfo(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Boolean refreshing = viewModel.isAutoRefresh().getValue();
        if (refreshing != null && refreshing) {
            viewModel.setAutoRefresh(false);
        }
    }
}
