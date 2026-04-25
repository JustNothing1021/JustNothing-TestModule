package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.ClassInfo;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;

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
public class ClassAnalysisActivity extends AppCompatActivity {
    
    private ClassQueryViewModel viewModel;
    private ClassInfo currentClassInfo;
    
    private TextInputEditText etClassName;
    private Button btnQuery;
    private View layoutResult;
    private View layoutLoading;
    private View layoutEmpty;
    
    private TextView tvClassName;
    private TextView tvModifiers;
    private TextView tvSuperClass;
    private TextView tvInterfaces;
    private View cardClassInfo;
    
    private View headerConstructors;
    private View headerMethods;
    private View headerFields;
    private RecyclerView rvConstructors;
    private RecyclerView rvMethods;
    private RecyclerView rvFields;
    
    private boolean constructorsExpanded = false;
    private boolean methodsExpanded = false;
    private boolean fieldsExpanded = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_analysis);
        
        initViews();
        initViewModel();
        setupListeners();
    }
    
    private void initViews() {
        etClassName = findViewById(R.id.et_class_name);
        btnQuery = findViewById(R.id.btn_query);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        layoutResult = findViewById(R.id.layout_result);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty = findViewById(R.id.layout_empty);
        
        tvClassName = findViewById(R.id.tv_class_name);
        tvModifiers = findViewById(R.id.tv_modifiers);
        tvSuperClass = findViewById(R.id.tv_super_class);
        tvInterfaces = findViewById(R.id.tv_interfaces);
        cardClassInfo = findViewById(R.id.card_class_info);
        
        headerConstructors = findViewById(R.id.header_constructors);
        headerMethods = findViewById(R.id.header_methods);
        headerFields = findViewById(R.id.header_fields);
        
        rvConstructors = findViewById(R.id.rv_constructors);
        rvMethods = findViewById(R.id.rv_methods);
        rvFields = findViewById(R.id.rv_fields);
        
        rvConstructors.setLayoutManager(new LinearLayoutManager(this));
        rvMethods.setLayoutManager(new LinearLayoutManager(this));
        rvFields.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.class_analysis));
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassQueryViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                layoutLoading.setVisibility(View.VISIBLE);
                layoutResult.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            }
            btnQuery.setEnabled(!isLoading);
        });
        
        viewModel.getClassInfo().observe(this, this::displayClassInfo);
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                layoutLoading.setVisibility(View.GONE);
                layoutResult.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                ((TextView) layoutEmpty.findViewById(R.id.tv_error_message)).setText(error);
            }
        });
    }
    
    private void setupListeners() {
        btnQuery.setOnClickListener(v -> {
            String className = etClassName.getText() != null ? etClassName.getText().toString().trim() : "";
            if (!className.isEmpty()) {
                viewModel.queryClassInfo(className);
            }
        });
        
        etClassName.setOnEditorActionListener((v, actionId, event) -> {
            btnQuery.performClick();
            return true;
        });
        
        cardClassInfo.setOnClickListener(v -> {
            if (currentClassInfo != null) {
                openClassGraph();
            }
        });
        
        headerConstructors.setOnClickListener(v -> toggleConstructors());
        headerMethods.setOnClickListener(v -> toggleMethods());
        headerFields.setOnClickListener(v -> toggleFields());
    }
    
    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        
        currentClassInfo = info;
        
        layoutLoading.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        
        tvClassName.setText(info.getName());
        tvModifiers.setText(info.getModifiersString());
        tvSuperClass.setText(info.getSuperClass() != null ? info.getSuperClass() : getString(R.string.none));
        tvInterfaces.setText(info.getInterfaces().isEmpty() ? getString(R.string.none) : TextUtils.join(", ", info.getInterfaces()));
        
        List<MethodInfo> constructors = info.getConstructors();
        ((TextView) findViewById(R.id.tv_constructors_title)).setText(
            getString(R.string.analysis_constructors_format, constructors.size()));
        
        List<MethodInfo> methods = sortMethodsByName(info.getMethods());
        Set<String> uniqueNames = new HashSet<>();
        for (MethodInfo m : methods) {
            uniqueNames.add(m.getName());
        }
        ((TextView) findViewById(R.id.tv_methods_title)).setText(
            getString(R.string.analysis_methods_and_count_format, uniqueNames.size(), methods.size()));
        
        List<FieldInfo> fields = info.getFields();
        ((TextView) findViewById(R.id.tv_fields_title)).setText(
            getString(R.string.analysis_fields_format, fields.size()));
        
        ConstructorAdapter constructorAdapter = new ConstructorAdapter(constructors);
        constructorAdapter.setOnItemClickListener(this::openConstructorDetail);
        rvConstructors.setAdapter(constructorAdapter);
        
        MethodAdapter methodAdapter = new MethodAdapter(methods, info.getName());
        methodAdapter.setOnItemClickListener(this::openMethodDetail);
        rvMethods.setAdapter(methodAdapter);
        
        FieldAdapter fieldAdapter = new FieldAdapter(fields, info.getName());
        fieldAdapter.setOnItemClickListener(this::openFieldDetail);
        rvFields.setAdapter(fieldAdapter);
        
        constructorsExpanded = false;
        methodsExpanded = false;
        fieldsExpanded = false;
        rvConstructors.setVisibility(View.GONE);
        rvMethods.setVisibility(View.GONE);
        rvFields.setVisibility(View.GONE);
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
        rvConstructors.setVisibility(constructorsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(headerConstructors, constructorsExpanded);
    }
    
    private void toggleMethods() {
        methodsExpanded = !methodsExpanded;
        rvMethods.setVisibility(methodsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(headerMethods, methodsExpanded);
    }
    
    private void toggleFields() {
        fieldsExpanded = !fieldsExpanded;
        rvFields.setVisibility(fieldsExpanded ? View.VISIBLE : View.GONE);
        updateExpandIcon(headerFields, fieldsExpanded);
    }
    
    private void updateExpandIcon(View header, boolean expanded) {
        android.widget.ImageView icon = null;
        if (header == headerConstructors) {
            icon = findViewById(R.id.iv_constructors_expand);
        } else if (header == headerMethods) {
            icon = findViewById(R.id.iv_methods_expand);
        } else if (header == headerFields) {
            icon = findViewById(R.id.iv_fields_expand);
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
