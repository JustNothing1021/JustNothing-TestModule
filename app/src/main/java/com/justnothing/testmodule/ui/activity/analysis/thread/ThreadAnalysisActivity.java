package com.justnothing.testmodule.ui.activity.analysis.thread;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.MaterialColors;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDeadlockResult;
import com.justnothing.testmodule.command.functions.threads.response.ThreadDetail;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.databinding.ActivityThreadAnalysisBinding;
import com.justnothing.testmodule.databinding.ItemThreadBinding;
import com.justnothing.testmodule.databinding.ItemThreadGroupHeaderBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.ThreadAnalysisViewModel;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThreadAnalysisActivity extends BaseActivity {

    private static final String[] STATE_ORDER = {
            "RUNNABLE", "BLOCKED", "WAITING", "TIMED_WAITING", "TERMINATED", "NEW"
    };

    private static final int SORT_NAME = 0;
    private static final int SORT_PRIORITY = 1;
    private static final int SORT_ID = 2;

    private ThreadAnalysisViewModel viewModel;
    private ActivityThreadAnalysisBinding binding;

    private String currentSearchText = "";
    private List<ThreadSnapshot.ThreadItem> allThreads = new ArrayList<>();
    private Map<String, Boolean> groupExpandedMap = new LinkedHashMap<>();

    private int sortMode = SORT_NAME;
    private boolean sortDescending = false;

    private boolean hasInitialData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThreadAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
    }

    private void initViews() {
        binding.tvLastUpdateLabel.setText(R.string.analysis_thread_last_update);

        binding.switchAutoRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setAutoRefresh(isChecked));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().trim();
                rebuildGroupList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnRefresh.setOnClickListener(v -> viewModel.queryThreadInfo(true));
        binding.btnExport.setOnClickListener(v -> exportDump());
        binding.btnDeadlockDetect.setOnClickListener(v -> showDeadlockConfirmDialog());
        binding.btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ThreadAnalysisViewModel.class);

        binding.switchAutoRefresh.setOnCheckedChangeListener(null);
        binding.switchAutoRefresh.setChecked(viewModel.isAutoRefresh().getValue() != null && viewModel.isAutoRefresh().getValue());
        binding.switchAutoRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setAutoRefresh(isChecked));

        viewModel.getThreadData().observe(this, snapshot -> {
            if (snapshot == null) return;
            hasInitialData = true;
            displayThreadSnapshot(snapshot);
        });


        viewModel.getLastUpdateTime().observe(this, time -> {
            if (time != null) binding.tvLastUpdateTime.setText(time);
        });


        viewModel.getDeadlockResult().observe(this, result -> {
            if (result != null && result.isSuccess()) displayDeadlockResult(result);
        });

        viewModel.queryThreadInfo(true);
    }

    private void displayThreadSnapshot(ThreadSnapshot snapshot) {
        binding.tvTotalCount.setText(getString(R.string.analysis_thread_total_format, snapshot.totalCount()));

        allThreads = new ArrayList<>(snapshot.threads());
        groupExpandedMap.clear();
        rebuildGroupList();
    }

    private List<ThreadSnapshot.ThreadItem> getFilteredThreads() {
        List<ThreadSnapshot.ThreadItem> result = new ArrayList<>();
        for (ThreadSnapshot.ThreadItem item : allThreads) {
            if (currentSearchText.isEmpty()
                    || (item.name() != null && item.name().toLowerCase(Locale.getDefault()).contains(currentSearchText.toLowerCase(Locale.getDefault())))) {
                result.add(item);
            }
        }
        Comparator<ThreadSnapshot.ThreadItem> comparator;
        switch (sortMode) {
            case SORT_PRIORITY:
                comparator = Comparator.comparingInt(ThreadSnapshot.ThreadItem::priority);
                break;
            case SORT_ID:
                comparator = Comparator.comparingLong(ThreadSnapshot.ThreadItem::threadId);
                break;
            default:
                comparator = (a, b) -> {
                    String na = a.name() != null ? a.name() : "";
                    String nb = b.name() != null ? b.name() : "";
                    return na.compareToIgnoreCase(nb);
                };
                break;
        }
        Collections.sort(result, sortDescending ? comparator.reversed() : comparator);
        return result;
    }

    private void rebuildGroupList() {
        binding.layoutThreadGroups.removeAllViews();

        List<ThreadSnapshot.ThreadItem> filtered = getFilteredThreads();

        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(currentSearchText.isEmpty() ? getString(R.string.analysis_thread_empty) : getString(R.string.analysis_thread_search_empty));
            emptyTv.setTextAppearance(android.R.style.TextAppearance_Medium);
            emptyTv.setTextColor(MaterialColors.getColor(emptyTv, com.google.android.material.R.attr.colorOnSurfaceVariant));
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(0, dpToPx(32), 0, dpToPx(32));
            binding.layoutThreadGroups.addView(emptyTv);
            return;
        }

        for (String state : STATE_ORDER) {
            List<ThreadSnapshot.ThreadItem> stateThreads = new ArrayList<>();
            for (ThreadSnapshot.ThreadItem item : filtered) {
                String groupKey = getGroupKey(item.state());
                if (groupKey.equals(state)) stateThreads.add(item);
            }

            if (stateThreads.isEmpty()) continue;

            boolean expanded = groupExpandedMap.containsKey(state) && groupExpandedMap.get(state);
            buildGroupSection(state, stateThreads, expanded);
        }

        List<ThreadSnapshot.ThreadItem> otherThreads = new ArrayList<>();
        for (ThreadSnapshot.ThreadItem item : filtered) {
            String groupKey = getGroupKey(item.state());
            if ("OTHER".equals(groupKey)) otherThreads.add(item);
        }
        if (!otherThreads.isEmpty()) {
            boolean expanded = groupExpandedMap.containsKey("OTHER") && groupExpandedMap.get("OTHER");
            buildGroupSection("OTHER", otherThreads, expanded);
        }
    }

    private String getGroupKey(String state) {
        if ("RUNNABLE".equals(state)) return "RUNNABLE";
        if ("BLOCKED".equals(state)) return "BLOCKED";
        if ("WAITING".equals(state)) return "WAITING";
        if ("TIMED_WAITING".equals(state)) return "TIMED_WAITING";
        return "OTHER";
    }

    private int getStateColor(String state) {
        if (state == null) return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                ContextCompat.getColor(this, R.color.gray));
        return switch (state) {
            case "RUNNABLE" -> ContextCompat.getColor(this, R.color.light_green);
            case "BLOCKED" -> ContextCompat.getColor(this, R.color.red);
            case "WAITING" -> ContextCompat.getColor(this, R.color.yellow);
            case "TIMED_WAITING" -> ContextCompat.getColor(this, R.color.magenta);
            default -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ContextCompat.getColor(this, R.color.gray));
        };
    }

    private int getPriorityColor(int priority, String state) {
        float ratio = Math.max(0f, Math.min(1f, (priority - 1) / 9f));
        int startColor, endColor;
        switch (state) {
            case "RUNNABLE":
                startColor = 0xFFB8E6A0;
                endColor = 0xFF2E7D32;
                break;
            case "BLOCKED":
                startColor = 0xFFFFCDD2;
                endColor = 0xFFB71C1C;
                break;
            case "WAITING":
                startColor = 0xFFFFF9C4;
                endColor = 0xFFFF8F00;
                break;
            case "TIMED_WAITING":
                startColor = 0xFFF3E5F5;
                endColor = 0xFF7B1FA2;
                break;
            default:
                startColor = 0xFFBDBDBD;
                endColor = 0xFF424242;
                break;
        }
        return interpolateColor(startColor, endColor, ratio);
    }

    private static int interpolateColor(int start, int end, float ratio) {
        int a = (int) (Color.alpha(start) * (1 - ratio) + Color.alpha(end) * ratio);
        int r = (int) (Color.red(start) * (1 - ratio) + Color.red(end) * ratio);
        int g = (int) (Color.green(start) * (1 - ratio) + Color.green(end) * ratio);
        int b = (int) (Color.blue(start) * (1 - ratio) + Color.blue(end) * ratio);
        return Color.argb(a, r, g, b);
    }

    private void buildGroupSection(String stateLabel, List<ThreadSnapshot.ThreadItem> threads, boolean expanded) {
        LayoutInflater inflater = LayoutInflater.from(this);
        int color = getStateColor(stateLabel);

        ItemThreadGroupHeaderBinding headerBinding = ItemThreadGroupHeaderBinding.inflate(
                inflater, binding.layoutThreadGroups, false);
        LinearLayout headerLayout = headerBinding.getRoot();

        headerBinding.viewHeaderLeftBar.setBackgroundColor(color);

        ImageView ivExpand = headerBinding.ivGroupExpand;
        ivExpand.setImageResource(expanded ? R.drawable.ic_expand_more : R.drawable.ic_expand_more);
        ivExpand.setRotation(expanded ? 180f : 0f);

        headerBinding.tvGroupTitle.setText(getString(R.string.analysis_thread_group_format, stateLabel, threads.size()));
        headerBinding.tvGroupTitle.setTextColor(color);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);

        for (int i = 0; i < threads.size(); i++) {
            ThreadSnapshot.ThreadItem item = threads.get(i);
            ItemThreadBinding threadBinding = ItemThreadBinding.inflate(inflater, contentLayout, false);

            threadBinding.viewStateIndicator.setBackgroundColor(getStateColor(item.state()));

            TextView nameTv = threadBinding.tvThreadName;
            nameTv.setText(item.name());
            nameTv.setTextColor(ContextCompat.getColor(this,
                    "BLOCKED".equals(item.state()) ? R.color.red :
                    "RUNNABLE".equals(item.state()) ? R.color.light_green :
                    "WAITING".equals(item.state()) ? R.color.yellow :
                    "TIMED_WAITING".equals(item.state()) ? R.color.magenta : R.color.cyan));

            TextView infoTv = threadBinding.tvThreadInfo;
            String infoText = item.daemon() ? "D," + item.priority() : String.valueOf(item.priority());
            infoTv.setText(infoText);
            infoTv.setTextColor(getPriorityColor(item.priority(), item.state()));

            View threadView = threadBinding.getRoot();

            final ThreadSnapshot.ThreadItem capturedItem = item;
            threadView.setOnClickListener(v -> {
                Intent intent = new Intent(ThreadAnalysisActivity.this, ThreadDetailActivity.class);
                intent.putExtra(ThreadDetailActivity.EXTRA_THREAD_ITEM, capturedItem);
                startActivity(intent);
            });
            threadView.setOnLongClickListener(v -> {
                String text = "Name: " + capturedItem.name()
                        + "\nID: " + capturedItem.threadId()
                        + "\nState: " + capturedItem.state()
                        + "\nPriority: " + capturedItem.priority()
                        + "\nDaemon: " + capturedItem.daemon();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("thread_info", text));
                    showToast(R.string.analysis_thread_copy_success);
                }
                return true;
            });

            contentLayout.addView(threadView);

            if (i < threads.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(MaterialColors.getColor(divider, com.google.android.material.R.attr.colorOutlineVariant));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(divParams);
                contentLayout.addView(divider);
            }
        }

        headerLayout.setOnClickListener(v -> {
            boolean isExpanded = contentLayout.getVisibility() == View.VISIBLE;
            contentLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ivExpand.setRotation(isExpanded ? 0f : 180f);
            groupExpandedMap.put(stateLabel, !isExpanded);
        });

        binding.layoutThreadGroups.addView(headerLayout);
        binding.layoutThreadGroups.addView(contentLayout);
    }

    private void displayDeadlockResult(ThreadDeadlockResult result) {
        binding.tvDeadlockResult.setVisibility(View.VISIBLE);
        binding.scrollDeadlockDetail.setVisibility(View.VISIBLE);

        String statusText;
        if (result.isHasDeadlock()) {
            statusText = getString(R.string.analysis_thread_deadlock_found, result.getBlockedThreadCount());
            binding.tvDeadlockResult.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            statusText = getString(R.string.analysis_thread_deadlock_none);
            binding.tvDeadlockResult.setTextColor(ContextCompat.getColor(this, R.color.light_green));
        }
        binding.tvDeadlockResult.setText(statusText);

        if (result.getBlockedThreads() != null && !result.getBlockedThreads().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.analysis_thread_blocked_threads)).append("\n\n");
            for (ThreadDetail t : result.getBlockedThreads()) {
                sb.append(getString(R.string.analysis_thread_dump_name, t.getName())).append("\n");
                sb.append(getString(R.string.analysis_thread_dump_id, String.valueOf(t.getThreadId()))).append("\n");
                sb.append(getString(R.string.analysis_thread_dump_state, t.getState())).append("\n");

                if (t.getStackTrace() != null && !t.getStackTrace().isEmpty()) {
                    sb.append("  Stack:\n");
                    for (String frame : t.getStackTrace()) {
                        sb.append(frame).append("\n");
                    }
                }
                sb.append("\n");
            }
            binding.tvDeadlockDetail.setText(sb.toString());
        } else {
            binding.tvDeadlockDetail.setText("");
            binding.scrollDeadlockDetail.setVisibility(View.GONE);
        }
    }

    private void showDeadlockConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_thread_deadlock_confirm_title)
                .setMessage(R.string.analysis_thread_deadlock_confirm_msg)
                .setPositiveButton(R.string.analysis_thread_btn_detect, (dialog, which) -> viewModel.detectDeadlock())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSortDialog() {
        String[] sortOptions = {
                getString(R.string.analysis_thread_sort_name),
                getString(R.string.analysis_thread_sort_priority),
                getString(R.string.analysis_thread_sort_id)
        };
        int checkedItem = sortMode;
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_thread_sort_title)
                .setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
                    sortMode = which;
                    dialog.dismiss();
                    showSortOrderDialog();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSortOrderDialog() {
        String[] orderOptions = {
                getString(R.string.analysis_thread_sort_asc),
                getString(R.string.analysis_thread_sort_desc)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_thread_sort_title)
                .setSingleChoiceItems(orderOptions, sortDescending ? 1 : 0, (dialog, which) -> {
                    sortDescending = which == 1;
                    dialog.dismiss();
                    rebuildGroupList();
                })
                .show();
    }

    private void exportDump() {
        ThreadSnapshot data = viewModel.getThreadData().getValue();
        if (data == null) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());

            File exportDir = new File(
                    Environment.getExternalStorageDirectory(),
                    FileDirectory.EXPORT_DIR_NAME + "/thread_dumps");
            IOManager.createDirectory(exportDir.getAbsolutePath());

            File file = new File(exportDir, "thread_dump_" + timestamp + ".txt");

            StringBuilder content = new StringBuilder();
            content.append("===== Thread Dump Report =====\n");
            content.append("Time: ").append(sdf.format(new Date(data.timestamp()))).append("\n");
            content.append("Total Threads: ").append(data.totalCount()).append("\n\n");

            ThreadSnapshot.StateStats stats = data.stateStats();
            content.append("===== State Statistics =====\n");
            content.append("  RUNNABLE: ").append(stats.runnable()).append("\n");
            content.append("  BLOCKED: ").append(stats.blocked()).append("\n");
            content.append("  WAITING: ").append(stats.waiting()).append("\n");
            content.append("  TIMED_WAITING: ").append(stats.timedWaiting()).append("\n");
            content.append("  TERMINATED: ").append(stats.terminated()).append("\n");
            content.append("  NEW: ").append(stats.nnew()).append("\n\n");

            content.append("===== Thread Details =====\n");
            for (ThreadSnapshot.ThreadItem item : data.threads()) {
                content.append("--- ").append(item.name()).append(" ---\n");
                content.append("  ID: ").append(item.threadId()).append("\n");
                content.append("  State: ").append(item.state()).append("\n");
                content.append("  Priority: ").append(item.priority()).append("\n");
                content.append("  Daemon: ").append(item.daemon()).append("\n");
                content.append("  Interrupted: ").append(item.interrupted()).append("\n");
                content.append("  Alive: ").append(item.alive()).append("\n");

                if (!item.stackTrace().isEmpty()) {
                    content.append("  Stack Trace:\n");
                    for (String frame : item.stackTrace()) {
                        content.append(frame).append("\n");
                    }
                }
                content.append("\n");
            }

            IOManager.writeFile(file.getAbsolutePath(), content.toString());

            new AlertDialog.Builder(this)
                    .setTitle(R.string.analysis_thread_export_success)
                    .setMessage(getString(R.string.analysis_thread_export_path, file.getAbsolutePath()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

        } catch (Exception e) {
            showToast(getString(R.string.analysis_thread_error_export_failed, e.getMessage()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasInitialData = false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
