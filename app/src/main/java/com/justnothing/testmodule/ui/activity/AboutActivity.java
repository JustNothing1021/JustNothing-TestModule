package com.justnothing.testmodule.ui.activity;

import android.os.Bundle;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.databinding.ActivityAboutBinding;

public class AboutActivity extends BaseActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvVersion.setText((getString(R.string.version_format, FileDirectory.APPLICATION_VERSION)));
        binding.btnBack.setOnClickListener(v -> finish());
    }
}
