package com.justnothing.testmodule.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivitySystemAnalysisBinding;
import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.ui.activity.analysis.classanalysis.ClassAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.memory.MemoryAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.hook.HookManagerActivity;
import com.justnothing.testmodule.ui.activity.analysis.thread.ThreadAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.network.NetworkAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.packages.PackagesAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.exportcontext.ExportContextAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.systeminfo.SystemInfoAnalysisActivity;
import com.justnothing.testmodule.ui.activity.analysis.alias.AliasAnalysisActivity;

/**
 * 系统分析菜单Activity（第二层）。
 * 
 * <p>提供各类分析功能的入口：
 * <ul>
 *   <li>类分析 - 查询类信息、调用方法</li>
 *   <li>内存分析 - 内存使用、GC管理</li>
 *   <li>Hook管理 - 动态Hook配置</li>
 *   <li>线程分析 - 线程状态、死锁检测</li>
 *   <li>网络分析 - 网络请求监控</li>
 * </ul>
 * </p>
 */
public class SystemAnalysisActivity extends BaseActivity {

    private ActivitySystemAnalysisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySystemAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        setupListeners();
        checkServerStatus();
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.system_analysis));
        }
    }

    private void setupListeners() {
        binding.cardClassAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, ClassAnalysisActivity.class));
        });

        binding.cardMemoryAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, MemoryAnalysisActivity.class));
        });

        binding.cardHookManager.setOnClickListener(v -> {
            startActivity(new Intent(this, HookManagerActivity.class));
        });

        binding.cardThreadAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, ThreadAnalysisActivity.class));
        });

        binding.cardNetworkAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, NetworkAnalysisActivity.class));
        });

        binding.cardPackages.setOnClickListener(v -> {
            startActivity(new Intent(this, PackagesAnalysisActivity.class));
        });

        binding.cardExportContext.setOnClickListener(v -> {
            startActivity(new Intent(this, ExportContextAnalysisActivity.class));
        });

        binding.cardSystemInfo.setOnClickListener(v -> {
            startActivity(new Intent(this, SystemInfoAnalysisActivity.class));
        });

        binding.cardAlias.setOnClickListener(v -> {
            startActivity(new Intent(this, AliasAnalysisActivity.class));
        });
    }

    private void checkServerStatus() {
        new Thread(() -> {
            boolean available = UiClient.getInstance().isServerAvailable();
            runOnUiThread(() -> updateServerStatus(available));
        }).start();
    }

    private void updateServerStatus(boolean available) {
        TextView statusView = binding.tvServerStatus;
        if (available) {
            statusView.setText(getString(R.string.analysis_server_connected));
            statusView.setTextColor(getColor(R.color.green));
        } else {
            statusView.setText(getString(R.string.analysis_server_disconnected));
            statusView.setTextColor(getColor(R.color.red));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServerStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
