package com.justnothing.testmodule.ui.analysis.classanalysis;

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
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.util.ArrayList;
import java.util.List;

public class MethodDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    public static final String EXTRA_METHOD_NAME = "methodName";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_MODIFIERS = "modifiers";
    public static final String EXTRA_RETURN_TYPE = "returnType";
    public static final String EXTRA_GENERIC_PARAM_TYPES = "genericParamTypes";
    public static final String EXTRA_IS_STATIC = "isStatic";
    public static final String EXTRA_DECLARING_CLASS = "declaringClass";
    public static final String EXTRA_DECLARING_CLASS_IS_INTERFACE = "declaringClassIsInterface";
    
    private MethodDetailViewModel viewModel;
    
    private TextView tvClassName;
    private TextView tvMethodName;
    private TextView tvModifiers;
    private TextView tvReturnType;
    private TextView tvSignature;
    private TextView tvDeclaringClass;
    private View cardInstance;
    private EditText etTargetInstance;
    private ViewGroup layoutParams;
    private TextView tvNoParams;
    private SwitchMaterial switchFreeMode;
    private ImageButton btnAddParam;
    private View cardResult;
    private TextView tvResult;
    private TextView tvResultType;
    private TextView tvResultHash;
    private ProgressBar progressBar;
    private TextView tvInstanceAfterLabel;
    private TextView tvInstanceAfter;
    private TextView tvInstanceHash;
    
    private String className;
    private String methodName;
    private String signature;
    private String returnType;
    private List<String> genericParamTypes;
    private boolean isStatic;
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
        setContentView(R.layout.activity_method_detail);
        
        className = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        methodName = getIntent().getStringExtra(EXTRA_METHOD_NAME);
        signature = getIntent().getStringExtra(EXTRA_SIGNATURE);
        returnType = getIntent().getStringExtra(EXTRA_RETURN_TYPE);
        isStatic = getIntent().getBooleanExtra(EXTRA_IS_STATIC, false);
        
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
        tvMethodName = findViewById(R.id.tv_method_name);
        tvModifiers = findViewById(R.id.tv_modifiers);
        tvReturnType = findViewById(R.id.tv_return_type);
        tvSignature = findViewById(R.id.tv_signature);
        tvDeclaringClass = findViewById(R.id.tv_declaring_class);
        cardInstance = findViewById(R.id.card_instance);
        etTargetInstance = findViewById(R.id.et_target_instance);
        layoutParams = findViewById(R.id.layout_params);
        tvNoParams = findViewById(R.id.tv_no_params);
        switchFreeMode = findViewById(R.id.switch_free_mode);
        btnAddParam = findViewById(R.id.btn_add_param);
        cardResult = findViewById(R.id.card_result);
        tvResult = findViewById(R.id.tv_result);
        tvResultType = findViewById(R.id.tv_result_type);
        tvResultHash = findViewById(R.id.tv_result_hash);
        progressBar = findViewById(R.id.progress_bar);
        tvInstanceAfterLabel = findViewById(R.id.tv_instance_after_label);
        tvInstanceAfter = findViewById(R.id.tv_instance_after);
        tvInstanceHash = findViewById(R.id.tv_instance_hash);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.method_invoke);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MethodDetailViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            findViewById(R.id.btn_invoke).setEnabled(!isLoading);
        });
        
        viewModel.getResult().observe(this, result -> {
            if (result != null) {
                cardResult.setVisibility(View.VISIBLE);
                tvResult.setText(result.getResultString() != null ? result.getResultString() : "null");
                tvResult.setTextColor(getColor(R.color.green));
                tvResultType.setText(getString(R.string.result_type_label) + ": " + 
                    (result.getResultTypeName() != null ? result.getResultTypeName() : "unknown"));
                tvResultHash.setText(getString(R.string.result_hash_label) + ": " + result.getResultHash());
                
                String instanceAfter = result.getInstanceAfterInvocation();
                if (instanceAfter != null && !instanceAfter.isEmpty()) {
                    tvInstanceAfterLabel.setVisibility(View.VISIBLE);
                    tvInstanceAfter.setVisibility(View.VISIBLE);
                    tvInstanceAfter.setText(instanceAfter);
                    tvInstanceAfter.setTextColor(getColor(R.color.blue));
                    tvInstanceHash.setVisibility(View.VISIBLE);
                    tvInstanceHash.setText(getString(R.string.result_hash_label) + ": " + result.getInstanceHash());
                } else {
                    tvInstanceAfterLabel.setVisibility(View.GONE);
                    tvInstanceAfter.setVisibility(View.GONE);
                    tvInstanceHash.setVisibility(View.GONE);
                }
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                cardResult.setVisibility(View.VISIBLE);
                tvResult.setText(error);
                tvResult.setTextColor(getColor(R.color.red));
                tvResultType.setText("");
                tvResultHash.setText("");
                tvInstanceAfterLabel.setVisibility(View.GONE);
                tvInstanceAfter.setVisibility(View.GONE);
                tvInstanceHash.setVisibility(View.GONE);
            }
        });
    }
    
    private void setupListeners() {
        findViewById(R.id.btn_invoke).setOnClickListener(v -> invokeMethod());
        
        switchFreeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateParamInputsForMode(isChecked);
        });
        
        btnAddParam.setOnClickListener(v -> addFreeParamInput());
    }
    
    private void displayInfo() {
        tvClassName.setText(className);
        tvMethodName.setText(methodName);
        
        String modifiers = getIntent().getStringExtra(EXTRA_MODIFIERS);
        if (modifiers != null && !modifiers.isEmpty()) {
            tvModifiers.setText(modifiers);
        } else {
            tvModifiers.setVisibility(View.GONE);
        }
        
        tvReturnType.setText(getString(R.string.return_type_label) + ": " + 
            DescriptorColorizer.formatTypeName(returnType));
        
        StringBuilder signatureBuilder = new StringBuilder();
        if (genericParamTypes.isEmpty()) {
            signatureBuilder.append(getString(R.string.no_params));
        } else {
            for (int i = 0; i < genericParamTypes.size(); i++) {
                if (i > 0) signatureBuilder.append(", ");
                signatureBuilder.append(DescriptorColorizer.formatTypeName(genericParamTypes.get(i)));
            }
        }
        tvSignature.setText(signatureBuilder.toString());
        
        String declaringClass = getIntent().getStringExtra(EXTRA_DECLARING_CLASS);
        boolean declaringClassIsInterface = getIntent().getBooleanExtra(EXTRA_DECLARING_CLASS_IS_INTERFACE, false);
        if (declaringClass != null && !declaringClass.equals(className)) {
            tvDeclaringClass.setVisibility(View.VISIBLE);
            if (declaringClassIsInterface) {
                tvDeclaringClass.setText(getString(R.string.implements_interface, DescriptorColorizer.formatTypeName(declaringClass)));
            } else {
                tvDeclaringClass.setText(getString(R.string.inherited_from, DescriptorColorizer.formatTypeName(declaringClass)));
            }
        } else {
            tvDeclaringClass.setVisibility(View.GONE);
        }
        
        if (!isStatic) {
            cardInstance.setVisibility(View.VISIBLE);
        } else {
            cardInstance.setVisibility(View.GONE);
        }
        
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
        tvLabel.setText(getString(R.string.param_label_format, index, paramType));
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
        
        tvLabel.setText(getString(R.string.param_label_format, index, getString(R.string.free_mode_param)));
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
            tvLabel.setText(getString(R.string.param_label_format, i, getString(R.string.free_mode_param)));
        }
    }
    
    private void updateParamInputsForMode(boolean freeMode) {
        createParamInputs(freeMode);
    }
    
    private void invokeMethod() {
        boolean freeMode = switchFreeMode.isChecked();
        String targetInstance = null;
        
        if (!isStatic) {
            targetInstance = etTargetInstance.getText() != null ? 
                etTargetInstance.getText().toString().trim() : "";
            if (targetInstance.isEmpty()) {
                cardResult.setVisibility(View.VISIBLE);
                tvResult.setText(getString(R.string.instance_method_requires_target));
                tvResult.setTextColor(getColor(R.color.red));
                return;
            }
        }
        
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
        viewModel.invokeMethod(className, methodName, signature, targetInstance, 
                              params, paramTypes, freeMode, isStatic);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
