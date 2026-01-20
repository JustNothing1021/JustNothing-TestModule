package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.data.LogWriter;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LogViewerActivity extends AppCompatActivity {

    private static class ViewerLogger extends Logger {
        @Override
        public String getTag() {
            return "LogViewerActivity";
        }

        public void infoWithoutFile(String str) {
            if (SILENT) return;
            xposedLog(str);
            if (shouldUseSystemLogger()) {
                Log.i(MAIN_TAG + "[" + getTag() + "]", str);
            }
        }

        public void errorWithoutFile(String str, Throwable e) {
            if (SILENT) return;
            xposedLog(str);
            if (shouldUseSystemLogger()) {
                Log.e(MAIN_TAG + "[" + getTag() + "]", str);
                if (e != null) {
                    Log.e(MAIN_TAG + "[" + getTag() + "]", Log.getStackTraceString(e));
                }
            }
        }
    }

    private final ViewerLogger logger = new ViewerLogger();

    private LogWriter logWriter;
    private LogAdapter adapter;
    private Handler handler;
    private Runnable updateRunnable;
    private ProgressBar progressBar;
    private boolean autoScroll = true;
    private String currentFilter = "ALL";
    private String searchText = "";
    private static final int MAX_DISPLAY_LOGS = 100;
    private static final int LOAD_MORE_INCREMENT = 100;
    private static final int REFRESH_INTERVAL = 5000;
    private int currentDisplayLimit = MAX_DISPLAY_LOGS;
    private String lastLogHash = "";
    private List<LogWriter.LogEntry> allCachedLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        logWriter = new LogWriter();
        handler = new Handler(Looper.getMainLooper());

        initViews();
        startAutoUpdate();
    }

    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.log_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progress_loading);

        EditText editSearch = findViewById(R.id.edit_search);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
                handler.post(() -> adapter.setEntries(filteredLogs));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            logWriter.clearLogs();
            refreshLogs();
            logger.infoWithoutFile("日志已清除");
        });

        findViewById(R.id.btn_all).setOnClickListener(v -> setFilter("ALL"));
        findViewById(R.id.btn_debug).setOnClickListener(v -> setFilter("DEBUG"));
        findViewById(R.id.btn_info).setOnClickListener(v -> setFilter("INFO"));
        findViewById(R.id.btn_warn).setOnClickListener(v -> setFilter("WARN"));
        findViewById(R.id.btn_error).setOnClickListener(v -> setFilter("ERROR"));

        findViewById(R.id.btn_auto_scroll).setOnClickListener(v -> {
            autoScroll = !autoScroll;
            ((TextView) v).setText(autoScroll ? "自动滚动: 开" : "自动滚动: 关");
        });

        findViewById(R.id.btn_load_more).setOnClickListener(v -> {
            currentDisplayLimit += LOAD_MORE_INCREMENT;
            List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
            handler.post(() -> {
                adapter.setEntries(filteredLogs);
                logger.infoWithoutFile("加载更多日志，当前显示限制: " + currentDisplayLimit);
            });
        });

        refreshLogs();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
        handler.post(() -> {
            adapter.setEntries(filteredLogs);
            logger.infoWithoutFile("切换日志过滤: " + filter);
        });
    }

    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    private void refreshLogs() {
        handler.post(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new Thread(() -> {
            try {
                String logsText = DataBridge.readLogs();
                String currentHash = String.valueOf(logsText.hashCode());

                if (!currentHash.equals(lastLogHash)) {
                    lastLogHash = currentHash;
                    
                    List<LogWriter.LogEntry> newEntries = new ArrayList<>();
                    if (!logsText.isEmpty()) {
                        String[] lines = logsText.split("\n");
                        
                        logger.infoWithoutFile("日志行数: " + lines.length);
                        
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                try {
                                    LogWriter.LogEntry entry = LogWriter.LogEntry.fromString(line);
                                    newEntries.add(entry);
                                } catch (Exception e) {
                                    logger.errorWithoutFile("解析日志失败: " + line, e);
                                    newEntries.add(new LogWriter.LogEntry("ERROR", "ParseError", line));
                                }
                            }
                        }
                    }
                    
                    allCachedLogs = newEntries;
                    sortLogsByTimestamp(allCachedLogs);
                    
                    List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
                    
                    handler.post(() -> {
                        adapter.setEntries(filteredLogs);
                        if (autoScroll) {
                            RecyclerView recyclerView = findViewById(R.id.log_list);
                            if (recyclerView != null && recyclerView.getLayoutManager() != null) {
                                recyclerView.getLayoutManager().scrollToPosition(filteredLogs.size() - 1);
                            }
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    handler.post(() -> {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                logger.errorWithoutFile("刷新日志失败", e);
                handler.post(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }
    
    private List<LogWriter.LogEntry> filterLogs(List<LogWriter.LogEntry> logs) {
        List<LogWriter.LogEntry> result = new ArrayList<>();
        
        for (LogWriter.LogEntry entry : logs) {
            if (!shouldShowEntry(entry)) {
                continue;
            }
            
            result.add(entry);
        }
        
        if (result.size() > currentDisplayLimit) {
            result = result.subList(result.size() - currentDisplayLimit, result.size());
        }
        
        return result;
    }

    private void sortLogsByTimestamp(List<LogWriter.LogEntry> logs) {
        logs.sort(Comparator.comparingLong(a -> a.timestampMs));
    }

    private boolean shouldShowEntry(LogWriter.LogEntry entry) {
        if (!currentFilter.equals("ALL") && !entry.level.equals(currentFilter)) {
            return false;
        }
        if (!searchText.isEmpty()) {
            String lowerSearch = searchText.toLowerCase();
            return entry.message.toLowerCase().contains(lowerSearch) ||
                   entry.tag.toLowerCase().contains(lowerSearch);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private List<LogWriter.LogEntry> entries = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogWriter.LogEntry entry = entries.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        void setEntries(List<LogWriter.LogEntry> newEntries) {
            List<LogWriter.LogEntry> oldEntries = new ArrayList<>(entries);
            entries = new ArrayList<>(newEntries);
            
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldEntries.size();
                }

                @Override
                public int getNewListSize() {
                    return newEntries.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    LogWriter.LogEntry oldEntry = oldEntries.get(oldItemPosition);
                    LogWriter.LogEntry newEntry = newEntries.get(newItemPosition);
                    return oldEntry.timestampMs == newEntry.timestampMs &&
                           oldEntry.level.equals(newEntry.level) &&
                           oldEntry.tag.equals(newEntry.tag) &&
                           oldEntry.message.equals(newEntry.message);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    LogWriter.LogEntry oldEntry = oldEntries.get(oldItemPosition);
                    LogWriter.LogEntry newEntry = newEntries.get(newItemPosition);
                    return oldEntry.timestampMs == newEntry.timestampMs &&
                           Objects.equals(oldEntry.level, newEntry.level) &&
                           Objects.equals(oldEntry.tag, newEntry.tag) &&
                           Objects.equals(oldEntry.message, newEntry.message);
                }
            });
            diffResult.dispatchUpdatesTo(this);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTimestamp;
            TextView textLevel;
            TextView textTag;
            TextView textMessage;

            ViewHolder(View itemView) {
                super(itemView);
                textTimestamp = itemView.findViewById(R.id.text_timestamp);
                textLevel = itemView.findViewById(R.id.text_level);
                textTag = itemView.findViewById(R.id.text_tag);
                textMessage = itemView.findViewById(R.id.text_message);
            }

            void bind(LogWriter.LogEntry entry) {
                textTimestamp.setText(entry.timestamp);
                textLevel.setText(entry.level);
                textTag.setText(entry.tag);
                textMessage.setText(entry.message);

                int colorRes = switch (entry.level) {
                    case "DEBUG" -> R.color.log_debug;
                    case "INFO" -> R.color.log_info;
                    case "WARN" -> R.color.log_warn;
                    case "ERROR" -> R.color.log_error;
                    default -> R.color.log_unknown;
                };
                textLevel.setTextColor(getColor(colorRes));
            }
        }
    }
}
