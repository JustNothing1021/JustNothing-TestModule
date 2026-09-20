package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.databinding.ActivityClassDetailBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.adapter.analysis.ConstructorAdapter;
import com.justnothing.testmodule.ui.adapter.analysis.FieldAdapter;
import com.justnothing.testmodule.ui.adapter.analysis.MethodAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.ClassQueryViewModel;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ClassDetailActivity extends BaseActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    
    private ActivityClassDetailBinding binding;
    private ClassQueryViewModel viewModel;
    
    private String currentClassName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        currentClassName = getIntent().getStringExtra(EXTRA_CLASS_NAME);
        
        initViews();
        initViewModel();
        
        if (currentClassName != null && !currentClassName.isEmpty()) {
            viewModel.queryClassInfo(currentClassName);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (binding.nestedScroll != null) {
            binding.nestedScroll.post(() -> binding.nestedScroll.scrollTo(0, 0));
        }
    }
    
    private void initViews() {
        binding.rvConstructors.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMethods.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFields.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_class_info);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassQueryViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.cardClassInfo.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        viewModel.getClassInfo().observe(this, this::displayClassInfo);
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
                binding.cardClassInfo.setVisibility(View.GONE);
            }
        });
    }
    
    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        
        binding.tvError.setVisibility(View.GONE);
        binding.cardClassInfo.setVisibility(View.VISIBLE);
        
        binding.tvClassName.setText(info.getName());
        
        String classTypeStr = getClassTypeString(info);
        binding.tvClassType.setText(classTypeStr);
        
        String modifiers = info.getModifiersString();
        if (modifiers != null && !modifiers.isEmpty()) {
            binding.tvModifiers.setText(modifiers);
            binding.tvModifiers.setVisibility(View.VISIBLE);
        } else {
            binding.tvModifiers.setText(getString(R.string.analysis_no_modifiers));
            binding.tvModifiers.setVisibility(View.VISIBLE);
        }
        
        String superClass = info.getSuperClass();
        if (superClass != null && !superClass.isEmpty()) {
            binding.tvSuperClass.setText(superClass);
        } else {
            binding.tvSuperClass.setText(getString(R.string.analysis_no_super_class));
        }
        
        List<String> interfaces = info.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            binding.tvInterfaces.setText(String.join(", ", interfaces));
            binding.tvInterfaces.setVisibility(View.VISIBLE);
        } else {
            binding.tvInterfaces.setText(getString(R.string.none));
            binding.tvInterfaces.setVisibility(View.VISIBLE);
        }
        binding.labelInterfaces.setVisibility(View.VISIBLE);
        
        List<MethodInfo> constructors = info.getConstructors();
        if (constructors != null && !constructors.isEmpty()) {
            ConstructorAdapter constructorAdapter = new ConstructorAdapter(constructors);
            constructorAdapter.setOnItemClickListener((position, constructor) -> openConstructorDetail(constructor));
            binding.rvConstructors.setAdapter(constructorAdapter);
            binding.labelConstructors.setVisibility(View.VISIBLE);
            binding.rvConstructors.setVisibility(View.VISIBLE);
        } else {
            binding.labelConstructors.setVisibility(View.GONE);
            binding.rvConstructors.setVisibility(View.GONE);
        }
        
        List<MethodInfo> methods = info.getMethods();
        if (methods != null && !methods.isEmpty()) {
            LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
            for (MethodInfo method : methods) {
                uniqueNames.add(method.getName());
            }
            
            binding.tvMethodCount.setText(getString(R.string.analysis_methods_count_format, uniqueNames.size(), methods.size()));
            
            MethodAdapter methodAdapter = new MethodAdapter(methods, info.getName());
            methodAdapter.setOnItemClickListener(method -> openMethodDetail(method, info.getName()));
            binding.rvMethods.setAdapter(methodAdapter);
            binding.labelMethods.setVisibility(View.VISIBLE);
            binding.rvMethods.setVisibility(View.VISIBLE);
        } else {
            binding.labelMethods.setVisibility(View.GONE);
            binding.rvMethods.setVisibility(View.GONE);
        }
        
        List<FieldInfo> fields = info.getFields();
        if (fields != null && !fields.isEmpty()) {
            FieldAdapter fieldAdapter = new FieldAdapter(fields, info.getName());
            fieldAdapter.setOnItemClickListener(field -> openFieldDetail(field, info.getName()));
            binding.rvFields.setAdapter(fieldAdapter);
            binding.labelFields.setVisibility(View.VISIBLE);
            binding.rvFields.setVisibility(View.VISIBLE);
        } else {
            binding.labelFields.setVisibility(View.GONE);
            binding.rvFields.setVisibility(View.GONE);
        }
        
        if (binding.nestedScroll != null) {
            binding.nestedScroll.post(() -> binding.nestedScroll.scrollTo(0, 0));
        }
    }
    
    private String getClassTypeString(ClassInfo info) {
        StringBuilder sb = new StringBuilder();
        
        if (info.isAnnotation()) {
            sb.append(getString(R.string.analysis_class_type_annotation));
        } else if (info.isInterface()) {
            sb.append(getString(R.string.analysis_class_type_interface));
        } else if (info.isEnum()) {
            sb.append(getString(R.string.analysis_class_type_enum));
        } else {
            if (info.isAbstract()) {
                sb.append(getString(R.string.analysis_class_type_abstract));
            } else if (info.isFinal()) {
                sb.append(getString(R.string.analysis_class_type_final));
            } else {
                sb.append(getString(R.string.analysis_class_type_class));
            }
        }
        
        return sb.toString();
    }
    
    private void openConstructorDetail(MethodInfo constructor) {
        Intent intent = new Intent(this, ConstructorDetailActivity.class);
        intent.putExtra(ConstructorDetailActivity.EXTRA_CLASS_NAME, currentClassName);
        intent.putExtra(ConstructorDetailActivity.EXTRA_SIGNATURE, buildSignature(constructor));
        intent.putExtra(ConstructorDetailActivity.EXTRA_MODIFIERS, constructor.getModifiersString());
        intent.putStringArrayListExtra(ConstructorDetailActivity.EXTRA_GENERIC_PARAM_TYPES, 
            new ArrayList<>(constructor.getGenericParameterTypes()));
        startActivity(intent);
    }
    
    private void openMethodDetail(MethodInfo method, String className) {
        Intent intent = new Intent(this, MethodDetailActivity.class);
        intent.putExtra(MethodDetailActivity.EXTRA_CLASS_NAME, className);
        intent.putExtra(MethodDetailActivity.EXTRA_METHOD_NAME, method.getName());
        intent.putExtra(MethodDetailActivity.EXTRA_SIGNATURE, buildSignature(method));
        intent.putExtra(MethodDetailActivity.EXTRA_MODIFIERS, method.getModifiersString());
        intent.putExtra(MethodDetailActivity.EXTRA_RETURN_TYPE, method.getGenericReturnType());
        intent.putExtra(MethodDetailActivity.EXTRA_IS_STATIC, Modifier.isStatic(method.getModifiers()));
        intent.putStringArrayListExtra(MethodDetailActivity.EXTRA_GENERIC_PARAM_TYPES, 
            new ArrayList<>(method.getGenericParameterTypes()));
        intent.putExtra(MethodDetailActivity.EXTRA_DECLARING_CLASS, method.getDeclaringClass());
        intent.putExtra(MethodDetailActivity.EXTRA_DECLARING_CLASS_IS_INTERFACE, method.isDeclaringClassIsInterface());
        startActivity(intent);
    }
    
    private void openFieldDetail(FieldInfo field, String className) {
        Intent intent = new Intent(this, FieldDetailActivity.class);
        intent.putExtra(FieldDetailActivity.EXTRA_CLASS_NAME, className);
        intent.putExtra(FieldDetailActivity.EXTRA_FIELD_NAME, field.getName());
        intent.putExtra(FieldDetailActivity.EXTRA_FIELD_TYPE, field.getGenericType());
        intent.putExtra(FieldDetailActivity.EXTRA_MODIFIERS, field.getModifiersString());
        intent.putExtra(FieldDetailActivity.EXTRA_IS_STATIC, Modifier.isStatic(field.getModifiers()));
        intent.putExtra(FieldDetailActivity.EXTRA_DECLARING_CLASS, field.getDeclaringClass());
        intent.putExtra(FieldDetailActivity.EXTRA_DECLARING_CLASS_IS_INTERFACE, field.isDeclaringClassIsInterface());
        startActivity(intent);
    }
    
    private String buildSignature(MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        List<String> paramTypes = method.getGenericParameterTypes();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(paramTypes.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
