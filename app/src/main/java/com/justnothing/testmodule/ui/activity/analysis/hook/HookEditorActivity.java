package com.justnothing.testmodule.ui.activity.analysis.hook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityHookEditorBinding;
import com.justnothing.testmodule.databinding.ItemHookParamInputBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.HookAnalysisViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HookEditorActivity extends BaseActivity {

    private ActivityHookEditorBinding binding;

    private boolean signatureExpanded = false;

    private List<ParamHolder> paramHolders = new ArrayList<>();

    private HookAnalysisViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHookEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        viewModel = new ViewModelProvider(this).get(HookAnalysisViewModel.class);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showToast(error, Toast.LENGTH_LONG);
            }
        });

        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                if (result.isSuccessAction()) {
                    showToast(R.string.analysis_hook_add_success);
                    finish();
                } else {
                    String msg = result.getMessage() != null ? result.getMessage() : getString(R.string.analysis_hook_add_failed);
                    showToast(msg, Toast.LENGTH_LONG);
                }
            }
        });
    }

    private void initViews() {
        binding.layoutSignatureHeader.setOnClickListener(v -> toggleSignaturePanel());

        binding.btnAddParam.setOnClickListener(v -> addParamInput());

        binding.cbBefore.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutBeforeCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        binding.cbAfter.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutAfterCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        binding.cbReplace.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutReplaceCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        binding.btnSubmit.setOnClickListener(v -> submitHook());
    }

    private void toggleSignaturePanel() {
        signatureExpanded = !signatureExpanded;
        binding.layoutParams.setVisibility(signatureExpanded ? View.VISIBLE : View.GONE);
        binding.btnAddParam.setVisibility(signatureExpanded ? View.VISIBLE : View.GONE);
        binding.ivSignatureExpand.setRotation(signatureExpanded ? 0 : 180);
    }

    private void addParamInput() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ItemHookParamInputBinding paramBinding = ItemHookParamInputBinding.inflate(inflater, binding.layoutParams, false);

        int index = paramHolders.size();
        paramBinding.tvParamLabel.setText(getString(R.string.analysis_param_label_format, index, getString(R.string.analyze_invoke_free_mode_param)));
        paramBinding.tilType.setVisibility(View.VISIBLE);
        paramBinding.btnRemoveParam.setVisibility(View.VISIBLE);

        ParamHolder holder = new ParamHolder();
        holder.binding = paramBinding;

        paramBinding.btnRemoveParam.setOnClickListener(v -> {
            binding.layoutParams.removeView(holder.binding.getRoot());
            paramHolders.remove(holder);
            updateParamLabels();
        });

        paramHolders.add(holder);
        binding.layoutParams.addView(paramBinding.getRoot(), binding.layoutParams.getChildCount() - 1);

        binding.tvNoParams.setVisibility(View.GONE);
    }

    private void updateParamLabels() {
        for (int i = 0; i < paramHolders.size(); i++) {
            ParamHolder holder = paramHolders.get(i);
            TextView tvLabel = holder.binding.tvParamLabel;
            tvLabel.setText(getString(R.string.analysis_param_label_format, i, getString(R.string.analyze_invoke_free_mode_param)));
        }

        if (paramHolders.isEmpty()) {
            binding.tvNoParams.setVisibility(View.VISIBLE);
        }
    }

    private void submitHook() {
        String className = Objects.requireNonNull(binding.etClassName.getText()).toString().trim();
        String methodName = Objects.requireNonNull(binding.etMethodName.getText()).toString().trim();

        if (className.isEmpty()) {
            binding.etClassName.setError(getString(R.string.analysis_hook_add_class_name_required));
            return;
        }
        if (methodName.isEmpty()) {
            binding.etMethodName.setError(getString(R.string.analysis_hook_add_method_name_required));
            return;
        }

        boolean hasBefore = binding.cbBefore.isChecked();
        boolean hasAfter = binding.cbAfter.isChecked();
        boolean hasReplace = binding.cbReplace.isChecked();

        if (!hasBefore && !hasAfter && !hasReplace) {
            showToast(R.string.analysis_hook_add_need_one_phase);
            return;
        }

        StringBuilder signatureBuilder = new StringBuilder();
        for (int i = 0; i < paramHolders.size(); i++) {
            String type = paramHolders.get(i).binding.etParamType.getText().toString().trim();
            if (!type.isEmpty()) {
                if (signatureBuilder.length() > 0) signatureBuilder.append(", ");
                signatureBuilder.append(type);
            }
        }
        String signature = signatureBuilder.length() > 0 ? signatureBuilder.toString() : null;

        String beforeCode = hasBefore ? Objects.requireNonNull(binding.etBeforeCode.getText()).toString().trim() : null;
        String afterCode = hasAfter ? Objects.requireNonNull(binding.etAfterCode.getText()).toString().trim() : null;
        String replaceCode = hasReplace ? Objects.requireNonNull(binding.etReplaceCode.getText()).toString().trim() : null;

        viewModel.addHook(className, methodName, signature,
                beforeCode, afterCode, replaceCode,
                null, null, null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static class ParamHolder {
        ItemHookParamInputBinding binding;
    }
}
