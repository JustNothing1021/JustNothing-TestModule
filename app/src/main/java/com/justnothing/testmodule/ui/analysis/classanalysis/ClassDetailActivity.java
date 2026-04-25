package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.ClassInfo;
import com.justnothing.testmodule.protocol.json.model.MethodInfo;
import com.justnothing.testmodule.protocol.json.model.FieldInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ClassDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_CLASS_NAME = "className";
    
    private ClassQueryViewModel viewModel;
    
    private MaterialCardView cardClassInfo;
    private TextView tvClassName;
    private TextView tvClassType;
    private TextView tvModifiers;
    private TextView tvSuperClass;
    private TextView tvInterfaces;
    private RecyclerView rvConstructors;
    private RecyclerView rvMethods;
    private RecyclerView rvFields;
    private ProgressBar progressBar;
    private TextView tvError;
    private androidx.core.widget.NestedScrollView nestedScrollView;
    
    private String currentClassName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
        
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
        if (nestedScrollView != null) {
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, 0));
        }
    }
    
    private void initViews() {
        cardClassInfo = findViewById(R.id.card_class_info);
        tvClassName = findViewById(R.id.tv_class_name);
        tvClassType = findViewById(R.id.tv_class_type);
        tvModifiers = findViewById(R.id.tv_modifiers);
        tvSuperClass = findViewById(R.id.tv_super_class);
        tvInterfaces = findViewById(R.id.tv_interfaces);
        rvConstructors = findViewById(R.id.rv_constructors);
        rvMethods = findViewById(R.id.rv_methods);
        rvFields = findViewById(R.id.rv_fields);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        nestedScrollView = findViewById(R.id.nested_scroll);
        
        rvConstructors.setLayoutManager(new LinearLayoutManager(this));
        rvMethods.setLayoutManager(new LinearLayoutManager(this));
        rvFields.setLayoutManager(new LinearLayoutManager(this));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.analysis_class_info);
        }
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassQueryViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            cardClassInfo.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        viewModel.getClassInfo().observe(this, this::displayClassInfo);
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(error);
                cardClassInfo.setVisibility(View.GONE);
            }
        });
    }
    
    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        
        tvError.setVisibility(View.GONE);
        cardClassInfo.setVisibility(View.VISIBLE);
        
        tvClassName.setText(info.getName());
        
        String classTypeStr = getClassTypeString(info);
        tvClassType.setText(classTypeStr);
        
        String modifiers = info.getModifiersString();
        if (modifiers != null && !modifiers.isEmpty()) {
            tvModifiers.setText(modifiers);
            tvModifiers.setVisibility(View.VISIBLE);
        } else {
            tvModifiers.setText(getString(R.string.analysis_no_modifiers));
            tvModifiers.setVisibility(View.VISIBLE);
        }
        
        String superClass = info.getSuperClass();
        if (superClass != null && !superClass.isEmpty()) {
            tvSuperClass.setText(superClass);
        } else {
            tvSuperClass.setText(getString(R.string.analysis_no_super_class));
        }
        
        List<String> interfaces = info.getInterfaces();
        if (interfaces != null && !interfaces.isEmpty()) {
            tvInterfaces.setText(String.join(", ", interfaces));
            tvInterfaces.setVisibility(View.VISIBLE);
        } else {
            tvInterfaces.setText(getString(R.string.none));
            tvInterfaces.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.label_interfaces).setVisibility(View.VISIBLE);
        
        List<MethodInfo> constructors = info.getConstructors();
        if (constructors != null && !constructors.isEmpty()) {
            ConstructorAdapter constructorAdapter = new ConstructorAdapter(constructors);
            constructorAdapter.setOnItemClickListener((position, constructor) -> openConstructorDetail(constructor));
            rvConstructors.setAdapter(constructorAdapter);
            findViewById(R.id.label_constructors).setVisibility(View.VISIBLE);
            rvConstructors.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.label_constructors).setVisibility(View.GONE);
            rvConstructors.setVisibility(View.GONE);
        }
        
        List<MethodInfo> methods = info.getMethods();
        if (methods != null && !methods.isEmpty()) {
            LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
            for (MethodInfo method : methods) {
                uniqueNames.add(method.getName());
            }
            
            TextView tvMethodCount = findViewById(R.id.tv_method_count);
            tvMethodCount.setText(getString(R.string.analysis_methods_count_format, uniqueNames.size(), methods.size()));
            
            MethodAdapter methodAdapter = new MethodAdapter(methods, info.getName());
            methodAdapter.setOnItemClickListener(method -> openMethodDetail(method, info.getName()));
            rvMethods.setAdapter(methodAdapter);
            findViewById(R.id.label_methods).setVisibility(View.VISIBLE);
            rvMethods.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.label_methods).setVisibility(View.GONE);
            rvMethods.setVisibility(View.GONE);
        }
        
        List<FieldInfo> fields = info.getFields();
        if (fields != null && !fields.isEmpty()) {
            FieldAdapter fieldAdapter = new FieldAdapter(fields, info.getName());
            fieldAdapter.setOnItemClickListener(field -> openFieldDetail(field, info.getName()));
            rvFields.setAdapter(fieldAdapter);
            findViewById(R.id.label_fields).setVisibility(View.VISIBLE);
            rvFields.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.label_fields).setVisibility(View.GONE);
            rvFields.setVisibility(View.GONE);
        }
        
        if (nestedScrollView != null) {
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, 0));
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
