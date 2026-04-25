package com.justnothing.testmodule.ui.menu;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.protocol.json.model.ClassInfo;
import com.justnothing.testmodule.ui.adapter.MethodsAdapter;
import com.justnothing.testmodule.ui.analysis.classanalysis.ClassBrowserViewModel;
import com.justnothing.testmodule.utils.logging.Logger;


public class ClassBrowserActivity extends AppCompatActivity {
    private static final Logger logger = Logger.getLoggerForName("ClassBrowserActivity");
    private ClassBrowserViewModel viewModel;
    
    private EditText etClassName;
    private Button btnQuery;
    private ProgressBar progressBar;
    private TextView tvServerStatus;
    private TextView tvClassName;
    private TextView tvSuperClass;
    private TextView tvInterfaces;
    private TextView tvModifiers;
    private RecyclerView rvMethods;
    private RecyclerView rvFields;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_browser);
        
        initViews();
        initViewModel();
        setupListeners();
        
        viewModel.checkServerStatus();
    }
    
    private void initViews() {
        etClassName = findViewById(R.id.et_class_name);
        btnQuery = findViewById(R.id.btn_query);
        progressBar = findViewById(R.id.progress_bar);
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvClassName = findViewById(R.id.tv_class_name);
        tvSuperClass = findViewById(R.id.tv_super_class);
        tvInterfaces = findViewById(R.id.tv_interfaces);
        tvModifiers = findViewById(R.id.tv_modifiers);
        rvMethods = findViewById(R.id.rv_methods);
        rvFields = findViewById(R.id.rv_fields);
        
        rvMethods.setLayoutManager(new LinearLayoutManager(this));
        rvFields.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassBrowserViewModel.class);
        
        viewModel.isLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnQuery.setEnabled(!isLoading);
        });
        
        viewModel.getServerStatus().observe(this, available -> {
            tvServerStatus.setText(available ? getString(R.string.analysis_server_connected) : getString(R.string.analysis_server_disconnected));
            tvServerStatus.setTextColor(getColor(available ? R.color.green : R.color.red));
        });
        
        viewModel.getClassInfo().observe(this, this::displayClassInfo);
        
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupListeners() {
        btnQuery.setOnClickListener(v -> {
            String className = etClassName.getText().toString().trim();
            viewModel.queryClassInfo(className);
        });
        
        etClassName.setOnEditorActionListener((v, actionId, event) -> {
            btnQuery.performClick();
            return true;
        });
    }
    
    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        logger.info("显示类信息: " + info.getName());

        tvClassName.setText(info.getName());
        tvSuperClass.setText(info.getSuperClass() != null ? info.getSuperClass() : getString(R.string.none));
        tvInterfaces.setText(String.join(", ", info.getInterfaces()));
        tvModifiers.setText(info.getModifiersString());
        
        MethodsAdapter methodsAdapter = new MethodsAdapter(info.getMethods());
        rvMethods.setAdapter(methodsAdapter);
        
        com.justnothing.testmodule.ui.analysis.classanalysis.FieldAdapter fieldsAdapter = new com.justnothing.testmodule.ui.analysis.classanalysis.FieldAdapter(info.getFields(), info.getName());
        rvFields.setAdapter(fieldsAdapter);
    }
}
