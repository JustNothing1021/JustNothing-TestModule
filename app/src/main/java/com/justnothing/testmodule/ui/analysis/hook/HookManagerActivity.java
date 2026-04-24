package com.justnothing.testmodule.ui.analysis.hook;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;

public class HookManagerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hook管理");
        }
    }
    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
