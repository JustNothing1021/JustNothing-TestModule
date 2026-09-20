package com.justnothing.testmodule.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.databinding.ActivityHookConfigBinding;
import com.justnothing.testmodule.databinding.ItemHookConfigBinding;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.hooks.conf.ClientHookConfig;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



@SuppressLint("NotifyDataSetChanged")
public class HookConfigActivity extends BaseActivity {

    private ActivityHookConfigBinding binding;

    private HookAdapter adapter;
    private List<HookItem> hookItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHookConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hookItems = new ArrayList<>();
        initViews();
        loadHooks();
    }

    private void initViews() {
        RecyclerView recyclerView = binding.hookList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HookAdapter();
        recyclerView.setAdapter(adapter);

        binding.btnRefresh.setOnClickListener(v -> loadHooks());
        binding.btnEnableAll.setOnClickListener(v -> enableAll(true));
        binding.btnDisableAll.setOnClickListener(v -> enableAll(false));
    }

    private void updateEmptyHint() {
        if (hookItems == null || hookItems.isEmpty()) {
            binding.textEmptyHint.setVisibility(View.VISIBLE);
        } else {
            binding.textEmptyHint.setVisibility(View.GONE);
        }
    }


    private void loadHooks() {
        ThreadPoolManager.submitFastRunnable(() -> {
            List<HookItem> loadedItems = new ArrayList<>();
            Map<String, Boolean> hookStates = ClientHookConfig.getAllHookStates();
            try {
                JSONObject serverConfig = DataBridge.readServerHookConfig();

                if (serverConfig.length() > 0) {
                    for (Iterator<String> it = serverConfig.keys(); it.hasNext(); ) {
                        String key = it.next();
                        JSONObject hookInfo = serverConfig.getJSONObject(key);
                        String name = hookInfo.getString(HookConfig.KEY_NAME);
                        String displayName = hookInfo.getString(HookConfig.KEY_DISPLAY_NAME);
                        if (displayName.isEmpty()) displayName = name;
                        String description = hookInfo.optString(HookConfig.KEY_DESCRIPTION);
                        if (description.isEmpty()) description = getString(R.string.hook_detail_not_found);
                        Boolean clientEnabled = hookStates.get(name);
                        boolean serverEnabled = hookInfo.getBoolean(HookConfig.KEY_ENABLED);
                        boolean enableDisplay;
                        if (clientEnabled == null) {
                            logger.warn("无法获取" + name + "的客户端Hook状态, 将会同步服务端");
                            ClientHookConfig.setHookEnabled(name, serverEnabled);
                            enableDisplay = serverEnabled;
                            description += getString(R.string.hook_config_state_uninitialized_hint);
                        } else {
                            enableDisplay = clientEnabled;
                            if (clientEnabled != serverEnabled)
                                description += getString(R.string.hook_config_state_mismatch_hint);
                        }
                        loadedItems.add(new HookItem(name, displayName, description, enableDisplay));
                    }
                    logger.info("从DataBridge加载了 " + loadedItems.size() + " 个Hook配置");
                } else {
                    logger.warn("未从DataBridge读取到Hook列表");
                }
            } catch (Exception e) {
                logger.error("读取Hook列表失败", e);
            }

            final List<HookItem> finalLoadedItems = loadedItems;
            runOnUiThread(() -> {
                hookItems = finalLoadedItems;
                adapter.notifyDataSetChanged();
                updateEmptyHint();
            });
        });
    }


    private void enableAll(boolean enabled) {
        Map<String, Boolean> map = new HashMap<>();
        for (HookItem item : hookItems) {
            map.put(item.name, enabled);
            item.enabled = enabled;
        }
        ClientHookConfig.setHookStatus(map);
        logger.info((enabled ? "启用" : "禁用") + "所有Hook");
        
        // 延迟重新加载Hook列表，显示服务端状态差异
        mainHandler.postDelayed(this::loadHooks, 300);
    }

    private static class HookItem {
        String name;
        String displayName;
        String description;
        boolean enabled;

        HookItem(String name, String displayName, String description, boolean enabled) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.enabled = enabled;
        }
    }

    private class HookAdapter extends RecyclerView.Adapter<HookAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemHookConfigBinding itemBinding = ItemHookConfigBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HookItem item = hookItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return hookItems.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemHookConfigBinding binding;

            ViewHolder(ItemHookConfigBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(HookItem item) {
                binding.textHookName.setText(item.displayName);
                binding.textHookDescription.setText(item.description);
                binding.switchHookEnabled.setOnCheckedChangeListener(null);
                binding.switchHookEnabled.setChecked(item.enabled);
                binding.switchHookEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (item.enabled == isChecked) return;
                    item.enabled = isChecked;
                    ClientHookConfig.setHookEnabled(item.name, isChecked);
                    logger.info(item.name + "的状态更改为" + (isChecked ? "启用" : "禁用"));
                    mainHandler.postDelayed(HookConfigActivity.this::loadHooks, 300); // 防止循环
                });
            }
        }
    }
}
