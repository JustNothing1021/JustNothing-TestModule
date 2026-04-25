package com.justnothing.testmodule.ui.analysis.classanalysis;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;

public class FieldViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.analysis_view_fields));
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
