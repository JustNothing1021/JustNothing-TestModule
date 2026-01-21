package com.justnothing.testmodule.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.executor.SocketCommandExecutor;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.io.IOManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScriptManagerActivity extends AppCompatActivity {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String COMMAND = "commands";
    private static final String CATEGORY = "category";

    private static class ScriptLogger extends Logger {
        @Override
        public String getTag() {
            return "ScriptManagerActivity";
        }
    }

    private final ScriptLogger logger = new ScriptLogger();

    private List<Script> scripts;
    private ScriptAdapter adapter;
    private File scriptsFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_manager);
        scriptsFile = new File(getFilesDir(), FileDirectory.SCRIPTS_FILE_NAME);
        initViews();
        loadScripts();
    }

    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.script_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScriptAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btn_add_script).setOnClickListener(v -> showAddScriptDialog());
        findViewById(R.id.btn_import).setOnClickListener(v -> showImportDialog());
    }

    private void loadScripts() {
        scripts = new ArrayList<>();
        if (scriptsFile.exists()) {
            try {
                String json = readFile(scriptsFile);
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Script script = new Script(
                            obj.getString(NAME),
                            obj.optString(DESCRIPTION, getString(R.string.script_no_description)),
                            obj.getString(COMMAND),
                            obj.optString(CATEGORY, getString(R.string.general_default))
                    );
                    scripts.add(script);
                }
                logger.info("加载了 " + scripts.size() + " 个脚本");
            } catch (Exception e) {
                logger.error("加载脚本失败", e);
            }
        } else {
            Script defaultScript = new Script(
                    getString(R.string.script_example_script_name),
                    getString(R.string.script_example_script_desc),
                    getString(R.string.script_example_script_command),
                    getString(R.string.script_example_script_category)
            );
            Script defaultScript2 = new Script(
                    getString(R.string.script_example_script_2_name),
                    getString(R.string.script_example_script_2_desc),
                    getString(R.string.script_example_script_2_command),
                    getString(R.string.script_example_script_category)
            );
            Script defaultScript3 = new Script(
                    getString(R.string.script_example_script_3_name),
                    getString(R.string.script_example_script_3_desc),
                    getString(R.string.script_example_script_3_command),
                    getString(R.string.script_example_script_category)
            );
            scripts.add(defaultScript);
            scripts.add(defaultScript2);
            scripts.add(defaultScript3);
        }
        adapter.notifyDataSetChanged();
    }

    private void saveScripts() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Script script : scripts) {
                JSONObject obj = new JSONObject();
                obj.put(NAME, script.name);
                obj.put(DESCRIPTION, script.description);
                obj.put(COMMAND, script.command);
                obj.put(CATEGORY, script.category);
                jsonArray.put(obj);
            }
            writeFile(scriptsFile, jsonArray.toString());
            logger.info("保存了 " + scripts.size() + " 个脚本");
        } catch (Exception e) {
            logger.error("保存脚本失败", e);
            Toast.makeText(this, getString(R.string.script_save_failure, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddScriptDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_script, null);
        EditText editName = dialogView.findViewById(R.id.edit_script_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_script_description);
        EditText editCommands = dialogView.findViewById(R.id.edit_script_commands);
        EditText editCategory = dialogView.findViewById(R.id.edit_script_category);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_add))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.general_save), (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String description = editDescription.getText().toString().trim();
                    String commands = editCommands.getText().toString().trim();
                    String category = editCategory.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(commands)) {
                        Toast.makeText(this, getString(R.string.script_name_and_content_cant_be_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Script script = new Script(name, description, commands, category);
                    scripts.add(script);
                    saveScripts();
                    adapter.notifyDataSetChanged();
                    logger.info("添加脚本: " + name);
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_script, null);
        EditText editContent = dialogView.findViewById(R.id.edit_import_content);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.import_script))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.general_import), (dialog, which) -> {
                    String content = editContent.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        Toast.makeText(this, getString(R.string.error_content_cannot_be_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject obj = new JSONObject(content);
                        Script script = new Script(
                                obj.getString(NAME),
                                obj.optString(DESCRIPTION, getString(R.string.script_no_description)),
                                obj.getString(COMMAND),
                                obj.optString(CATEGORY, getString(R.string.general_default))
                        );
                        scripts.add(script);
                        saveScripts();
                        adapter.notifyDataSetChanged();
                        logger.info("导入脚本: " + script.name);
                        Toast.makeText(this, getString(R.string.script_import_succeed), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        logger.error("导入脚本失败", e);
                        Toast.makeText(this, getString(R.string.script_import_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    private String readFile(File file) throws IOException {
        return IOManager.readFile(file.getAbsolutePath());
    }

    private void writeFile(File file, String content) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
            }
        }
        IOManager.writeFile(file.getAbsolutePath(), content);
    }

    private static class Script {
        String name;
        String description;
        String command;
        String category;

        Script(String name, String description, String command, String category) {
            this.name = name;
            this.description = description;
            this.command = command;
            this.category = category;
        }
    }

    private class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_script, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Script script = scripts.get(position);
            holder.bind(script);
        }

        @Override
        public int getItemCount() {
            return scripts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName;
            TextView textDescription;
            TextView textCategory;
            TextView textCommands;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_script_name);
                textDescription = itemView.findViewById(R.id.text_script_description);
                textCategory = itemView.findViewById(R.id.text_script_category);
                textCommands = itemView.findViewById(R.id.text_script_commands);
            }

            void bind(Script script) {
                textName.setText(script.name);
                textDescription.setText(TextUtils.isEmpty(script.description) ? getString(R.string.script_no_description) : script.description);
                textCategory.setText(script.category);
                textCommands.setText(script.command);
                itemView.setOnClickListener(v -> showScriptOptions(script));
            }
        }
    }

    private void showScriptOptions(Script script) {
        String[] options = {getString(R.string.script_execute), getString(R.string.script_edit),
                getString(R.string.script_export), getString(R.string.script_delete)};

        new AlertDialog.Builder(this)
                .setTitle(script.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            executeScript(script);
                            break;
                        case 1:
                            editScript(script);
                            break;
                        case 2:
                            exportScript(script);
                            break;
                        case 3:
                            deleteScript(script);
                            break;
                    }
                })
                .show();
    }

    private void executeScript(Script script) {
        logger.info("执行脚本: " + script.name);

        new Thread(() -> {
            StringBuilder allOutput = new StringBuilder();
            StringBuilder allErrors = new StringBuilder();
            String cmd = script.command;
            boolean succeed = false;
            try {
                if (!TextUtils.isEmpty(cmd.trim())) {
                    String trimmedCmd = cmd.trim();
                    logger.info("执行命令: " + trimmedCmd);

                    SocketCommandExecutor.ExecutionResult result =
                        StreamClient.executeSocketWithOutput(trimmedCmd);

                    allOutput.append(getString(R.string.script_result_info_command, trimmedCmd)).append("\n");
                    if (result.success) {
                        allOutput.append(result.output).append("\n");
                        succeed = true;
                    } else {
                        allOutput.append(getString(R.string.script_result_info_error, result.error)).append("\n");
                        allErrors.append(getString(R.string.script_result_info_execution_failed)).append("\n");
                    }
                    allOutput.append("\n");
                }
                String finalOutput = allOutput.toString();
                boolean finalSucceed = succeed;
                runOnUiThread(() -> {
                    if (finalSucceed) {
                        Toast.makeText(this, getString(R.string.script_result_toast_execution_succeed, script.name), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.script_result_toast_execution_failure, script.name) , Toast.LENGTH_SHORT).show();
                    }
                    
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.script_result_info_result_of, script.name))
                            .setMessage(finalOutput)
                            .setPositiveButton(getString(R.string.general_confirm), null)
                            .show();
                });
            } catch (Exception e) {
                logger.error("执行脚本失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.script_result_toast_execution_unexpected_exception,
                            script.name, e.getMessage()), Toast.LENGTH_SHORT).show();
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.script_result_info_unexpected_exception, script.name))
                            .setMessage(getString(R.string.script_result_info_exception_info, Log.getStackTraceString(e)))
                            .setPositiveButton(getString(R.string.general_confirm), null)
                            .show();
                });
            }
        }).start();
    }

    private void editScript(Script script) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_script, null);
        EditText editName = dialogView.findViewById(R.id.edit_script_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_script_description);
        EditText editCommands = dialogView.findViewById(R.id.edit_script_commands);
        EditText editCategory = dialogView.findViewById(R.id.edit_script_category);

        editName.setText(script.name);
        editDescription.setText(script.description);
        editCommands.setText(script.command);
        editCategory.setText(script.category);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_edit))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.general_save), (dialog, which) -> {
                    script.name = editName.getText().toString().trim();
                    script.description = editDescription.getText().toString().trim();
                    script.command = editCommands.getText().toString().trim();
                    script.category = editCategory.getText().toString().trim();
                    saveScripts();
                    adapter.notifyDataSetChanged();
                    logger.info("编辑脚本: " + script.name);
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    private void exportScript(Script script) {
        try {
            JSONObject obj = new JSONObject();
            obj.put(NAME, script.name);
            obj.put(DESCRIPTION, script.description);
            obj.put(COMMAND, script.command);
            obj.put(CATEGORY, script.category);

            String json = obj.toString(2);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.script_export))
                    .setMessage(json)
                    .setPositiveButton(getString(R.string.general_copy), (dialog, which) -> {
                        ClipboardManager clipboard =
                                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("script", json);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.general_close), null)
                    .show();
        } catch (JSONException e) {
            logger.error("导出脚本失败", e);
            Toast.makeText(this, getString(R.string.script_export_toast_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteScript(Script script) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_confirm_deletion))
                .setMessage(getString(R.string.script_confirm_deletion_message, script.name))
                .setPositiveButton(R.string.general_delete, (dialog, which) -> {
                    scripts.remove(script);
                    saveScripts();
                    adapter.notifyDataSetChanged();
                    logger.info("删除脚本: " + script.name);
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }
}
