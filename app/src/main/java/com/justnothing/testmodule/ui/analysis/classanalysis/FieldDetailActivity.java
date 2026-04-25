package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

public class FieldDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    public static final String EXTRA_FIELD_NAME = "fieldName";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_FIELD_TYPE = "fieldType";
    public static final String EXTRA_IS_STATIC = "isStatic";
    public static final String EXTRA_DECLARING_CLASS = "declaringClass";
    public static final String EXTRA_DECLARING_CLASS_IS_INTERFACE = "declaringClassIsInterface";
    
    private FieldDetailViewModel viewModel;
    
    private TextView tvClassName;
    private TextView tvFieldName;
    private TextView tvModifiers;
    private TextView tvFieldType;
    private TextView tvDeclaringClass;
    private View cardInstance;
    private EditText etTargetInstance;
    private View cardResult;
    private TextView tvValue;
    private TextView tvValueType;
    private TextView tvValueHash;
    private ProgressBar progressBar;
    private EditText etValueExpression;
    private EditText etValueTypeHint;
    
    private String className;
    private String fieldName;
    private String fieldType;
    private boolean isStatic;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_detail);
        
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
        tvClassName = findViewById(R.id.tv_class_name);
        tvFieldName = findViewById(R.id.tv_field_name);
        tvModifiers = findViewById(R.id.tv_modifiers);
        tvFieldType = findViewById(R.id.tv_field_type);
        tvDeclaringClass = findViewById(R.id.tv_declaring_class);
        cardInstance = findViewById(R.id.card_instance);
        etTargetInstance = findViewById(R.id.et_target_instance);
        cardResult = findViewById(R.id.card_result);
        tvValue = findViewById(R.id.tv_value);
        tvValueType = findViewById(R.id.tv_value_type);
        tvValueHash = findViewById(R.id.tv_value_hash);
        progressBar = findViewById(R.id.progress_bar);
        etValueExpression = findViewById(R.id.et_value_expression);
        etValueTypeHint = findViewById(R.id.et_value_type_hint);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_field_value);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(FieldDetailViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            findViewById(R.id.btn_get_value).setEnabled(!isLoading);
            findViewById(R.id.btn_set_value).setEnabled(!isLoading);
        });
        
        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                cardResult.setVisibility(View.VISIBLE);
                tvValue.setText(result.getValueString() != null ? result.getValueString() : "null");
                tvValue.setTextColor(getColor(R.color.green));
                tvValueType.setText(getString(R.string.analysis_field_value_type_label,
                        result.getValueTypeName() != null ? result.getValueTypeName() : "unknown"));
                tvValueHash.setText(getString(R.string.analysis_field_value_hash_label, result.getValueHash()));
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                cardResult.setVisibility(View.VISIBLE);
                tvValue.setText(error);
                tvValue.setTextColor(getColor(R.color.red));
                tvValueType.setText("");
                tvValueHash.setText("");
            }
        });
        
        viewModel.getSetSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, R.string.analysis_field_set_success, Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getSetError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, getString(R.string.analysis_set_field_failed, error), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void setupListeners() {
        findViewById(R.id.btn_get_value).setOnClickListener(v -> getFieldValue());
        findViewById(R.id.btn_set_value).setOnClickListener(v -> setFieldValue());
    }
    
    private void displayInfo() {
        tvClassName.setText(className);
        tvFieldName.setText(fieldName);
        
        String modifiers = getIntent().getStringExtra(EXTRA_MODIFIERS);
        if (modifiers != null && !modifiers.isEmpty()) {
            tvModifiers.setText(modifiers);
        } else {
            tvModifiers.setVisibility(View.GONE);
        }
        
        tvFieldType.setText(getString(R.string.analysis_field_type_label, DescriptorColorizer.formatTypeName(fieldType)));
        
        String declaringClass = getIntent().getStringExtra(EXTRA_DECLARING_CLASS);
        boolean declaringClassIsInterface = getIntent().getBooleanExtra(EXTRA_DECLARING_CLASS_IS_INTERFACE, false);
        if (declaringClass != null && !declaringClass.equals(className)) {
            tvDeclaringClass.setVisibility(View.VISIBLE);
            if (declaringClassIsInterface) {
                tvDeclaringClass.setText(getString(R.string.analysis_implements, DescriptorColorizer.formatTypeName(declaringClass)));
            } else {
                tvDeclaringClass.setText(getString(R.string.analysis_extends, DescriptorColorizer.formatTypeName(declaringClass)));
            }
        } else {
            tvDeclaringClass.setVisibility(View.GONE);
        }
        
        if (!isStatic) {
            cardInstance.setVisibility(View.VISIBLE);
        } else {
            cardInstance.setVisibility(View.GONE);
        }
    }
    
    private void getFieldValue() {
        String targetInstance = null;
        
        if (!isStatic) {
            targetInstance = etTargetInstance.getText() != null ? 
                etTargetInstance.getText().toString().trim() : "";
            if (targetInstance.isEmpty()) {
                cardResult.setVisibility(View.VISIBLE);
                tvValue.setText(R.string.analysis_instance_field_requires_target);
                tvValue.setTextColor(getColor(R.color.red));
                return;
            }
        }
        
        tvValue.setTextColor(getColor(R.color.green));
        viewModel.getFieldValue(className, fieldName, targetInstance, isStatic);
    }
    
    private void setFieldValue() {
        String targetInstance = null;
        
        if (!isStatic) {
            targetInstance = etTargetInstance.getText() != null ? 
                etTargetInstance.getText().toString().trim() : "";
            if (targetInstance.isEmpty()) {
                Toast.makeText(this, R.string.analysis_instance_field_requires_target, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        String valueExpression = etValueExpression.getText() != null ? 
            etValueExpression.getText().toString().trim() : "";
        if (valueExpression.isEmpty()) {
            Toast.makeText(this, R.string.analysis_please_enter_value_expression, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String valueTypeHint = etValueTypeHint.getText() != null ? 
            etValueTypeHint.getText().toString().trim() : null;
        
        viewModel.setFieldValue(className, fieldName, targetInstance, valueExpression, 
                               valueTypeHint, isStatic);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
