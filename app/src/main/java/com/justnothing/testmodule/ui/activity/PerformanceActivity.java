package com.justnothing.testmodule.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityPerformanceBinding;
import com.justnothing.testmodule.databinding.ItemPerformanceStatBinding;
import com.justnothing.testmodule.utils.data.PerformanceMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerformanceActivity extends BaseActivity {
    private ActivityPerformanceBinding binding;
    private PerformanceMonitor monitor;
    private StatsAdapter adapter;
    private List<PerformanceMonitor.HookStats> statsList;
    private Runnable updateRunnable;
    private static final int REFRESH_INTERVAL = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerformanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        monitor = new PerformanceMonitor();

        statsList = new ArrayList<>();
        adapter = new StatsAdapter();

        RecyclerView recyclerView = binding.statsList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        binding.btnRefresh.setOnClickListener(v -> {
            refreshStats();
            logger.info("刷新性能统计");
        });

        binding.btnClear.setOnClickListener(v -> {
            monitor.clearStats();
            refreshStats();
            logger.info("清除性能统计");
        });

        refreshStats();
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshStats();
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

    @SuppressLint("NotifyDataSetChanged")
    private void refreshStats() {
        Map<String, PerformanceMonitor.HookStats> stats = monitor.getAllStats();
        statsList.clear();
        statsList.addAll(stats.values());
        adapter.notifyDataSetChanged();
        String statDesc = getString(R.string.performance_monitor_stat_desc,
                (monitor.isEnabled() ?
                        getString(R.string.performance_monitor_stat_enabled) :
                        getString(R.string.performance_monitor_stat_disabled)));
        String hookDesc = getString(R.string.performance_hook_count, statsList.size());
        String finalDesc = statDesc + getString(R.string.newline) + hookDesc;
        binding.textStatus.setText(finalDesc);

    }

    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPerformanceStatBinding itemBinding = ItemPerformanceStatBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PerformanceMonitor.HookStats stat = statsList.get(position);
            holder.bind(stat);
        }

        @Override
        public int getItemCount() {
            return statsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemPerformanceStatBinding binding;

            ViewHolder(ItemPerformanceStatBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(PerformanceMonitor.HookStats stat) {
                binding.textStatName.setText(stat.name);
                binding.textCallCount.setText(getString(R.string.performance_hook_call_cnt, stat.callCount));
                binding.textTotalTime.setText(getString(R.string.performance_hook_total_time, stat.totalTime));
                binding.textAvgTime.setText(getString(R.string.performance_hook_avg_time, stat.avgTime));
            }
        }
    }
}
