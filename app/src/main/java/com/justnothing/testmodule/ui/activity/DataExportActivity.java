package com.justnothing.testmodule.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityDataExportBinding;
import com.justnothing.testmodule.databinding.ItemExportedFileBinding;
import com.justnothing.testmodule.utils.data.DataExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataExportActivity extends BaseActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private ActivityDataExportBinding binding;
    private DataExporter exporter;
    private final List<File> exportedFiles = new ArrayList<>();
    private ExportedFilesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataExportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        exporter = new DataExporter();
        logger.info("数据导出界面启动");

        setupRecyclerView();
        setupButtons();
        checkStoragePermission();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerExportedFiles;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExportedFilesAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnExportAll.setOnClickListener(v -> exportAllData());
        binding.btnExportConfig.setOnClickListener(v -> exportConfig());
        binding.btnExportStatus.setOnClickListener(v -> exportStatus());
        binding.btnExportPerformance.setOnClickListener(v -> exportPerformance());
        binding.btnRefresh.setOnClickListener(v -> refreshFileList());
        binding.btnClearAll.setOnClickListener(v -> clearAllExports());
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            refreshFileList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                   @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshFileList();
            } else {
                showToast(getString(R.string.data_export_permission_warn), Toast.LENGTH_LONG);
            }
        }
    }

    private void exportAllData() {
        try {
            String path = exporter.exportAllData();
            showToast(getString(R.string.data_export_all_data_exported_to, path),
                    Toast.LENGTH_LONG);
            logger.info("全部数据导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            showToast(getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG);
            logger.error("导出失败", e);
        }
    }

    private void exportConfig() {
        try {
            String path = exporter.saveToFile("hook_config", exporter.exportHookConfig());
            showToast(getString(R.string.data_export_hook_conf_exported_to, path),
                    Toast.LENGTH_LONG);
            logger.info("Hook配置导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            showToast(getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG);
            logger.error("导出失败", e);
        }
    }

    private void exportStatus() {
        try {
            String path = exporter.saveToFile("module_status", exporter.exportModuleStatus());
            showToast(getString(R.string.data_export_module_stat_exported_to, path),
                    Toast.LENGTH_LONG);
            logger.info("模块状态导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            showToast(getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG);
            logger.error("导出失败", e);
        }
    }

    private void exportPerformance() {
        try {
            String path = exporter.saveToFile("performance_data", exporter.exportPerformanceData());
            showToast(getString(R.string.data_export_perf_data_exported_to, path),
                    Toast.LENGTH_LONG);
            logger.info("性能数据导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            showToast(getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG);
            logger.error("导出失败", e);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshFileList() {
        exportedFiles.clear();
        exportedFiles.addAll(exporter.getExportedFiles());
        adapter.notifyDataSetChanged();
        binding.textFileCount.setText(getString(R.string.exported_file_count, exportedFiles.size()));
        logger.info("刷新文件列表完成，共 " + exportedFiles.size() + " 个文件");
    }

    private void clearAllExports() {
        if (exporter.clearAllExports()) {
            showToast(getString(R.string.data_export_all_data_cleared));
            logger.info("清除所有导出文件成功");
            refreshFileList();
        } else {
            showToast(getString(R.string.data_export_clear_all_data_failed));
            logger.error("清除导出文件失败");
        }
    }

    class ExportedFilesAdapter extends RecyclerView.Adapter<ExportedFilesAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemExportedFileBinding itemBinding = ItemExportedFileBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(exportedFiles.get(position));
        }

        @Override
        public int getItemCount() {
            return exportedFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemExportedFileBinding binding;

            ViewHolder(ItemExportedFileBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(File file) {
                binding.textFileName.setText(getString(R.string.exported_file_name, file.getName()));
                binding.textFilePath.setText(getString(R.string.exported_file_directory, file.getAbsolutePath()));
                binding.textFileSize.setText(getString(R.string.exported_file_size, formatFileSize(file.length())));

                binding.btnDeleteFile.setOnClickListener(v -> {
                    if (exporter.deleteExportedFile(file)) {
                        showToast(getString(R.string.data_export_file_cleared, file.getName()));
                        logger.info("删除文件成功: " + file.getName());
                        refreshFileList();
                    } else {
                        showToast(getString(R.string.data_export_file_clear_failed, file.getName()));
                        logger.error("删除文件失败: " + file.getName());
                    }
                });
            }

            private String formatFileSize(long size) {
                if (size < 1024) {
                    return size + " B";
                } else if (size < 1024 * 1024) {
                    return String.format(Locale.getDefault(), "%.2f KB", size / 1024.0);
                } else if (size < 1024 * 1024 * 1024) {
                    return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024));
                } else {
                    return String.format(Locale.getDefault(), "%.2f GB", size / (1024.0 * 1024 * 1024));
                }
            }
        }
    }
}
