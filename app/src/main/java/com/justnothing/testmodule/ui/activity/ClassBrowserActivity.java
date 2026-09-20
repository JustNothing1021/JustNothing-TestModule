package com.justnothing.testmodule.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.databinding.ActivityClassBrowserBinding;
import com.justnothing.testmodule.ui.adapter.analysis.FieldAdapter;
import com.justnothing.testmodule.ui.adapter.analysis.MethodsAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.ClassBrowserViewModel;


public class ClassBrowserActivity extends BaseActivity {
    private ActivityClassBrowserBinding binding;
    private ClassBrowserViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
        setupListeners();

        viewModel.checkServerStatus();
    }

    private void initViews() {
        binding.rvMethods.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFields.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ClassBrowserViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnQuery.setEnabled(!isLoading);
        });

        viewModel.getServerStatus().observe(this, available -> {
            binding.tvServerStatus.setText(available ? getString(R.string.analysis_server_connected) : getString(R.string.analysis_server_disconnected));
            binding.tvServerStatus.setTextColor(getColor(available ? R.color.green : R.color.red));
        });

        viewModel.getClassInfo().observe(this, this::displayClassInfo);

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                showToast(error);
            }
        });
    }

    private void setupListeners() {
        binding.btnQuery.setOnClickListener(v -> {
            String className = binding.etClassName.getText().toString().trim();
            viewModel.queryClassInfo(className);
        });

        binding.etClassName.setOnEditorActionListener((v, actionId, event) -> {
            binding.btnQuery.performClick();
            return true;
        });
    }

    private void displayClassInfo(ClassInfo info) {
        if (info == null) return;
        logger.info("显示类信息: " + info.getName());

        binding.tvClassName.setText(info.getName());
        binding.tvSuperClass.setText(info.getSuperClass() != null ? info.getSuperClass() : getString(R.string.none));
        binding.tvInterfaces.setText(String.join(", ", info.getInterfaces()));
        binding.tvModifiers.setText(info.getModifiersString());

        MethodsAdapter methodsAdapter = new MethodsAdapter(info.getMethods());
        binding.rvMethods.setAdapter(methodsAdapter);

        FieldAdapter fieldsAdapter = new FieldAdapter(info.getFields(), info.getName());
        binding.rvFields.setAdapter(fieldsAdapter);
    }
}
