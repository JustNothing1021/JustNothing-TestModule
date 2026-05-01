package com.justnothing.testmodule.ui.activity.analysis.hook;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.hook.HookActionRequest;
import com.justnothing.testmodule.command.functions.hook.HookAddResult;
import com.justnothing.testmodule.ui.viewmodel.analysis.HookAnalysisViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_HOOK_ITEM = "hook_item";

    private HookAnalysisViewModel viewModel;
    private String currentHookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        HookSnapshot.HookItem item = (HookSnapshot.HookItem) getIntent().getSerializableExtra(EXTRA_HOOK_ITEM);
        if (item == null) {
            finish();
            return;
        }

        currentHookId = item.id();
        setupToolbar(item);
        displayHookInfo(item);

        viewModel = new ViewModelProvider(this).get(HookAnalysisViewModel.class);
        viewModel.getActionResult().observe(this, result -> {
            if (result != null && result.isSuccessAction()) {
                refreshOutput();
            }
        });

        setupButtons();

        refreshOutput();
    }

    private void setupToolbar(HookSnapshot.HookItem item) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(item.id());
        }
    }

    private void displayHookInfo(HookSnapshot.HookItem item) {
        TextView tvId = findViewById(R.id.tv_detail_id);
        TextView tvTarget = findViewById(R.id.tv_detail_target);
        TextView tvStatus = findViewById(R.id.tv_detail_status);
        TextView tvCallCount = findViewById(R.id.tv_detail_call_count);
        TextView tvCreateTime = findViewById(R.id.tv_detail_create_time);
        TextView tvSignature = findViewById(R.id.tv_detail_signature);

        if (tvId != null) tvId.setText(item.id());
        if (tvTarget != null) tvTarget.setText(item.targetDisplay());

        if (tvStatus != null) {
            tvStatus.setText(getStatusLabel(item.statusKey()));
            int color = getStatusColor(item.statusKey());
            tvStatus.setTextColor(color);
        }

        if (tvCallCount != null) tvCallCount.setText(String.valueOf(item.callCount()));

        if (tvCreateTime != null && item.createTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            tvCreateTime.setText(sdf.format(new Date(item.createTime())));
        }

        if (tvSignature != null) {
            if (item.signature() != null && !item.signature().isEmpty()) {
                tvSignature.setText(item.signature());
            } else {
                tvSignature.setText("(auto)");
                tvSignature.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        }

        showCodeSection(R.id.label_before_code, R.id.tv_detail_before_code, item.hasBefore(), item.beforeCodePreview());
        showCodeSection(R.id.label_after_code, R.id.tv_detail_after_code, item.hasAfter(), item.afterCodePreview());
        showCodeSection(R.id.label_replace_code, R.id.tv_detail_replace_code, item.hasReplace(), item.replaceCodePreview());
    }

    private void showCodeSection(int labelId, int textId, boolean hasPhase, String codePreview) {
        View label = findViewById(labelId);
        TextView textView = findViewById(textId);
        if (label == null || textView == null) return;

        if (hasPhase && codePreview != null && !codePreview.isEmpty()) {
            label.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            textView.setText(codePreview);
        } else {
            label.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        }
    }

    private int getStatusColor(String status) {
        return switch (status) {
            case "ENABLED" -> ContextCompat.getColor(this, R.color.light_green);
            case "DISABLED" -> ContextCompat.getColor(this, R.color.yellow);
            default -> ContextCompat.getColor(this, R.color.gray);
        };
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "ENABLED" -> getString(R.string.analysis_hook_status_enabled);
            case "DISABLED" -> getString(R.string.analysis_hook_status_disabled);
            default -> getString(R.string.analysis_hook_status_inactive);
        };
    }

    private void setupButtons() {
        findViewById(R.id.btn_remove).setOnClickListener(v -> showRemoveConfirmDialog());

        findViewById(R.id.btn_toggle_enable).setOnClickListener(v -> {
            viewModel.performAction(
                    HookActionRequest.ACTION_ENABLE,
                    currentHookId
            );
        });

        findViewById(R.id.btn_refresh_output).setOnClickListener(v -> {
            viewModel.performAction(HookActionRequest.ACTION_OUTPUT, currentHookId, 50);
        });
    }

    private void refreshOutput() {
        viewModel.performAction(HookActionRequest.ACTION_OUTPUT, currentHookId, 50);

        viewModel.getActionResult().observe(this, result -> {
            if (result != null && result.getDetail() != null && !result.getDetail().isEmpty()) {
                TextView outputTv = findViewById(R.id.tv_detail_output);
                if (outputTv != null) {
                    StringBuilder sb = new StringBuilder();
                    for (HookAddResult.HookDetailInfo info : result.getDetail()) {
                        sb.append(info.getKey()).append(": ").append(info.getValue()).append("\n");
                    }
                    String text = sb.toString().trim();
                    outputTv.setText(text.isEmpty() ? getString(R.string.analysis_hook_detail_no_output) : text);
                }
            }
        });
    }

    private void showRemoveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.analysis_hook_confirm_remove_title)
                .setMessage(getString(R.string.analysis_hook_confirm_remove_msg, currentHookId))
                .setPositiveButton(R.string.analysis_hook_detail_btn_remove, (dialog, which) -> {
                    viewModel.performAction(HookActionRequest.ACTION_REMOVE, currentHookId);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
