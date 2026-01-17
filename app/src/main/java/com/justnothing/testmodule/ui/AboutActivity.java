package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.FileDirectory;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText((getString(R.string.version_format, FileDirectory.APPLICATION_VERSION)));
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }
}
