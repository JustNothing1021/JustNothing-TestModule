package com.justnothing.testmodule.ui.activity.analysis.packages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.ui.adapter.analysis.PackagesAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.PackagesQueryViewModel;

import java.util.List;

public class PackagesAnalysisActivity extends AppCompatActivity {

    private PackagesQueryViewModel viewModel;
    private PackagesAdapter adapter;

    private View layoutResult;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvTotalCount;
    private Button btnRefresh;
    private RecyclerView rvPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packages_analysis);

        initViews();
        initViewModel();
        setupListeners();

        viewModel.queryPackages();
    }

    private void initViews() {
        layoutResult = findViewById(R.id.layout_result);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty = findViewById(R.id.layout_empty);

        tvTotalCount = findViewById(R.id.tv_total_count);
        btnRefresh = findViewById(R.id.btn_refresh);
        rvPackages = findViewById(R.id.rv_packages);

        rvPackages.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.packages_analysis));
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(PackagesQueryViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                layoutLoading.setVisibility(View.VISIBLE);
                layoutResult.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            }
            btnRefresh.setEnabled(!isLoading);
        });

        viewModel.getPackages().observe(this, this::displayPackages);

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
        btnRefresh.setOnClickListener(v -> viewModel.queryPackages());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void displayPackages(List<String> packages) {
        if (packages == null) return;

        layoutLoading.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        tvTotalCount.setText(getString(R.string.packages_total_format, packages.size()));

        adapter = new PackagesAdapter(packages, packageName -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("package name", packageName);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.packages_copy_success), Toast.LENGTH_SHORT).show();
        });
        rvPackages.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
