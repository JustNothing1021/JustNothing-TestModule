package com.justnothing.testmodule.ui.activity.analysis.classanalysis;

import android.os.Bundle;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityPlaceholderBinding;
import com.justnothing.testmodule.ui.activity.BaseActivity;

public class FieldViewActivity extends BaseActivity {
    private ActivityPlaceholderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_view_fields));
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
