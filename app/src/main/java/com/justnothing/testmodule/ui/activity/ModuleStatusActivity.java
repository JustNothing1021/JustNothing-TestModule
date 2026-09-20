package com.justnothing.testmodule.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityModuleStatusBinding;
import com.justnothing.testmodule.databinding.ItemHookDetailBinding;
import com.justnothing.testmodule.utils.data.ModuleStatusMonitor;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.methodsclient.StreamClient;

import java.util.ArrayList;
import java.util.List;

public class ModuleStatusActivity extends BaseActivity {
    private ActivityModuleStatusBinding binding;
    private ModuleStatusMonitor monitor;
    private final List<ModuleStatusMonitor.HookDetail> hookDetails = new ArrayList<>();
    private HookDetailAdapter adapter;
    private Runnable updateRunnable;
    private static final int REFRESH_INTERVAL = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModuleStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        monitor = new ModuleStatusMonitor();
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
                mainHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        mainHandler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateRunnable != null) {
            mainHandler.removeCallbacks(updateRunnable);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerHookDetails;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);
        adapter = new HookDetailAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnRefresh.setOnClickListener(v -> {
            logger.info("刷新模块状态");
            refreshStatus();
        });
    }

    private void refreshStatus() {
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                if (!StreamClient.writeHookData(false)) logger.warn("写入Hook数据失败, writeHookData returns false");
            } catch (Exception e) {
                logger.error("写入Hook数据出现错误: " + e.getMessage());
            }

            mainHandler.post(() -> {
                ModuleStatusMonitor.ModuleStatus status = monitor.getModuleStatus(true);

                TextView textModuleStatus = binding.textModuleStatus;
                TextView textHookCount = binding.textHookCount;
                TextView textPackageCount = binding.textPackageCount;
                TextView textProcessedPackages = binding.textProcessedPackages;

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
        });
    }

    class HookDetailAdapter extends RecyclerView.Adapter<HookDetailAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemHookDetailBinding itemBinding = ItemHookDetailBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
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
            private final ItemHookDetailBinding binding;

            ViewHolder(ItemHookDetailBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(ModuleStatusMonitor.HookDetail detail) {
                String displayName = detail.displayName != null && !detail.displayName.isEmpty() ? detail.displayName : detail.name;
                String nameText = detail.name != null && !detail.name.isEmpty() ? detail.name : "";
                if (!nameText.isEmpty()) {
                    binding.textHookName.setText(getString(R.string.status_hook_name_label, String.format(getString(R.string.hook_name_with_internal), displayName, nameText)));
                } else {
                    binding.textHookName.setText(getString(R.string.status_hook_name_label, displayName));
                }
                binding.textHookType.setText(getString(R.string.status_hook_type_label, detail.type));
                binding.textHookInitialized.setText(getString(R.string.status_hook_initialized_label,
                        detail.isInitialized ? getString(R.string.status_hook_initialized) : getString(R.string.status_hook_not_initialized)));
                binding.textHookCount.setText(getString(R.string.status_hook_count_label, detail.hookCount));
                
                StringBuilder stats = new StringBuilder();
                if (detail.description != null && !detail.description.isEmpty()) {
                    stats.append(detail.description).append("\n");
                }
                if (detail.processedPackageCount != null) {
                    stats.append(getString(R.string.status_hook_processed_packages_label, detail.processedPackageCount));
                }
                binding.textHookStats.setText(stats.length() > 0 ? stats.toString() : getString(R.string.no_detailed_stats));
            }
        }
    }
}
