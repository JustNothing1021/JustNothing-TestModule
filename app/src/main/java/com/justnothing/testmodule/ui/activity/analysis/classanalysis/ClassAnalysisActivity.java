package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.databinding.ActivityClassAnalysisBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.adapter.analysis.ConstructorAdapter;
import com.justnothing.testmodule.ui.adapter.analysis.FieldAdapter;
import com.justnothing.testmodule.ui.adapter.analysis.MethodAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.ClassQueryViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类分析Activity。
 * 
 * <p>展示类的详细信息，包括：
 * <ul>
 *   <li>类名、访问修饰符、父类、接口（点击可查看继承图）</li>
 *   <li>可展开的构造函数列表</li>
 *   <li>可展开的方法列表（按名称分组，重载放在一起，显示继承自提示）</li>
 *   <li>可展开的字段列表（显示继承自提示）</li>
 * </ul>
 * </p>
 */
public class ClassAnalysisActivity extends BaseActivity {
    
    private ActivityClassAnalysisBinding binding;
    private ClassQueryViewModel viewModel;
    private ClassInfo currentClassInfo;
    
    private boolean constructorsExpanded = false;
    private boolean methodsExpanded = false;
    private boolean fieldsExpanded = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initViews();
        initViewModel();
        setupListeners();
    }
    
    private void initViews() {
        binding.rvConstructors.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMethods.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFields.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.class_analysis));
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassQueryViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutResult.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            }
            binding.btnQuery.setEnabled(!isLoading);
        });
        
        viewModel.getClassInfo().observe(this, this::displayClassInfo);
        
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
        binding.btnQuery.setOnClickListener(v -> {
            String className = binding.etClassName.getText() != null ? binding.etClassName.getText().toString().trim() : "";
            if (!className.isEmpty()) {
                viewModel.queryClassInfo(className);
            }
        });
        
        binding.etClassName.setOnEditorActionListener((v, actionId, event) -> {
            binding.btnQuery.performClick();
            return true;
        });
        
        binding.cardClassInfo.setOnClickListener(v -> {
            if (currentClassInfo != null) {
                openClassGraph();
            }
        });
        
        binding.headerConstructors.setOnClickListener(v -> toggleConstructors());
        binding.headerMethods.setOnClickListener(v -> toggleMethods());
        binding.headerFields.setOnClickListener(v -> toggleFields());
    }
    
    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        
        currentClassInfo = info;
        
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);
        
        binding.tvClassName.setText(info.getName());
        binding.tvModifiers.setText(info.getModifiersString());
        binding.tvSuperClass.setText(info.getSuperClass() != null ? info.getSuperClass() : getString(R.string.none));
        binding.tvInterfaces.setText(info.getInterfaces().isEmpty() ? getString(R.string.none) : TextUtils.join(", ", info.getInterfaces()));
        
        List<MethodInfo> constructors = info.getConstructors();
        binding.tvConstructorsTitle.setText(
            getString(R.string.analysis_constructors_format, constructors.size()));
        
        List<MethodInfo> methods = sortMethodsByName(info.getMethods());
        Set<String> uniqueNames = new HashSet<>();
        for (MethodInfo m : methods) {
            uniqueNames.add(m.getName());
        }
        binding.tvMethodsTitle.setText(
            getString(R.string.analysis_methods_and_count_format, uniqueNames.size(), methods.size()));
        
        List<FieldInfo> fields = info.getFields();
        binding.tvFieldsTitle.setText(
            getString(R.string.analysis_fields_format, fields.size()));
        
        ConstructorAdapter constructorAdapter = new ConstructorAdapter(constructors);
        constructorAdapter.setOnItemClickListener(this::openConstructorDetail);
        binding.rvConstructors.setAdapter(constructorAdapter);
        
        MethodAdapter methodAdapter = new MethodAdapter(methods, info.getName());
        methodAdapter.setOnItemClickListener(this::openMethodDetail);
        binding.rvMethods.setAdapter(methodAdapter);
        
        FieldAdapter fieldAdapter = new FieldAdapter(fields, info.getName());
        fieldAdapter.setOnItemClickListener(this::openFieldDetail);
        binding.rvFields.setAdapter(fieldAdapter);
        
        constructorsExpanded = false;
        methodsExpanded = false;
        fieldsExpanded = false;
        binding.rvConstructors.setVisibility(View.GONE);
        binding.rvMethods.setVisibility(View.GONE);
        binding.rvFields.setVisibility(View.GONE);
    }
    
    private void openClassGraph() {
        if (currentClassInfo == null) return;
        
        Intent intent = new Intent(this, ClassGraphActivity.class);
        intent.putExtra(ClassGraphActivity.EXTRA_CLASS_NAME, currentClassInfo.getName());
        startActivity(intent);
    }
    
    private void openConstructorDetail(int position, MethodInfo constructor) {
        if (currentClassInfo == null) return;
        
        StringBuilder signatureBuilder = new StringBuilder();
        List<String> paramTypes = constructor.getParameterTypes();
        Intent intent = getIntent(constructor, paramTypes, signatureBuilder);
        intent.putStringArrayListExtra(ConstructorDetailActivity.EXTRA_GENERIC_PARAM_TYPES, 
            new ArrayList<>(constructor.getGenericParameterTypes()));
        startActivity(intent);
    }

    @NonNull
    private Intent getIntent(MethodInfo constructor, List<String> paramTypes, StringBuilder signatureBuilder) {
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) signatureBuilder.append(", ");
            signatureBuilder.append(paramTypes.get(i));
        }
        String signature = signatureBuilder.toString();

        Intent intent = new Intent(this, ConstructorDetailActivity.class);
        intent.putExtra(ConstructorDetailActivity.EXTRA_CLASS_NAME, currentClassInfo.getName());
        intent.putExtra(ConstructorDetailActivity.EXTRA_SIGNATURE, signature);
        intent.putExtra(ConstructorDetailActivity.EXTRA_MODIFIERS, constructor.getModifiersString());
        return intent;
    }

    private void openMethodDetail(MethodInfo method) {
        if (currentClassInfo == null) return;
        
        StringBuilder signatureBuilder = new StringBuilder();
        List<String> paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) signatureBuilder.append(", ");
            signatureBuilder.append(paramTypes.get(i));
        }
        String signature = signatureBuilder.toString();
        
        boolean isStatic = false;
        String modifiers = method.getModifiersString();
        if (modifiers != null && modifiers.contains("static")) {
            isStatic = true;
        }
        
        Intent intent = new Intent(this, MethodDetailActivity.class);
        intent.putExtra(MethodDetailActivity.EXTRA_CLASS_NAME, currentClassInfo.getName());
        intent.putExtra(MethodDetailActivity.EXTRA_METHOD_NAME, method.getName());
        intent.putExtra(MethodDetailActivity.EXTRA_SIGNATURE, signature);
        intent.putExtra(MethodDetailActivity.EXTRA_MODIFIERS, modifiers);
        intent.putExtra(MethodDetailActivity.EXTRA_RETURN_TYPE, method.getReturnType());
        intent.putExtra(MethodDetailActivity.EXTRA_IS_STATIC, isStatic);
        intent.putStringArrayListExtra(MethodDetailActivity.EXTRA_GENERIC_PARAM_TYPES, 
            new ArrayList<>(method.getGenericParameterTypes()));
        intent.putExtra(MethodDetailActivity.EXTRA_DECLARING_CLASS, method.getDeclaringClass());
        intent.putExtra(MethodDetailActivity.EXTRA_DECLARING_CLASS_IS_INTERFACE, method.isDeclaringClassIsInterface());
        startActivity(intent);
    }
    
    private void openFieldDetail(FieldInfo field) {
        if (currentClassInfo == null) return;
        
        boolean isStatic = false;
        String modifiers = field.getModifiersString();
        if (modifiers != null && modifiers.contains("static")) {
            isStatic = true;
        }
        
        String fieldType = field.getGenericType();
        if (fieldType == null || fieldType.isEmpty()) {
            fieldType = field.getType();
        }
        
        Intent intent = new Intent(this, FieldDetailActivity.class);
        intent.putExtra(FieldDetailActivity.EXTRA_CLASS_NAME, currentClassInfo.getName());
        intent.putExtra(FieldDetailActivity.EXTRA_FIELD_NAME, field.getName());
        intent.putExtra(FieldDetailActivity.EXTRA_MODIFIERS, modifiers);
        intent.putExtra(FieldDetailActivity.EXTRA_FIELD_TYPE, fieldType);
        intent.putExtra(FieldDetailActivity.EXTRA_IS_STATIC, isStatic);
        intent.putExtra(FieldDetailActivity.EXTRA_DECLARING_CLASS, field.getDeclaringClass());
        intent.putExtra(FieldDetailActivity.EXTRA_DECLARING_CLASS_IS_INTERFACE, field.isDeclaringClassIsInterface());
        startActivity(intent);
    }
    
    private List<MethodInfo> sortMethodsByName(List<MethodInfo> methods) {
        if (methods == null) return new ArrayList<>();
        List<MethodInfo> sorted = new ArrayList<>(methods);
        sorted.sort(Comparator.comparing(MethodInfo::getName));
        return sorted;
    }
    
    private void toggleConstructors() {
        constructorsExpanded = !constructorsExpanded;
        binding.rvConstructors.setVisibility(constructorsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(binding.headerConstructors, constructorsExpanded);
    }
    
    private void toggleMethods() {
        methodsExpanded = !methodsExpanded;
        binding.rvMethods.setVisibility(methodsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(binding.headerMethods, methodsExpanded);
    }
    
    private void toggleFields() {
        fieldsExpanded = !fieldsExpanded;
        binding.rvFields.setVisibility(fieldsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(binding.headerFields, fieldsExpanded);
    }
    
    private void updateExpandIcon(View header, boolean expanded) {
        ImageView icon = null;
        if (header == binding.headerConstructors) {
            icon = binding.ivConstructorsExpand;
        } else if (header == binding.headerMethods) {
            icon = binding.ivMethodsExpand;
        } else if (header == binding.headerFields) {
            icon = binding.ivFieldsExpand;
        }
        if (icon != null) {
            icon.setRotation(expanded ? 180 : 0);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
