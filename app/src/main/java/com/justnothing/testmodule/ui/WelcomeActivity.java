package com.justnothing.testmodule.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.functions.FileUtils;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.tips.TipSystem;
import com.justnothing.testmodule.utils.tips.TipCallback;
import com.justnothing.testmodule.utils.ui.UISettings;
import com.justnothing.methodsclient.StreamClient;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";
    
    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        UISettings uiSettings = UISettings.getInstance(this);
        uiSettings.applyUIScale(this);

        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText(getString(R.string.version_format, FileDirectory.APPLICATION_VERSION));
        
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        setupTipSystem(tvSubtitle);

        Button btnGetHttpConf = findViewById(R.id.btn_get_httpconf);
        Button btnHookConfig = findViewById(R.id.btn_hook_config);
        Button btnLogViewer = findViewById(R.id.btn_log_viewer);
        Button btnScriptManager = findViewById(R.id.btn_script_manager);
        Button btnModuleStatus = findViewById(R.id.btn_module_status);
        Button btnPerformance = findViewById(R.id.btn_performance);
        Button btnDataExport = findViewById(R.id.btn_data_export);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnDidYouKnow = findViewById(R.id.btn_did_you_know);
        Button btnAbout = findViewById(R.id.btn_about);

        btnGetHttpConf.setOnClickListener(v -> {
            Intent intent = new Intent(this, HttpConfigActivity.class);
            startActivity(intent);
        });

        btnHookConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, HookConfigActivity.class);
            startActivity(intent);
        });

        btnLogViewer.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogViewerActivity.class);
            startActivity(intent);
        });

        btnScriptManager.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScriptManagerActivity.class);
            startActivity(intent);
        });

        btnModuleStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, ModuleStatusActivity.class);
            startActivity(intent);
        });

        btnPerformance.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerformanceActivity.class);
            startActivity(intent);
        });

        btnDataExport.setOnClickListener(v -> {
            Intent intent = new Intent(this, DataExportActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnDidYouKnow.setOnClickListener(v -> {
            Intent intent = new Intent(this, DidYouKnowActivity.class);
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        });

        Logger.setContext(this);

        if (!FileUtils.checkStoragePermission(this))
            FileUtils.requestPermission(this);

        if (!FileUtils.checkStoragePermission(this)) {
            Toast.makeText(getApplicationContext(),
                    "没有授予读取文件的权限, \n有的功能可能无法正常运作...", Toast.LENGTH_LONG).show();
        }

    }
    

    private void setupTipSystem(TextView subtitleView) {
        TipSystem tipSystem = new TipSystem();
        
        TipCallback displayTip = tipSystem.getDisplayTipForWelcome();
        
        if (displayTip != null && displayTip.shouldDisplay()) {
            String newContent = displayTip.getContent();

    subtitleView.setText(newContent);
    logger.info("欢迎页面副标题已替换为 \"" + newContent + "\"");
    } else {
        logger.info("本次未加载特殊提示");
        }
    }
}
