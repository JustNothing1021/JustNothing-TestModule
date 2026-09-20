package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityFieldDetailBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.viewmodel.analysis.FieldDetailViewModel;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

public class FieldDetailActivity extends BaseActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    public static final String EXTRA_FIELD_NAME = "fieldName";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_FIELD_TYPE = "fieldType";
    public static final String EXTRA_IS_STATIC = "isStatic";
    public static final String EXTRA_DECLARING_CLASS = "declaringClass";
    public static final String EXTRA_DECLARING_CLASS_IS_INTERFACE = "declaringClassIsInterface";
    
    private FieldDetailViewModel viewModel;
    
    private ActivityFieldDetailBinding binding;
    
    private String className;
    private String fieldName;
    private String fieldType;
    private boolean isStatic;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFieldDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        className = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        fieldName = getIntent().getStringExtra(EXTRA_FIELD_NAME);
        fieldType = getIntent().getStringExtra(EXTRA_FIELD_TYPE);
        isStatic = getIntent().getBooleanExtra(EXTRA_IS_STATIC, false);
        
        initViews();
        initViewModel();
        setupListeners();
        displayInfo();
    }
    
    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_field_value);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FieldDetailViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnGetValue.setEnabled(!isLoading);
            binding.btnSetValue.setEnabled(!isLoading);
        });
        
        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                binding.cardResult.setVisibility(View.VISIBLE);
                binding.tvValue.setText(result.getValueString() != null ? result.getValueString() : "null");
                binding.tvValue.setTextColor(getColor(R.color.green));
                binding.tvValueType.setText(getString(R.string.analysis_field_value_type_label,
                        result.getValueTypeName() != null ? result.getValueTypeName() : "unknown"));
                binding.tvValueHash.setText(getString(R.string.analysis_field_value_hash_label, result.getValueHash()));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.cardResult.setVisibility(View.VISIBLE);
                binding.tvValue.setText(error);
                binding.tvValue.setTextColor(getColor(R.color.red));
                binding.tvValueType.setText("");
                binding.tvValueHash.setText("");
            }
        });
        
        viewModel.getSetSuccess().observe(this, success -> {
            if (success != null && success) {
                showToast(R.string.analysis_field_set_success);
            }
        });
        
        viewModel.getSetError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showToast(getString(R.string.analysis_set_field_failed, error), Toast.LENGTH_LONG);
            }
        });
    }
    
    private void setupListeners() {
        binding.btnGetValue.setOnClickListener(v -> getFieldValue());
        binding.btnSetValue.setOnClickListener(v -> setFieldValue());
    }
    
    private void displayInfo() {
        binding.tvClassName.setText(className);
        binding.tvFieldName.setText(fieldName);
        
        String modifiers = getIntent().getStringExtra(EXTRA_MODIFIERS);
        if (modifiers != null && !modifiers.isEmpty()) {
            binding.tvModifiers.setText(modifiers);
        } else {
            binding.tvModifiers.setVisibility(View.GONE);
        }
        
        binding.tvFieldType.setText(getString(R.string.analysis_field_type_label, DescriptorColorizer.formatTypeName(fieldType)));
        
        String declaringClass = getIntent().getStringExtra(EXTRA_DECLARING_CLASS);
        boolean declaringClassIsInterface = getIntent().getBooleanExtra(EXTRA_DECLARING_CLASS_IS_INTERFACE, false);
        if (declaringClass != null && !declaringClass.equals(className)) {
            binding.tvDeclaringClass.setVisibility(View.VISIBLE);
            if (declaringClassIsInterface) {
                binding.tvDeclaringClass.setText(getString(R.string.analysis_implements, DescriptorColorizer.formatTypeName(declaringClass)));
            } else {
                binding.tvDeclaringClass.setText(getString(R.string.analysis_extends, DescriptorColorizer.formatTypeName(declaringClass)));
            }
        } else {
            binding.tvDeclaringClass.setVisibility(View.GONE);
        }
        
        if (!isStatic) {
            binding.cardInstance.setVisibility(View.VISIBLE);
        } else {
            binding.cardInstance.setVisibility(View.GONE);
        }
    }
    
    private void getFieldValue() {
        String targetInstance = null;
        
        if (!isStatic) {
            targetInstance = binding.etTargetInstance.getText() != null ? 
                binding.etTargetInstance.getText().toString().trim() : "";
            if (targetInstance.isEmpty()) {
                binding.cardResult.setVisibility(View.VISIBLE);
                binding.tvValue.setText(R.string.analysis_instance_field_requires_target);
                binding.tvValue.setTextColor(getColor(R.color.red));
                return;
            }
        }
        
        binding.tvValue.setTextColor(getColor(R.color.green));
        viewModel.getFieldValue(className, fieldName, targetInstance, isStatic);
    }
    
    private void setFieldValue() {
        String targetInstance = null;
        
        if (!isStatic) {
            targetInstance = binding.etTargetInstance.getText() != null ? 
                binding.etTargetInstance.getText().toString().trim() : "";
            if (targetInstance.isEmpty()) {
                showToast(R.string.analysis_instance_field_requires_target);
                return;
            }
        }
        
        String valueExpression = binding.etValueExpression.getText() != null ? 
            binding.etValueExpression.getText().toString().trim() : "";
        if (valueExpression.isEmpty()) {
            showToast(R.string.analysis_please_enter_value_expression);
            return;
        }
        
        viewModel.setFieldValue(className, fieldName, targetInstance, valueExpression, isStatic);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
