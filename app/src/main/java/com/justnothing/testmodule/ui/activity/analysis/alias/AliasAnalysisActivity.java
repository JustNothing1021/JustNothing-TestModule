package com.justnothing.testmodule.ui.activity.analysis.alias;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.ui.adapter.analysis.AliasAdapter;
import com.justnothing.testmodule.ui.viewmodel.analysis.AliasQueryViewModel;

import java.util.List;

public class AliasAnalysisActivity extends AppCompatActivity {

    private AliasQueryViewModel viewModel;

    private View layoutContent;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvTotalCount;
    private RecyclerView recyclerAliases;
    private EditText etAliasName;
    private EditText etAliasCommand;
    private Button btnAddAlias;
    private Button btnClearAll;

    private AliasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alias_analysis);

        initViews();
        initViewModel();
        setupListeners();

        viewModel.loadAliases();
    }

    private void initViews() {
        layoutContent = findViewById(R.id.layout_content);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty = findViewById(R.id.layout_empty);

        tvTotalCount = findViewById(R.id.tv_total_count);
        recyclerAliases = findViewById(R.id.recycler_aliases);
        etAliasName = findViewById(R.id.et_alias_name);
        etAliasCommand = findViewById(R.id.et_alias_command);
        btnAddAlias = findViewById(R.id.btn_add_alias);
        btnClearAll = findViewById(R.id.btn_clear_all);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_alias));
        }

        adapter = new AliasAdapter(this, alias -> {
            // 点击删除
            showDeleteDialog(alias);
        });
        recyclerAliases.setAdapter(adapter);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(AliasQueryViewModel.class);

        viewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                layoutLoading.setVisibility(View.VISIBLE);
                layoutContent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                layoutLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getAliases().observe(this, this::displayAliases);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                layoutContent.setVisibility(View.GONE);
                ((TextView) layoutEmpty.findViewById(R.id.tv_error_message)).setText(error);
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
        btnAddAlias.setOnClickListener(v -> addAlias());
        btnClearAll.setOnClickListener(v -> showClearDialog());
    }

    private void addAlias() {
        String name = etAliasName.getText().toString().trim();
        String command = etAliasCommand.getText().toString().trim();

        if (name.isEmpty()) {
            showToast(getString(R.string.analysis_alias_name_empty));
            return;
        }

        if (command.isEmpty()) {
            showToast(getString(R.string.analysis_alias_command_empty));
            return;
        }

        viewModel.addAlias(name, command);
        etAliasName.setText("");
        etAliasCommand.setText("");
    }

    private void displayAliases(List<AliasInfo> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            layoutContent.setVisibility(View.GONE);
            ((TextView) layoutEmpty.findViewById(R.id.tv_error_message)).setText(getString(R.string.analysis_alias_empty));
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        tvTotalCount.setText(getString(R.string.analysis_alias_total_format, aliases.size()));
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
