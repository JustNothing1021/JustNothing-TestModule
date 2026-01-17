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
import com.justnothing.testmodule.utils.data.PerformanceMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerformanceActivity extends AppCompatActivity {
    private Logger logger;
    private PerformanceMonitor monitor;
    private StatsAdapter adapter;
    private List<PerformanceMonitor.HookStats> statsList;
    private Handler handler;
    private Runnable updateRunnable;
    private static final int REFRESH_INTERVAL = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        logger = new Logger() {
            @Override
            public String getTag() {
                return "PerformanceActivity";
            }
        };
        monitor = new PerformanceMonitor();
        handler = new Handler(Looper.getMainLooper());

        statsList = new ArrayList<>();
        adapter = new StatsAdapter();

        RecyclerView recyclerView = findViewById(R.id.stats_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnClear = findViewById(R.id.btn_clear);
        TextView textStatus = findViewById(R.id.text_status);

        btnRefresh.setOnClickListener(v -> {
            refreshStats();
            logger.info("刷新性能统计");
        });

        btnClear.setOnClickListener(v -> {
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

    private void refreshStats() {
        Map<String, PerformanceMonitor.HookStats> stats = monitor.getAllStats();
        statsList.clear();
        statsList.addAll(stats.values());
        adapter.notifyDataSetChanged();

        TextView textStatus = findViewById(R.id.text_status);
        textStatus.setText("监控状态: " + (monitor.isEnabled() ? "启用" : "禁用") + 
                          "\nHook数量: " + statsList.size());
    }

    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_performance_stat, parent, false);
            return new ViewHolder(view);
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
            TextView textName;
            TextView textCallCount;
            TextView textTotalTime;
            TextView textAvgTime;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_stat_name);
                textCallCount = itemView.findViewById(R.id.text_call_count);
                textTotalTime = itemView.findViewById(R.id.text_total_time);
                textAvgTime = itemView.findViewById(R.id.text_avg_time);
            }

            void bind(PerformanceMonitor.HookStats stat) {
                textName.setText(stat.name);
                textCallCount.setText("调用次数: " + stat.callCount);
                textTotalTime.setText("总耗时: " + stat.totalTime + "ms");
                textAvgTime.setText("平均耗时: " + stat.avgTime + "ms");
            }
        }
    }
}
