package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityConstructorDetailBinding;
import com.justnothing.testmodule.databinding.ItemParamInputBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.ConstructorDetailViewModel;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class ConstructorDetailActivity extends BaseActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_GENERIC_PARAM_TYPES = "genericParamTypes";
    
    private ConstructorDetailViewModel viewModel;
    
    private ActivityConstructorDetailBinding binding;
    
    private String className;
    private String signature;
    private List<String> genericParamTypes;
    private List<ParamInputHolder> paramInputs;
    
    static class ParamInputHolder {
        ItemParamInputBinding binding;
        EditText etType;
        EditText etValue;
        ImageButton btnRemove;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConstructorDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_constructor));
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ConstructorDetailViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnInvoke.setEnabled(!isLoading);
        });
        
        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                binding.cardResult.setVisibility(View.VISIBLE);
                binding.tvResult.setText(result.getResultString() != null ? result.getResultString() : "null");
                binding.tvResult.setTextColor(getColor(R.color.green));
                binding.tvResultType.setText(getString(R.string.analysis_invoke_result_type_label, result.getResultTypeName() != null ? result.getResultTypeName() : "unknown"));
                binding.tvResultHash.setText(getString(R.string.analysis_invoke_result_hash_label, result.getResultHash()));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.cardResult.setVisibility(View.VISIBLE);
                binding.tvResult.setText(error);
                binding.tvResult.setTextColor(getColor(R.color.red));
                binding.tvResultType.setText("");
                binding.tvResultHash.setText("");
            }
        });
    }
    
    private void setupListeners() {
        binding.btnInvoke.setOnClickListener(v -> invokeConstructor());
        
        binding.switchFreeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateParamInputsForMode(isChecked);
        });
        
        binding.btnAddParam.setOnClickListener(v -> addFreeParamInput());
    }
    
    private void displayInfo() {
        binding.tvClassName.setText(className);
        
        String modifiers = getIntent().getStringExtra(EXTRA_MODIFIERS);
        if (modifiers != null && !modifiers.isEmpty()) {
            binding.tvModifiers.setText(modifiers);
        } else {
            binding.tvModifiers.setVisibility(View.GONE);
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
        binding.tvSignature.setText(signatureBuilder.toString());
        
        createParamInputs(false);
    }
    
    private void createParamInputs(boolean freeMode) {
        binding.layoutParams.removeAllViews();
        paramInputs = new ArrayList<>();
        
        if (!freeMode && genericParamTypes.isEmpty()) {
            binding.tvNoParams.setVisibility(View.VISIBLE);
            binding.btnAddParam.setVisibility(View.GONE);
            return;
        }
        
        binding.tvNoParams.setVisibility(View.GONE);
        binding.btnAddParam.setVisibility(freeMode ? View.VISIBLE : View.GONE);
        
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
        ItemParamInputBinding paramBinding = ItemParamInputBinding.inflate(inflater, binding.layoutParams, false);
        
        String paramType = DescriptorColorizer.formatTypeName(genericParamTypes.get(index));
        paramBinding.tvParamLabel.setText(getString(R.string.analysis_param_label_format, index, paramType));
        paramBinding.tilType.setVisibility(View.GONE);
        paramBinding.btnRemoveParam.setVisibility(View.GONE);
        
        ParamInputHolder holder = new ParamInputHolder();
        holder.binding = paramBinding;
        holder.etType = null;
        holder.etValue = paramBinding.etParamValue;
        holder.btnRemove = paramBinding.btnRemoveParam;
        paramInputs.add(holder);
        
        binding.layoutParams.addView(paramBinding.getRoot());
    }
    
    private void addFreeParamInput() {
        LayoutInflater inflater = LayoutInflater.from(this);
        addFreeParamInputInternal(inflater, paramInputs.size());
    }
    
    private void addFreeParamInputInternal(LayoutInflater inflater, int index) {
        ItemParamInputBinding paramBinding = ItemParamInputBinding.inflate(inflater, binding.layoutParams, false);
        
        paramBinding.tvParamLabel.setText(getString(R.string.analysis_param_label_format, index, getString(R.string.analyze_invoke_free_mode_param)));
        paramBinding.tilType.setVisibility(View.VISIBLE);
        paramBinding.btnRemoveParam.setVisibility(View.VISIBLE);
        
        ParamInputHolder holder = new ParamInputHolder();
        holder.binding = paramBinding;
        holder.etType = paramBinding.etParamType;
        holder.etValue = paramBinding.etParamValue;
        holder.btnRemove = paramBinding.btnRemoveParam;
        
        paramBinding.btnRemoveParam.setOnClickListener(v -> {
            binding.layoutParams.removeView(holder.binding.getRoot());
            paramInputs.remove(holder);
            updateParamLabels();
        });
        
        paramInputs.add(holder);
        binding.layoutParams.addView(paramBinding.getRoot());
    }
    
    private void updateParamLabels() {
        for (int i = 0; i < paramInputs.size(); i++) {
            ParamInputHolder holder = paramInputs.get(i);
            holder.binding.tvParamLabel.setText(getString(R.string.analysis_param_label_format, i, getString(R.string.analyze_invoke_free_mode_param)));
        }
    }
    
    private void updateParamInputsForMode(boolean freeMode) {
        createParamInputs(freeMode);
    }
    
    private void invokeConstructor() {
        boolean freeMode = binding.switchFreeMode.isChecked();
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
        
        binding.tvResult.setTextColor(getColor(R.color.green));
        viewModel.invokeConstructor(className, signature, params, paramTypes, freeMode);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
