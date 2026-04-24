package com.justnothing.testmodule.ui.analysis.memory;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;

public class MemoryAnalysisActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.memory_analysis));
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
