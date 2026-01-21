package com.justnothing.testmodule.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.data.DataExporter;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataExportActivity extends AppCompatActivity {

    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "DataExportActivity";
        }
    };

    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private DataExporter exporter;
    private final List<File> exportedFiles = new ArrayList<>();
    private ExportedFilesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_export);

        exporter = new DataExporter(this);
        logger.info("数据导出界面启动");

        setupRecyclerView();
        setupButtons();
        checkStoragePermission();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_exported_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExportedFilesAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        Button btnExportAll = findViewById(R.id.btn_export_all);
        Button btnExportConfig = findViewById(R.id.btn_export_config);
        Button btnExportStatus = findViewById(R.id.btn_export_status);
        Button btnExportPerformance = findViewById(R.id.btn_export_performance);
        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnClearAll = findViewById(R.id.btn_clear_all);

        btnExportAll.setOnClickListener(v -> exportAllData());
        btnExportConfig.setOnClickListener(v -> exportConfig());
        btnExportStatus.setOnClickListener(v -> exportStatus());
        btnExportPerformance.setOnClickListener(v -> exportPerformance());
        btnRefresh.setOnClickListener(v -> refreshFileList());
        btnClearAll.setOnClickListener(v -> clearAllExports());
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
                Toast.makeText(this, getString(R.string.data_export_permission_warn), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void exportAllData() {
        try {
            String path = exporter.exportAllData();
            Toast.makeText(this, getString(R.string.data_export_all_data_exported_to, path),
                    Toast.LENGTH_LONG).show();
            logger.info("全部数据导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG).show();
            logger.error("导出失败", e);
        }
    }

    private void exportConfig() {
        try {
            String path = exporter.saveToFile("hook_config", exporter.exportHookConfig());
            Toast.makeText(this, getString(R.string.data_export_hook_conf_exported_to, path),
                    Toast.LENGTH_LONG).show();
            logger.info("Hook配置导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG).show();
            logger.error("导出失败", e);
        }
    }

    private void exportStatus() {
        try {
            String path = exporter.saveToFile("module_status", exporter.exportModuleStatus());
            Toast.makeText(this, getString(R.string.data_export_module_stat_exported_to, path),
                    Toast.LENGTH_LONG).show();
            logger.info("模块状态导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG).show();
            logger.error("导出失败", e);
        }
    }

    private void exportPerformance() {
        try {
            String path = exporter.saveToFile("performance_data", exporter.exportPerformanceData());
            Toast.makeText(this, getString(R.string.data_export_perf_data_exported_to, path),
                    Toast.LENGTH_LONG).show();
            logger.info("性能数据导出成功: " + path);
            refreshFileList();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.data_export_exception_info, e.getMessage()),
                    Toast.LENGTH_LONG).show();
            logger.error("导出失败", e);
        }
    }

    private void refreshFileList() {
        exportedFiles.clear();
        exportedFiles.addAll(exporter.getExportedFiles());
        adapter.notifyDataSetChanged();
        TextView textFileCount = findViewById(R.id.text_file_count);
        textFileCount.setText(getString(R.string.exported_file_count, exportedFiles.size()));
        logger.info("刷新文件列表完成，共 " + exportedFiles.size() + " 个文件");
    }

    private void clearAllExports() {
        if (exporter.clearAllExports()) {
            Toast.makeText(this, getString(R.string.data_export_all_data_cleared),
                    Toast.LENGTH_SHORT).show();
            logger.info("清除所有导出文件成功");
            refreshFileList();
        } else {
            Toast.makeText(this, getString(R.string.data_export_clear_all_data_failed),
                    Toast.LENGTH_SHORT).show();
            logger.error("清除导出文件失败");
        }
    }

    class ExportedFilesAdapter extends RecyclerView.Adapter<ExportedFilesAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exported_file, parent, false);
            return new ViewHolder(view);
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
            TextView textFileName;
            TextView textFilePath;
            TextView textFileSize;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textFileName = itemView.findViewById(R.id.text_file_name);
                textFilePath = itemView.findViewById(R.id.text_file_path);
                textFileSize = itemView.findViewById(R.id.text_file_size);
                btnDelete = itemView.findViewById(R.id.btn_delete_file);
            }

            void bind(File file) {
                textFileName.setText(getString(R.string.exported_file_name, file.getName()));
                textFilePath.setText(getString(R.string.exported_file_directory, file.getAbsolutePath()));
                textFileSize.setText(getString(R.string.exported_file_size, formatFileSize(file.length())));

                btnDelete.setOnClickListener(v -> {
                    if (exporter.deleteExportedFile(file)) {
                        Toast.makeText(DataExportActivity.this, 
                                getString(R.string.data_export_file_cleared, file.getName()), Toast.LENGTH_SHORT).show();
                        logger.info("删除文件成功: " + file.getName());
                        refreshFileList();
                    } else {
                        Toast.makeText(DataExportActivity.this, 
                                getString(R.string.data_export_file_clear_failed, file.getName()), Toast.LENGTH_SHORT).show();
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
