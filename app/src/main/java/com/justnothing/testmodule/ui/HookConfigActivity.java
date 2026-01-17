package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.HookConfig;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.hooks.ClientHookConfig;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HookConfigActivity extends AppCompatActivity {

    private static class ConfigLogger extends Logger {
        @Override
        public String getTag() {
            return "HookConfigActivity";
        }
    }

    private final ConfigLogger logger = new ConfigLogger();

    private HookAdapter adapter;
    private List<HookItem> hookItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_config);
        hookItems = new ArrayList<>();
        initViews();
        loadHooks();
    }

    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.hook_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HookAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btn_refresh).setOnClickListener(v -> loadHooks());
        findViewById(R.id.btn_enable_all).setOnClickListener(v -> enableAll(true));
        findViewById(R.id.btn_disable_all).setOnClickListener(v -> enableAll(false));
    }

    private void updateEmptyHint() {
        TextView emptyHint = findViewById(R.id.text_empty_hint);
        if (hookItems == null || hookItems.isEmpty()) {
            emptyHint.setVisibility(View.VISIBLE);
        } else {
            emptyHint.setVisibility(View.GONE);
        }
    }

    private void loadHooks() {
        new Thread(() -> {
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
                        if (description.isEmpty()) description = "找不到这个Hook的详细信息...";
                        Boolean clientEnabled = hookStates.get(name);
                        boolean serverEnabled = hookInfo.getBoolean(HookConfig.KEY_ENABLED);
                        boolean enableDisplay;
                        if (clientEnabled == null) {
                            logger.warn("无法获取" + name + "的客户端Hook状态, 将会同步服务端");
                            ClientHookConfig.setHookEnabled(name, serverEnabled);
                            enableDisplay = serverEnabled;
                            description += "(未设置激活状态, 同步了服务端的状态)";
                        } else {
                            enableDisplay = clientEnabled;
                            if (clientEnabled != serverEnabled)
                                description += "(服务端状态不同, 重启后更新)";
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
        }).start();
    }


    private void enableAll(boolean enabled) {
        for (HookItem item : hookItems) {
            ClientHookConfig.setHookEnabled(item.displayName, enabled);
            item.enabled = enabled;
        }
        adapter.notifyDataSetChanged();
        updateEmptyHint();
        logger.info((enabled ? "启用" : "禁用") + "所有Hook");
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
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hook_config, parent, false);
            return new ViewHolder(view);
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
            TextView textName;
            TextView textDescription;
            Switch switchEnabled;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_hook_name);
                textDescription = itemView.findViewById(R.id.text_hook_description);
                switchEnabled = itemView.findViewById(R.id.switch_hook_enabled);
            }

            void bind(HookItem item) {
                textName.setText(item.displayName);
                textDescription.setText(item.description);
                
                switchEnabled.setOnCheckedChangeListener(null);
                switchEnabled.setChecked(item.enabled);
                switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.enabled = isChecked;
                    ClientHookConfig.setHookEnabled(item.name, isChecked);
                    logger.info(item.name+ "的状态更改为" + (isChecked ? "启用" : "禁用"));
                });
            }
        }
    }
}
