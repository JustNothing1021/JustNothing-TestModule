package com.justnothing.testmodule.ui.analysis.hook;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.request.HookActionRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HookManagerActivity extends AppCompatActivity {

    private static final String[] STATUS_ORDER = {"ENABLED", "DISABLED", "INACTIVE"};

    private HookAnalysisViewModel viewModel;

    private TextView tvLastUpdateTime, tvHookStats;
    private LinearLayout layoutHookGroups;

    private String currentSearchText = "";
    private List<HookSnapshot.HookItem> allHooks = new ArrayList<>();
    private final Map<String, Boolean> groupExpandedMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_manager);

        initViews();
        initViewModel();
    }

    private void initViews() {
        tvLastUpdateTime = findViewById(R.id.tv_last_update_time);
        ((TextView) findViewById(R.id.tv_last_update_label)).setText(R.string.analysis_hook_last_update);

        tvHookStats = findViewById(R.id.tv_hook_stats);

        TextInputEditText etSearch = findViewById(R.id.et_search);
        layoutHookGroups = findViewById(R.id.layout_hook_groups);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().trim();
                rebuildGroupList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_refresh).setOnClickListener(v -> viewModel.queryHookList());
        findViewById(R.id.btn_add).setOnClickListener(v -> {
            Intent intent = new Intent(this, HookEditorActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.btn_clear_all).setOnClickListener(v -> showClearConfirmDialog());
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(HookAnalysisViewModel.class);

        viewModel.getHookData().observe(this, snapshot -> {
            if (snapshot == null) return;
            displayHookSnapshot(snapshot);
        });

        viewModel.getLastUpdateTime().observe(this, time -> {
            if (time != null) tvLastUpdateTime.setText(time);
        });

        viewModel.getActionResult().observe(this, result -> {
            if (result != null && result.isSuccessAction()) {
                if ("clear".equals(result.getMessage()) || result.getMessage().contains("Cleared")) {
                    String msg = getString(R.string.analysis_hook_cleared_success, snapshotTotalCount);
                    showToast(msg);
                } else if (result.getMessage().contains("removed")) {
                    showToast(getString(R.string.analysis_hook_removed_success));
                }
            }
        });

        viewModel.queryHookList();
    }

    private int snapshotTotalCount = 0;

    private void displayHookSnapshot(HookSnapshot snapshot) {
        tvHookStats.setText(getString(R.string.analysis_hook_overview_format, snapshot.totalHookCount(), snapshot.activeCount()));
        snapshotTotalCount = snapshot.totalHookCount();

        allHooks = new ArrayList<>(snapshot.hooks());
        groupExpandedMap.clear();
        rebuildGroupList();
    }

    private List<HookSnapshot.HookItem> getFilteredHooks() {
        List<HookSnapshot.HookItem> result = new ArrayList<>();
        for (HookSnapshot.HookItem item : allHooks) {
            if (currentSearchText.isEmpty()
                    || (item.className() != null && item.className().toLowerCase(Locale.getDefault()).contains(currentSearchText.toLowerCase(Locale.getDefault())))
                    || (item.methodName() != null && item.methodName().toLowerCase(Locale.getDefault()).contains(currentSearchText.toLowerCase(Locale.getDefault())))) {
                result.add(item);
            }
        }
        return result;
    }

    private void rebuildGroupList() {
        layoutHookGroups.removeAllViews();

        List<HookSnapshot.HookItem> filtered = getFilteredHooks();

        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(currentSearchText.isEmpty() ? getString(R.string.analysis_hook_empty) : getString(R.string.analysis_hook_search_empty));
            emptyTv.setTextAppearance(android.R.style.TextAppearance_Medium);
            emptyTv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            emptyTv.setGravity(android.view.Gravity.CENTER);
            emptyTv.setPadding(0, dpToPx(32), 0, dpToPx(32));
            layoutHookGroups.addView(emptyTv);
            return;
        }

        for (String status : STATUS_ORDER) {
            List<HookSnapshot.HookItem> statusHooks = new ArrayList<>();
            for (HookSnapshot.HookItem item : filtered) {
                if (status.equals(item.statusKey())) statusHooks.add(item);
            }
            if (statusHooks.isEmpty()) continue;

            boolean expanded = groupExpandedMap.containsKey(status) && Boolean.TRUE.equals(groupExpandedMap.get(status));
            buildGroupSection(status, statusHooks, expanded);
        }
    }

    private int getStatusColorRes(String status) {
        return switch (status) {
            case "ENABLED" -> R.color.light_green;
            case "DISABLED" -> R.color.yellow;
            case "INACTIVE" -> R.color.gray;
            default -> android.R.color.darker_gray;
        };
    }

    private int getStatusIndicatorColor(String status) {
        return switch (status) {
            case "ENABLED" -> ContextCompat.getColor(this, R.color.light_green);
            case "DISABLED" -> ContextCompat.getColor(this, R.color.yellow);
            case "INACTIVE" -> ContextCompat.getColor(this, R.color.gray);
            default -> ContextCompat.getColor(this, android.R.color.darker_gray);
        };
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "ENABLED" -> getString(R.string.analysis_hook_group_enabled, 0);
            case "DISABLED" -> getString(R.string.analysis_hook_group_disabled, 0);
            case "INACTIVE" -> getString(R.string.analysis_hook_group_inactive, 0);
            default -> status;
        };
    }

    private void buildGroupSection(String statusLabel, List<HookSnapshot.HookItem> hooks, boolean expanded) {
        LayoutInflater inflater = LayoutInflater.from(this);
        int color = getStatusIndicatorColor(statusLabel);

        LinearLayout headerLayout = (LinearLayout) inflater.inflate(
                R.layout.item_hook_group_header, layoutHookGroups, false);

        View leftBar = headerLayout.findViewById(R.id.view_header_left_bar);
        leftBar.setBackgroundColor(color);

        ImageView ivExpand = headerLayout.findViewById(R.id.iv_group_expand);
        ivExpand.setRotation(expanded ? 0f : 180f);

        TextView tvTitle = headerLayout.findViewById(R.id.tv_group_title);
        tvTitle.setText(getStatusLabel(statusLabel).replace("0", String.valueOf(hooks.size())));
        tvTitle.setTextColor(color);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);

        for (int i = 0; i < hooks.size(); i++) {
            HookSnapshot.HookItem item = hooks.get(i);
            View hookView = inflater.inflate(R.layout.item_hook, contentLayout, false);

            View indicator = hookView.findViewById(R.id.view_status_indicator);
            indicator.setBackgroundColor(getStatusIndicatorColor(item.statusKey()));

            TextView targetTv = hookView.findViewById(R.id.tv_hook_target);
            targetTv.setText(item.targetDisplay());

            TextView metaTv = hookView.findViewById(R.id.tv_hook_meta);
            StringBuilder meta = new StringBuilder();
            meta.append(item.phaseLabel());
            if (item.callCount() > 0) {
                meta.append(" · ").append(getString(R.string.analysis_hook_calls_format, item.callCount()));
            }
            metaTv.setText(meta.toString());

            TextView infoTv = hookView.findViewById(R.id.tv_hook_info);
            infoTv.setText(String.valueOf(item.callCount()));

            final HookSnapshot.HookItem capturedItem = item;
            hookView.setOnClickListener(v -> {
                Intent intent = new Intent(HookManagerActivity.this, HookDetailActivity.class);
                intent.putExtra(HookDetailActivity.EXTRA_HOOK_ITEM, capturedItem);
                startActivity(intent);
            });

            hookView.setOnLongClickListener(v -> {
                String text = "ID: " + capturedItem.id()
                        + "\nTarget: " + capturedItem.targetDisplay()
                        + "\nStatus: " + capturedItem.statusKey()
                        + "\nCalls: " + capturedItem.callCount()
                        + "\nPhases: " + capturedItem.phaseLabel();
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("hook_info", text));
                    android.widget.Toast.makeText(this, getString(R.string.analysis_thread_copy_success),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            contentLayout.addView(hookView);

            if (i < hooks.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0x0AFFFFFF);
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
            groupExpandedMap.put(statusLabel, !isExpanded);
        });

        layoutHookGroups.addView(headerLayout);
        layoutHookGroups.addView(contentLayout);
    }

    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_hook_confirm_clear_title)
                .setMessage(R.string.analysis_hook_confirm_clear_msg)
                .setPositiveButton(R.string.analysis_hook_btn_clear_all, (dialog, which) ->
                        viewModel.performAction(HookActionRequest.ACTION_CLEAR, null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.queryHookList();
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
