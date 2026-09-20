package com.justnothing.testmodule.ui.activity.analysis.packages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityPackagesAnalysisBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;
import com.justnothing.testmodule.ui.adapter.analysis.PackagesAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.PackagesQueryViewModel;

import java.util.List;

public class PackagesAnalysisActivity extends BaseActivity {

    private PackagesQueryViewModel viewModel;
    private ActivityPackagesAnalysisBinding binding;
    private PackagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPackagesAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initViewModel();
        setupListeners();

        viewModel.queryPackages();
    }

    private void initViews() {
        binding.rvPackages.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.packages_analysis));
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(PackagesQueryViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutResult.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            }
            binding.btnRefresh.setEnabled(!isLoading);
        });

        viewModel.getPackages().observe(this, this::displayPackages);

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
        binding.btnRefresh.setOnClickListener(v -> viewModel.queryPackages());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void displayPackages(List<String> packages) {
        if (packages == null) return;

        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        binding.tvTotalCount.setText(getString(R.string.packages_total_format, packages.size()));

        adapter = new PackagesAdapter(packages, packageName -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("package name", packageName);
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.packages_copy_success));
        });
        binding.rvPackages.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
