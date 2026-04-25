package com.justnothing.testmodule.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.justnothing.testmodule.R;
import com.justnothing.methodsclient.UiClient;
import com.justnothing.testmodule.ui.analysis.classanalysis.ClassAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.memory.MemoryAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.hook.HookManagerActivity;
import com.justnothing.testmodule.ui.analysis.thread.ThreadAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.network.NetworkAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.packages.PackagesAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.exportcontext.ExportContextAnalysisActivity;
import com.justnothing.testmodule.ui.analysis.systeminfo.SystemInfoAnalysisActivity;

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
public class SystemAnalysisActivity extends AppCompatActivity {
    
    private CardView cardClassAnalysis;
    private CardView cardMemoryAnalysis;
    private CardView cardHookManager;
    private CardView cardThreadAnalysis;
    private CardView cardNetworkAnalysis;
    private CardView cardPackages;
    private CardView cardExportContext;
    private CardView cardSystemInfo;
    private View tvServerStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_analysis);
        
        initViews();
        setupListeners();
        checkServerStatus();
    }
    
    private void initViews() {
        cardClassAnalysis = findViewById(R.id.card_class_analysis);
        cardMemoryAnalysis = findViewById(R.id.card_memory_analysis);
        cardHookManager = findViewById(R.id.card_hook_manager);
        cardThreadAnalysis = findViewById(R.id.card_thread_analysis);
        cardNetworkAnalysis = findViewById(R.id.card_network_analysis);
        cardPackages = findViewById(R.id.card_packages);
        cardExportContext = findViewById(R.id.card_export_context);
        cardSystemInfo = findViewById(R.id.card_system_info);
        tvServerStatus = findViewById(R.id.tv_server_status);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.system_analysis));
        }
    }
    
    private void setupListeners() {
        cardClassAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, ClassAnalysisActivity.class));
        });
        
        cardMemoryAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, MemoryAnalysisActivity.class));
        });
        
        cardHookManager.setOnClickListener(v -> {
            startActivity(new Intent(this, HookManagerActivity.class));
        });
        
        cardThreadAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, ThreadAnalysisActivity.class));
        });
        
        cardNetworkAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, NetworkAnalysisActivity.class));
        });

        cardPackages.setOnClickListener(v -> {
            startActivity(new Intent(this, PackagesAnalysisActivity.class));
        });

        cardExportContext.setOnClickListener(v -> {
            startActivity(new Intent(this, ExportContextAnalysisActivity.class));
        });

        cardSystemInfo.setOnClickListener(v -> {
            startActivity(new Intent(this, SystemInfoAnalysisActivity.class));
        });
    }
    
    private void checkServerStatus() {
        new Thread(() -> {
            boolean available = UiClient.getInstance().isServerAvailable();
            runOnUiThread(() -> updateServerStatus(available));
        }).start();
    }
    
    private void updateServerStatus(boolean available) {
        if (tvServerStatus instanceof TextView statusView) {
            if (available) {
                statusView.setText(getString(R.string.analysis_server_connected));
                statusView.setTextColor(getColor(R.color.green));
            } else {
                statusView.setText(getString(R.string.analysis_server_disconnected));
                statusView.setTextColor(getColor(R.color.red));
            }
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
