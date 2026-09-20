package com.justnothing.testmodule.ui.activity.analysis.alias;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.databinding.ActivityAliasAnalysisBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.adapter.analysis.AliasAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.AliasQueryViewModel;

import java.util.List;

public class AliasAnalysisActivity extends BaseActivity {

    private AliasQueryViewModel viewModel;
    private ActivityAliasAnalysisBinding binding;

    private AliasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAliasAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
        setupListeners();

        viewModel.loadAliases();
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_alias));
        }

        adapter = new AliasAdapter(alias -> {
            // 点击删除
            showDeleteDialog(alias);
        });
        binding.recyclerAliases.setAdapter(adapter);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AliasQueryViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.layoutLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getAliases().observe(this, this::displayAliases);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.tvErrorMessage.setText(error);
                showToast(error);
            }
        });

        viewModel.getMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
            }
        });
    }

    private void setupListeners() {
        binding.btnAddAlias.setOnClickListener(v -> addAlias());
        binding.btnClearAll.setOnClickListener(v -> showClearDialog());
    }

    private void addAlias() {
        String name = binding.etAliasName.getText().toString().trim();
        String command = binding.etAliasCommand.getText().toString().trim();

        if (name.isEmpty()) {
            showToast(getString(R.string.analysis_alias_name_empty));
            return;
        }

        if (command.isEmpty()) {
            showToast(getString(R.string.analysis_alias_command_empty));
            return;
        }

        viewModel.addAlias(name, command);
        binding.etAliasName.setText("");
        binding.etAliasCommand.setText("");
    }

    private void displayAliases(List<AliasInfo> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.GONE);
            binding.tvErrorMessage.setText(getString(R.string.analysis_alias_empty));
            return;
        }

        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);

        binding.tvTotalCount.setText(getString(R.string.analysis_alias_total_format, aliases.size()));
        adapter.setAliases(aliases);
    }

    private void showDeleteDialog(AliasInfo alias) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.analysis_alias_delete_title))
                .setMessage(getString(R.string.analysis_alias_delete_message, alias.getName()))
                .setPositiveButton(getString(R.string.general_confirm), (dialog, which) -> {
                    viewModel.removeAlias(alias.getName());
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.analysis_alias_clear_title))
                .setMessage(getString(R.string.analysis_alias_clear_message))
                .setPositiveButton(getString(R.string.general_confirm), (dialog, which) -> {
                    viewModel.clearAliases();
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
