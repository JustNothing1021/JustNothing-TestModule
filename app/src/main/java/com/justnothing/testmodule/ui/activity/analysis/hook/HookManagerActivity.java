package com.justnothing.testmodule.ui.activity.analysis.hook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
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
import com.justnothing.testmodule.databinding.ActivityHookManagerBinding;
import com.justnothing.testmodule.databinding.ItemHookBinding;
import com.justnothing.testmodule.databinding.ItemHookGroupHeaderBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.HookAnalysisViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HookManagerActivity extends BaseActivity {

    private static final String[] STATUS_ORDER = {"ENABLED", "DISABLED", "INACTIVE"};

    private HookAnalysisViewModel viewModel;
    private ActivityHookManagerBinding binding;

    private String currentSearchText = "";
    private List<HookSnapshot.HookItem> allHooks = new ArrayList<>();
    private final Map<String, Boolean> groupExpandedMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHookManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
    }

    private void initViews() {
        binding.tvLastUpdateLabel.setText(R.string.analysis_hook_last_update);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().trim();
                rebuildGroupList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnRefresh.setOnClickListener(v -> viewModel.queryHookList());
        binding.btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, HookEditorActivity.class);
            startActivity(intent);
        });
        binding.btnClearAll.setOnClickListener(v -> showClearConfirmDialog());
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(HookAnalysisViewModel.class);

        viewModel.getHookData().observe(this, snapshot -> {
            if (snapshot == null) return;
            displayHookSnapshot(snapshot);
        });

        viewModel.getLastUpdateTime().observe(this, time -> {
            if (time != null) binding.tvLastUpdateTime.setText(time);
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
        binding.tvHookStats.setText(getString(R.string.analysis_hook_overview_format, snapshot.totalHookCount(), snapshot.activeCount()));
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
        binding.layoutHookGroups.removeAllViews();

        List<HookSnapshot.HookItem> filtered = getFilteredHooks();

        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(currentSearchText.isEmpty() ? getString(R.string.analysis_hook_empty) : getString(R.string.analysis_hook_search_empty));
            emptyTv.setTextAppearance(android.R.style.TextAppearance_Medium);
            emptyTv.setTextColor(MaterialColors.getColor(emptyTv, com.google.android.material.R.attr.colorOnSurfaceVariant));
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(0, dpToPx(32), 0, dpToPx(32));
            binding.layoutHookGroups.addView(emptyTv);
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

    private int getStatusIndicatorColor(String status) {
        return switch (status) {
            case "ENABLED" -> ContextCompat.getColor(this, R.color.light_green);
            case "DISABLED" -> ContextCompat.getColor(this, R.color.yellow);
            case "INACTIVE" -> ContextCompat.getColor(this, R.color.gray);
            default -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ContextCompat.getColor(this, R.color.gray));
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

        ItemHookGroupHeaderBinding headerBinding = ItemHookGroupHeaderBinding.inflate(
                inflater, binding.layoutHookGroups, false);
        LinearLayout headerLayout = headerBinding.getRoot();

        headerBinding.viewHeaderLeftBar.setBackgroundColor(color);

        ImageView ivExpand = headerBinding.ivGroupExpand;
        ivExpand.setRotation(expanded ? 0f : 180f);

        headerBinding.tvGroupTitle.setText(getStatusLabel(statusLabel).replace("0", String.valueOf(hooks.size())));
        headerBinding.tvGroupTitle.setTextColor(color);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);

        for (int i = 0; i < hooks.size(); i++) {
            HookSnapshot.HookItem item = hooks.get(i);
            ItemHookBinding hookBinding = ItemHookBinding.inflate(inflater, contentLayout, false);

            hookBinding.viewStatusIndicator.setBackgroundColor(getStatusIndicatorColor(item.statusKey()));

            TextView targetTv = hookBinding.tvHookTarget;
            targetTv.setText(item.targetDisplay());

            TextView metaTv = hookBinding.tvHookMeta;
            StringBuilder meta = new StringBuilder();
            meta.append(item.phaseLabel());
            if (item.callCount() > 0) {
                meta.append(" · ").append(getString(R.string.analysis_hook_calls_format, item.callCount()));
            }
            metaTv.setText(meta.toString());

            TextView infoTv = hookBinding.tvHookInfo;
            infoTv.setText(String.valueOf(item.callCount()));

            View hookView = hookBinding.getRoot();

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
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("hook_info", text));
                    showToast(getString(R.string.analysis_thread_copy_success));
                }
                return true;
            });

            contentLayout.addView(hookView);

            if (i < hooks.size() - 1) {
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
            groupExpandedMap.put(statusLabel, !isExpanded);
        });

        binding.layoutHookGroups.addView(headerLayout);
        binding.layoutHookGroups.addView(contentLayout);
    }

    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_hook_confirm_clear_title)
                .setMessage(R.string.analysis_hook_confirm_clear_msg)
                .setPositiveButton(R.string.analysis_hook_btn_clear_all, (dialog, which) ->
                        viewModel.performAction("clear", null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
