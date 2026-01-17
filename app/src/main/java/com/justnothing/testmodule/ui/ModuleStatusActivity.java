package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.data.ModuleStatusMonitor;
import com.justnothing.methodsclient.StreamClient;

import java.util.ArrayList;
import java.util.List;

public class ModuleStatusActivity extends AppCompatActivity {

    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "ModuleStatusActivity";
        }
    };

    private ModuleStatusMonitor monitor;
    private final List<ModuleStatusMonitor.HookDetail> hookDetails = new ArrayList<>();
    private HookDetailAdapter adapter;
    private Handler handler;
    private Runnable updateRunnable;
    private static final int REFRESH_INTERVAL = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_status);

        monitor = new ModuleStatusMonitor(this);
        handler = new Handler(Looper.getMainLooper());
        logger.info("模块状态监控界面启动");

        setupRecyclerView();
        setupButtons();
        refreshStatus();
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshStatus();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_hook_details);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);
        adapter = new HookDetailAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            logger.info("刷新模块状态");
            refreshStatus();
        });
    }

    private void refreshStatus() {
        new Thread(() -> {
            try {
                StreamClient.writeHookData(false);
            } catch (Exception e) {
                logger.error("写入Hook数据失败: " + e.getMessage());
            }

            handler.post(() -> {
                ModuleStatusMonitor.ModuleStatus status = monitor.getModuleStatus(true);

                TextView textModuleStatus = findViewById(R.id.text_module_status);
                TextView textHookCount = findViewById(R.id.text_hook_count);
                TextView textPackageCount = findViewById(R.id.text_package_count);
                TextView textProcessedPackages = findViewById(R.id.text_processed_packages);

                textModuleStatus.setText(getString(R.string.status_module_status_tip, status.isModuleActive ?
                        getString(R.string.status_module_status_activated) : getString(R.string.status_module_status_not_activated)));
                textHookCount.setText(getString(R.string.status_hook_count, status.hookCount, status.zygoteHookCount, status.packageHookCount));
                textPackageCount.setText(getString(R.string.status_processed_package_count_tip, status.processedPackages.size()));
                
                StringBuilder packages = new StringBuilder();
                for (String pkg : status.processedPackages) {
                    packages.append(pkg).append("\n");
                }
                textProcessedPackages.setText(getString(R.string.status_processed_packages_tip,
                        (packages.length() > 0 ? packages.toString() : getString(R.string.status_processed_package_none))));

                hookDetails.clear();
                hookDetails.addAll(status.hookDetails);
                
                logger.info("更新RecyclerView数据: hookDetails大小=" + hookDetails.size() +
                           ", status.hookDetails大小=" + status.hookDetails.size());
                
                for (int i = 0; i < hookDetails.size(); i++) {
                    ModuleStatusMonitor.HookDetail detail = hookDetails.get(i);
                    logger.info("Hook详情[" + i + "]: name=" + detail.name + 
                               ", type=" + detail.type + 
                               ", initialized=" + detail.isInitialized + 
                               ", hookCount=" + detail.hookCount);
                }
                
                adapter.notifyDataSetChanged();

                logger.info("状态刷新完成, 模块激活: " + status.isModuleActive +
                           ", Hook数量: " + status.hookCount + ", Hook详情数量: " + status.hookDetails.size());
            });
        }).start();
    }

    class HookDetailAdapter extends RecyclerView.Adapter<HookDetailAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hook_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(hookDetails.get(position));
        }

        @Override
        public int getItemCount() {
            return hookDetails.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName;
            TextView textType;
            TextView textInitialized;
            TextView textHookCount;
            TextView textStats;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_hook_name);
                textType = itemView.findViewById(R.id.text_hook_type);
                textInitialized = itemView.findViewById(R.id.text_hook_initialized);
                textHookCount = itemView.findViewById(R.id.text_hook_count);
                textStats = itemView.findViewById(R.id.text_hook_stats);
            }

            void bind(ModuleStatusMonitor.HookDetail detail) {
                String displayName = detail.displayName != null && !detail.displayName.isEmpty() ? detail.displayName : detail.name;
                String nameText = detail.name != null && !detail.name.isEmpty() ? detail.name : "";
                if (!nameText.isEmpty()) {
                    textName.setText(getString(R.string.status_hook_name_label, displayName + " (" + nameText + ")"));
                } else {
                    textName.setText(getString(R.string.status_hook_name_label, displayName));
                }
                textType.setText(getString(R.string.status_hook_type_label, detail.type));
                textInitialized.setText(getString(R.string.status_hook_initialized_label,
                        detail.isInitialized ? getString(R.string.status_hook_initialized) : getString(R.string.status_hook_not_initialized)));
                textHookCount.setText(getString(R.string.status_hook_count_label, detail.hookCount));
                
                StringBuilder stats = new StringBuilder();
                if (detail.description != null && !detail.description.isEmpty()) {
                    stats.append(detail.description).append("\n");
                }
                if (detail.processedPackageCount != null) {
                    stats.append(getString(R.string.status_hook_processed_packages_label, detail.processedPackageCount));
                }
                textStats.setText(stats.length() > 0 ? stats.toString() : getString(R.string.no_detailed_stats));
            }
        }
    }
}
