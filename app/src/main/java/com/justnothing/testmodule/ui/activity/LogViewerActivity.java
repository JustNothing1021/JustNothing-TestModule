package com.justnothing.testmodule.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityLogViewerBinding;
import com.justnothing.testmodule.databinding.ItemLogEntryBinding;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.logging.LogWriter;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LogViewerActivity extends BaseActivity {

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

    private ActivityLogViewerBinding binding;

    // 这个界面故意不用父类那个写文件的 logger：它显示的就是日志文件本身，
    // 再把自己看日志的动作写进去，就成了自我引用的噪声（每刷新一次多一条）。
    // 所以这里单独持有 ViewerLogger，只用不落盘的两个方法。
    private final ViewerLogger viewerLogger = new ViewerLogger();

    private LogWriter logWriter;
    private LogAdapter adapter;
    private Runnable updateRunnable;
    private boolean autoScroll = true;
    private String currentFilter = "ALL";
    private String searchText = "";
    private static final int MAX_DISPLAY_LOGS = 100;
    private static final int LOAD_MORE_INCREMENT = 100;
    private static final int REFRESH_INTERVAL = 30000;
    private int currentDisplayLimit = MAX_DISPLAY_LOGS;
    private String lastLogHash = "";
    private List<LogWriter.LogEntry> allCachedLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logWriter = new LogWriter();

        initViews();
        startAutoUpdate();
    }

    private void initViews() {
        RecyclerView recyclerView = binding.logList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
                mainHandler.post(() -> adapter.setEntries(filteredLogs));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClear.setOnClickListener(v -> {
            logWriter.clearLogs();
            refreshLogs();
            viewerLogger.infoWithoutFile("日志已清除");
        });

        binding.btnAll.setOnClickListener(v -> setFilter("ALL"));
        binding.btnDebug.setOnClickListener(v -> setFilter("DEBUG"));
        binding.btnInfo.setOnClickListener(v -> setFilter("INFO"));
        binding.btnWarn.setOnClickListener(v -> setFilter("WARN"));
        binding.btnError.setOnClickListener(v -> setFilter("ERROR"));

        binding.btnAutoScroll.setOnClickListener(v -> {
            autoScroll = !autoScroll;
            binding.btnAutoScroll.setText(autoScroll ? getString(R.string.auto_scroll_on) : getString(R.string.auto_scroll_off));
        });

        binding.btnLoadMore.setOnClickListener(v -> {
            currentDisplayLimit += LOAD_MORE_INCREMENT;
            List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
            mainHandler.post(() -> {
                adapter.setEntries(filteredLogs);
                viewerLogger.infoWithoutFile("加载更多日志，当前显示限制: " + currentDisplayLimit);
            });
        });

        refreshLogs();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
        mainHandler.post(() -> {
            adapter.setEntries(filteredLogs);
            viewerLogger.infoWithoutFile("切换日志过滤: " + filter);
        });
    }

    private void startAutoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                mainHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        mainHandler.post(updateRunnable);
    }

    private void refreshLogs() {
        mainHandler.post(() -> {
            if (binding.progressLoading != null) {
                binding.progressLoading.setVisibility(View.VISIBLE);
            }
        });

        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                String logsText = DataBridge.readLogs();
                String currentHash = String.valueOf(logsText.hashCode());

                if (!currentHash.equals(lastLogHash)) {
                    lastLogHash = currentHash;
                    
                    List<LogWriter.LogEntry> newEntries = new ArrayList<>();
                    if (!logsText.isEmpty()) {
                        String[] lines = logsText.split("\n");

                        viewerLogger.infoWithoutFile("日志行数: " + lines.length);

                        for (String line : lines) {
                            if (line.trim().isEmpty()) continue;
                            try {
                                if (LogWriter.LogEntry.isHeaderLine(line)) {
                                    newEntries.add(LogWriter.LogEntry.fromString(line));
                                } else if (!newEntries.isEmpty()) {
                                    // 多行正文的续行（堆栈、JSON 报文）：并进上一条。
                                    // 不能新开一条 —— 那样它会拿到「当前时间」当时间戳，
                                    // 排序时永远排在最后，自动滚动又正好停在末尾，一屏就全是 UnknownTag。
                                    int last = newEntries.size() - 1;
                                    newEntries.set(last, newEntries.get(last).withAppendedLine(line));
                                }
                            } catch (Exception e) {
                                viewerLogger.errorWithoutFile("解析日志失败: " + line, e);
                            }
                        }
                    }
                    
                    allCachedLogs = newEntries;
                    sortLogsByTimestamp(allCachedLogs);
                    
                    List<LogWriter.LogEntry> filteredLogs = filterLogs(allCachedLogs);
                    
                    mainHandler.post(() -> {
                        adapter.setEntries(filteredLogs);
                        if (autoScroll) {
                            RecyclerView recyclerView = binding.logList;
                            if (recyclerView != null && recyclerView.getLayoutManager() != null) {
                                recyclerView.getLayoutManager().scrollToPosition(filteredLogs.size() - 1);
                            }
                        }
                        if (binding.progressLoading != null) {
                            binding.progressLoading.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (binding.progressLoading != null) {
                            binding.progressLoading.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                viewerLogger.errorWithoutFile("刷新日志失败", e);
                mainHandler.post(() -> {
                    if (binding.progressLoading != null) {
                        binding.progressLoading.setVisibility(View.GONE);
                    }
                });
            }
        });
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
        if (updateRunnable != null) {
            mainHandler.removeCallbacks(updateRunnable);
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private List<LogWriter.LogEntry> entries = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemLogEntryBinding itemBinding = ItemLogEntryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
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
            private final ItemLogEntryBinding binding;

            ViewHolder(ItemLogEntryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(LogWriter.LogEntry entry) {
                binding.textTimestamp.setText(entry.timestamp);
                binding.textLevel.setText(entry.level);
                binding.textTag.setText(entry.tag);
                binding.textMessage.setText(entry.message);

                int colorRes = switch (entry.level) {
                    case "DEBUG" -> R.color.log_debug;
                    case "INFO" -> R.color.log_info;
                    case "WARN" -> R.color.log_warn;
                    case "ERROR" -> R.color.log_error;
                    default -> R.color.log_unknown;
                };
                binding.textLevel.setTextColor(getColor(colorRes));
            }
        }
    }
}
