package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.ui.viewmodel.analysis.ConstructorDetailViewModel;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class ConstructorDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_GENERIC_PARAM_TYPES = "genericParamTypes";
    
    private ConstructorDetailViewModel viewModel;
    
    private TextView tvClassName;
    private TextView tvModifiers;
    private TextView tvSignature;
    private ViewGroup layoutParams;
    private TextView tvNoParams;
    private SwitchMaterial switchFreeMode;
    private ImageButton btnAddParam;
    private View cardResult;
    private TextView tvResult;
    private TextView tvResultType;
    private TextView tvResultHash;
    private ProgressBar progressBar;
    
    private String className;
    private String signature;
    private List<String> genericParamTypes;
    private List<ParamInputHolder> paramInputs;
    
    static class ParamInputHolder {
        View rootView;
        EditText etType;
        EditText etValue;
        ImageButton btnRemove;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constructor_detail);
        
        className = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        signature = getIntent().getStringExtra(EXTRA_SIGNATURE);
        
        genericParamTypes = new ArrayList<>();
        ArrayList<String> paramTypesList = getIntent().getStringArrayListExtra(EXTRA_GENERIC_PARAM_TYPES);
        if (paramTypesList != null) {
            genericParamTypes.addAll(paramTypesList);
        }
        
        initViews();
        initViewModel();
        setupListeners();
        displayInfo();
    }
    
    private void initViews() {
        tvClassName = findViewById(R.id.tv_class_name);
        tvModifiers = findViewById(R.id.tv_modifiers);
        tvSignature = findViewById(R.id.tv_signature);
        layoutParams = findViewById(R.id.layout_params);
        tvNoParams = findViewById(R.id.tv_no_params);
        switchFreeMode = findViewById(R.id.switch_free_mode);
        btnAddParam = findViewById(R.id.btn_add_param);
        cardResult = findViewById(R.id.card_result);
        tvResult = findViewById(R.id.tv_result);
        tvResultType = findViewById(R.id.tv_result_type);
        tvResultHash = findViewById(R.id.tv_result_hash);
        progressBar = findViewById(R.id.progress_bar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_constructor));
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ConstructorDetailViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            findViewById(R.id.btn_invoke).setEnabled(!isLoading);
        });
        
        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                cardResult.setVisibility(View.VISIBLE);
                tvResult.setText(result.getResultString() != null ? result.getResultString() : "null");
                tvResult.setTextColor(getColor(R.color.green));
                tvResultType.setText(getString(R.string.analysis_invoke_result_type_label, result.getResultTypeName() != null ? result.getResultTypeName() : "unknown"));
                tvResultHash.setText(getString(R.string.analysis_invoke_result_hash_label, result.getResultHash()));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                cardResult.setVisibility(View.VISIBLE);
                tvResult.setText(error);
                tvResult.setTextColor(getColor(R.color.red));
                tvResultType.setText("");
                tvResultHash.setText("");
            }
        });
    }
    
    private void setupListeners() {
        findViewById(R.id.btn_invoke).setOnClickListener(v -> invokeConstructor());
        
        switchFreeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateParamInputsForMode(isChecked);
        });
        
        btnAddParam.setOnClickListener(v -> addFreeParamInput());
    }
    
    private void displayInfo() {
        tvClassName.setText(className);
        
        String modifiers = getIntent().getStringExtra(EXTRA_MODIFIERS);
        if (modifiers != null && !modifiers.isEmpty()) {
            tvModifiers.setText(modifiers);
        } else {
            tvModifiers.setVisibility(View.GONE);
        }
        
        StringBuilder signatureBuilder = new StringBuilder();
        if (genericParamTypes.isEmpty()) {
            signatureBuilder.append(getString(R.string.analysis_no_params));
        } else {
            for (int i = 0; i < genericParamTypes.size(); i++) {
                if (i > 0) signatureBuilder.append(", ");
                signatureBuilder.append(DescriptorColorizer.formatTypeName(genericParamTypes.get(i)));
            }
        }
        tvSignature.setText(signatureBuilder.toString());
        
        createParamInputs(false);
    }
    
    private void createParamInputs(boolean freeMode) {
        layoutParams.removeAllViews();
        paramInputs = new ArrayList<>();
        
        if (!freeMode && genericParamTypes.isEmpty()) {
            tvNoParams.setVisibility(View.VISIBLE);
            btnAddParam.setVisibility(View.GONE);
            return;
        }
        
        tvNoParams.setVisibility(View.GONE);
        btnAddParam.setVisibility(freeMode ? View.VISIBLE : View.GONE);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        int paramCount = freeMode ? 1 : genericParamTypes.size();
        
        for (int i = 0; i < paramCount; i++) {
            if (freeMode) {
                addFreeParamInputInternal(inflater, i);
            } else {
                addFixedParamInput(inflater, i);
            }
        }
    }
    
    private void addFixedParamInput(LayoutInflater inflater, int index) {
        View paramView = inflater.inflate(R.layout.item_param_input, layoutParams, false);
        
        TextView tvLabel = paramView.findViewById(R.id.tv_param_label);
        TextInputLayout tilType = paramView.findViewById(R.id.til_type);
        EditText etValue = paramView.findViewById(R.id.et_param_value);
        ImageButton btnRemove = paramView.findViewById(R.id.btn_remove_param);
        
        String paramType = DescriptorColorizer.formatTypeName(genericParamTypes.get(index));
        tvLabel.setText(getString(R.string.analysis_param_label_format, index, paramType));
        tilType.setVisibility(View.GONE);
        btnRemove.setVisibility(View.GONE);
        
        ParamInputHolder holder = new ParamInputHolder();
        holder.rootView = paramView;
        holder.etType = null;
        holder.etValue = etValue;
        holder.btnRemove = btnRemove;
        paramInputs.add(holder);
        
        layoutParams.addView(paramView);
    }
    
    private void addFreeParamInput() {
        LayoutInflater inflater = LayoutInflater.from(this);
        addFreeParamInputInternal(inflater, paramInputs.size());
    }
    
    private void addFreeParamInputInternal(LayoutInflater inflater, int index) {
        View paramView = inflater.inflate(R.layout.item_param_input, layoutParams, false);
        
        TextView tvLabel = paramView.findViewById(R.id.tv_param_label);
        TextInputLayout tilType = paramView.findViewById(R.id.til_type);
        EditText etType = paramView.findViewById(R.id.et_param_type);
        EditText etValue = paramView.findViewById(R.id.et_param_value);
        ImageButton btnRemove = paramView.findViewById(R.id.btn_remove_param);
        
        tvLabel.setText(getString(R.string.analysis_param_label_format, index, getString(R.string.analyze_invoke_free_mode_param)));
        tilType.setVisibility(View.VISIBLE);
        btnRemove.setVisibility(View.VISIBLE);
        
        ParamInputHolder holder = new ParamInputHolder();
        holder.rootView = paramView;
        holder.etType = etType;
        holder.etValue = etValue;
        holder.btnRemove = btnRemove;
        
        btnRemove.setOnClickListener(v -> {
            layoutParams.removeView(holder.rootView);
            paramInputs.remove(holder);
            updateParamLabels();
        });
        
        paramInputs.add(holder);
        layoutParams.addView(paramView);
    }
    
    private void updateParamLabels() {
        for (int i = 0; i < paramInputs.size(); i++) {
            ParamInputHolder holder = paramInputs.get(i);
            TextView tvLabel = holder.rootView.findViewById(R.id.tv_param_label);
            tvLabel.setText(getString(R.string.analysis_param_label_format, i, getString(R.string.analyze_invoke_free_mode_param)));
        }
    }
    
    private void updateParamInputsForMode(boolean freeMode) {
        createParamInputs(freeMode);
    }
    
    private void invokeConstructor() {
        boolean freeMode = switchFreeMode.isChecked();
        List<String> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();
        
        for (ParamInputHolder holder : paramInputs) {
            String value = holder.etValue.getText() != null ? holder.etValue.getText().toString().trim() : "";
            params.add(value);
            
            if (freeMode && holder.etType != null) {
                String type = holder.etType.getText() != null ? holder.etType.getText().toString().trim() : "";
                paramTypes.add(type.isEmpty() ? null : type);
            }
        }
        
        tvResult.setTextColor(getColor(R.color.green));
        viewModel.invokeConstructor(className, signature, params, paramTypes, freeMode);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
