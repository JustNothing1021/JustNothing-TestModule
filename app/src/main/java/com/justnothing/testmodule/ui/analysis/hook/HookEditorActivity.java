package com.justnothing.testmodule.ui.analysis.hook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.justnothing.testmodule.R;

import java.util.ArrayList;
import java.util.List;

public class HookEditorActivity extends AppCompatActivity {

    private TextInputEditText etClassName, etMethodName;
    private TextInputEditText etBeforeCode, etAfterCode, etReplaceCode;
    private TextInputLayout layoutBeforeCode, layoutAfterCode, layoutReplaceCode;
    private MaterialCheckBox cbBefore, cbAfter, cbReplace;

    private LinearLayout layoutParams;
    private TextView tvNoParams;
    private View btnAddParam;
    private ImageView ivSignatureExpand;
    private boolean signatureExpanded = false;

    private List<ParamHolder> paramHolders = new ArrayList<>();

    private HookAnalysisViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_editor);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        viewModel = new ViewModelProvider(this).get(HookAnalysisViewModel.class);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                if (result.isSuccessAction()) {
                    Toast.makeText(this, R.string.analysis_hook_add_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = result.getMessage() != null ? result.getMessage() : "添加失败";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initViews() {
        etClassName = findViewById(R.id.et_class_name);
        etMethodName = findViewById(R.id.et_method_name);

        cbBefore = findViewById(R.id.cb_before);
        cbAfter = findViewById(R.id.cb_after);
        cbReplace = findViewById(R.id.cb_replace);

        layoutBeforeCode = findViewById(R.id.layout_before_code);
        layoutAfterCode = findViewById(R.id.layout_after_code);
        layoutReplaceCode = findViewById(R.id.layout_replace_code);

        etBeforeCode = findViewById(R.id.et_before_code);
        etAfterCode = findViewById(R.id.et_after_code);
        etReplaceCode = findViewById(R.id.et_replace_code);

        layoutParams = findViewById(R.id.layout_params);
        tvNoParams = findViewById(R.id.tv_no_params);
        btnAddParam = findViewById(R.id.btn_add_param);
        ivSignatureExpand = findViewById(R.id.iv_signature_expand);

        findViewById(R.id.layout_signature_header).setOnClickListener(v -> toggleSignaturePanel());

        btnAddParam.setOnClickListener(v -> addParamInput());

        cbBefore.setOnCheckedChangeListener((buttonView, isChecked) ->
                layoutBeforeCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbAfter.setOnCheckedChangeListener((buttonView, isChecked) ->
                layoutAfterCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbReplace.setOnCheckedChangeListener((buttonView, isChecked) ->
                layoutReplaceCode.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        findViewById(R.id.btn_submit).setOnClickListener(v -> submitHook());
    }

    private void toggleSignaturePanel() {
        signatureExpanded = !signatureExpanded;
        layoutParams.setVisibility(signatureExpanded ? View.VISIBLE : View.GONE);
        btnAddParam.setVisibility(signatureExpanded ? View.VISIBLE : View.GONE);
        ivSignatureExpand.setRotation(signatureExpanded ? 0 : 180);
    }

    private void addParamInput() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View paramView = inflater.inflate(R.layout.item_hook_param_input, layoutParams, false);

        TextView tvLabel = paramView.findViewById(R.id.tv_param_label);
        TextInputLayout tilType = paramView.findViewById(R.id.til_type);
        EditText etType = paramView.findViewById(R.id.et_param_type);
        ImageButton btnRemove = paramView.findViewById(R.id.btn_remove_param);

        int index = paramHolders.size();
        tvLabel.setText(getString(R.string.analysis_param_label_format, index, getString(R.string.analyze_invoke_free_mode_param)));
        tilType.setVisibility(View.VISIBLE);
        btnRemove.setVisibility(View.VISIBLE);

        ParamHolder holder = new ParamHolder();
        holder.rootView = paramView;
        holder.etType = etType;
        holder.btnRemove = btnRemove;

        btnRemove.setOnClickListener(v -> {
            layoutParams.removeView(holder.rootView);
            paramHolders.remove(holder);
            updateParamLabels();
        });

        paramHolders.add(holder);
        layoutParams.addView(paramView, layoutParams.getChildCount() - 1);

        tvNoParams.setVisibility(View.GONE);
    }

    private void updateParamLabels() {
        for (int i = 0; i < paramHolders.size(); i++) {
            ParamHolder holder = paramHolders.get(i);
            TextView tvLabel = holder.rootView.findViewById(R.id.tv_param_label);
            tvLabel.setText(getString(R.string.analysis_param_label_format, i, getString(R.string.analyze_invoke_free_mode_param)));
        }

        if (paramHolders.isEmpty()) {
            tvNoParams.setVisibility(View.VISIBLE);
        }
    }

    private void submitHook() {
        String className = etClassName.getText().toString().trim();
        String methodName = etMethodName.getText().toString().trim();

        if (className.isEmpty()) {
            etClassName.setError("请输入类名");
            return;
        }
        if (methodName.isEmpty()) {
            etMethodName.setError("请输入方法名");
            return;
        }

        boolean hasBefore = cbBefore.isChecked();
        boolean hasAfter = cbAfter.isChecked();
        boolean hasReplace = cbReplace.isChecked();

        if (!hasBefore && !hasAfter && !hasReplace) {
            Toast.makeText(this, R.string.analysis_hook_add_need_one_phase, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder signatureBuilder = new StringBuilder();
        for (int i = 0; i < paramHolders.size(); i++) {
            String type = paramHolders.get(i).etType.getText().toString().trim();
            if (!type.isEmpty()) {
                if (signatureBuilder.length() > 0) signatureBuilder.append(", ");
                signatureBuilder.append(type);
            }
        }
        String signature = signatureBuilder.length() > 0 ? signatureBuilder.toString() : null;

        String beforeCode = hasBefore ? etBeforeCode.getText().toString().trim() : null;
        String afterCode = hasAfter ? etAfterCode.getText().toString().trim() : null;
        String replaceCode = hasReplace ? etReplaceCode.getText().toString().trim() : null;

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
        View rootView;
        EditText etType;
        ImageButton btnRemove;
    }
}
