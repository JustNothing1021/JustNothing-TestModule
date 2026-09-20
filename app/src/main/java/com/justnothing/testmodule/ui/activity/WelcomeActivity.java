package com.justnothing.testmodule.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.databinding.ActivityWelcomeBinding;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.tips.TipSystem;
import com.justnothing.testmodule.utils.tips.TipCallback;
import com.justnothing.testmodule.utils.ui.UISettings;

public class WelcomeActivity extends BaseActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UISettings uiSettings = UISettings.getInstance(this);
        uiSettings.applyUIScale(this);

        binding.tvVersion.setText(getString(R.string.version_format, FileDirectory.APPLICATION_VERSION));
        
        setupTipSystem(binding.tvSubtitle);

        binding.btnSystemAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(this, SystemAnalysisActivity.class);
            startActivity(intent);
        });

        binding.btnGetHttpconf.setOnClickListener(v -> {
            Intent intent = new Intent(this, HttpConfigActivity.class);
            startActivity(intent);
        });

        binding.btnHookConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, HookConfigActivity.class);
            startActivity(intent);
        });

        binding.btnLogViewer.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogViewerActivity.class);
            startActivity(intent);
        });

        binding.btnScriptManager.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScriptManagerActivity.class);
            startActivity(intent);
        });

        binding.btnModuleStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, ModuleStatusActivity.class);
            startActivity(intent);
        });

        binding.btnPerformance.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerformanceActivity.class);
            startActivity(intent);
        });

        binding.btnDataExport.setOnClickListener(v -> {
            Intent intent = new Intent(this, DataExportActivity.class);
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnDidYouKnow.setOnClickListener(v -> {
            Intent intent = new Intent(this, DidYouKnowActivity.class);
            startActivity(intent);
        });

        binding.btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        });

        Logger.setContext(this);

        if (!BootMonitor.PermissionUtils.checkStoragePermission(this))
            BootMonitor.PermissionUtils.requestPermission(this);

        if (!BootMonitor.PermissionUtils.checkStoragePermission(this)) {
            showToast(getString(R.string.no_file_permission_warn), Toast.LENGTH_LONG);
        }

    }
    

    private void setupTipSystem(TextView subtitleView) {
        TipSystem tipSystem = new TipSystem();
        
        TipCallback displayTip = tipSystem.getDisplayTipForWelcome();
        
        if (displayTip != null) {
            String newContent = displayTip.getContent();
            subtitleView.setText(newContent);
            logger.info("欢迎页面副标题已替换为 \"" + newContent + "\"");
        } else {
            logger.info("本次未加载特殊提示");
        }
    }
}
