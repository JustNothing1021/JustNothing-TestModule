package com.justnothing.testmodule.ui.activity.analysis.network;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;

public class NetworkAnalysisActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_network_analysis));
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
