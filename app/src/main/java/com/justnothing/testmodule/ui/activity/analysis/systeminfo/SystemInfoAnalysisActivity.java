package com.justnothing.testmodule.ui.activity.analysis.systeminfo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.system.model.SystemFieldInfo;
import com.justnothing.testmodule.databinding.ActivitySystemInfoAnalysisBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.SystemInfoQueryViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemInfoAnalysisActivity extends BaseActivity {

    private SystemInfoQueryViewModel viewModel;
    private ActivitySystemInfoAnalysisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySystemInfoAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
        setupListeners();

        viewModel.querySystemInfo();
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_system_info));
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SystemInfoQueryViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutResult.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            }
            binding.btnRefresh.setEnabled(!isLoading);
        });

        viewModel.getFields().observe(this, this::displayFields);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutResult.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.tvErrorMessage.setText(error);
            }
        });
    }

    private void setupListeners() {
        binding.btnRefresh.setOnClickListener(v -> viewModel.querySystemInfo());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void displayFields(List<SystemFieldInfo> allFields) {
        if (allFields == null || allFields.isEmpty()) return;

        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        binding.tvTotalCount.setText(getString(R.string.analysis_system_info_total_format, allFields.size()));

        binding.layoutTablesContainer.removeAllViews();

        TextView tipView = new TextView(this);
        tipView.setText(getString(R.string.analysis_system_info_tip));
        tipView.setTextAppearance(R.style.TextAppearance_App_Label);
        tipView.setTextColor(ContextCompat.getColor(this, R.color.grey_800));
        tipView.setGravity(Gravity.CENTER);
        tipView.setPadding(0, dpToPx(4), 0, dpToPx(8));
        binding.layoutTablesContainer.addView(tipView);

        String currentCategory = null;
        TableLayout currentTable = null;

        for (SystemFieldInfo field : allFields) {

            if (!field.getCategory().equals(currentCategory)) {
                currentCategory = field.getCategory();

                LinearLayout headerSection = createSectionHeader(currentCategory);
                binding.layoutTablesContainer.addView(headerSection);

                currentTable = createTable();
                binding.layoutTablesContainer.addView(currentTable);
            }

            addTableRow(currentTable, field.getLabel(), field.getValue());
        }
    }

    private LinearLayout createSectionHeader(String category) {
        String localizedCategory = localizeCategory(category);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int paddingVert = dpToPx(12);
        header.setPadding(dpToPx(16), paddingVert, dpToPx(16), dpToPx(4));

        TextView titleView = new TextView(this);
        titleView.setText(localizedCategory);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextAppearance(R.style.TextAppearance_App_Body);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.purple_700));
        header.addView(titleView);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(2)));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));
        int marginVert = dpToPx(4);
        divider.setPadding(0, marginVert, 0, marginVert);
        header.addView(divider);

        return header;
    }

    private TableLayout createTable() {
        TableLayout table = new TableLayout(this);
        table.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(true);
        int padding = dpToPx(8);
        table.setPadding(padding, 0, padding, padding);
        table.setBackgroundColor(Color.WHITE);

        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_100));

        TextView labelHeader = createLabelCell(getString(R.string.analysis_system_info_field_label), true);
        labelHeader.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.35f));

        TextView valueHeader = createValueCell(getString(R.string.analysis_system_info_field_value), true);
        valueHeader.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.65f));

        headerRow.addView(labelHeader);
        headerRow.addView(valueHeader);
        table.addView(headerRow);

        return table;
    }

    private void addTableRow(TableLayout table, String label, String value) {
        TableRow row = new TableRow(this);

        final String copyText = (value != null && !value.isEmpty()) ? value : label;

        TextView labelView = createLabelCell(label, false);
        labelView.setTypeface(null, Typeface.BOLD);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.grey_800));
        labelView.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.35f));

        TextView valueView = createValueCell(value, false);
        valueView.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.65f));

        row.addView(labelView);
        row.addView(valueView);
        table.addView(row);

        row.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("system_info_value", copyText);
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.analysis_system_info_copy_success));
        });
    }

    private TextView createLabelCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(R.style.TextAppearance_App_Caption);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        int paddingH = dpToPx(12);
        int paddingV = dpToPx(8);
        tv.setPadding(paddingH, paddingV, paddingH, paddingV);
        return tv;
    }

    private TextView createValueCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "");
        tv.setTextAppearance(R.style.TextAppearance_App_Caption);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (!isHeader) {
            tv.setTypeface(Typeface.MONOSPACE);
        }
        int paddingH = dpToPx(12);
        int paddingV = dpToPx(8);
        tv.setPadding(paddingH, paddingV, paddingH, paddingV);
        return tv;
    }

    private String localizeCategory(String categoryKey) {
        Map<String, Integer> map = new HashMap<>();
        map.put("os_info", R.string.sys_category_os_info);
        map.put("cpu_info", R.string.sys_category_cpu_info);
        map.put("memory_info", R.string.sys_category_memory_info);
        map.put("system_props", R.string.sys_category_system_props);

        Integer resId = map.get(categoryKey);
        if (resId != null) {
            return getString(resId);
        }
        return categoryKey;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
