package com.justnothing.testmodule.ui.activity.analysis.network;

import android.os.Bundle;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityPlaceholderBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;

public class NetworkAnalysisActivity extends BaseActivity {

    private ActivityPlaceholderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_network_analysis));
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
