package com.justnothing.testmodule.ui.activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.justnothing.methodsclient.model.ColoredSegment;
import com.justnothing.methodsclient.executor.SocketCommandExecutor;
import com.justnothing.testmodule.R;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.databinding.ActivityScriptManagerBinding;
import com.justnothing.testmodule.databinding.DialogAddScriptBinding;
import com.justnothing.testmodule.databinding.DialogImportScriptBinding;
import com.justnothing.testmodule.databinding.DialogScriptResultBinding;
import com.justnothing.testmodule.databinding.ItemScriptBinding;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptManagerActivity extends BaseActivity {

    private ActivityScriptManagerBinding binding;
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String COMMAND = "commands";
    private static final String CATEGORY = "category";
    private List<Script> scripts;
    private ScriptAdapter adapter;
    private File scriptsFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScriptManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        scriptsFile = new File(getFilesDir(), FileDirectory.SCRIPTS_FILE_NAME);
        initViews();
        loadScripts();
    }

    private void initViews() {
        binding.scriptList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScriptAdapter();
        binding.scriptList.setAdapter(adapter);

        binding.btnAddScript.setOnClickListener(v -> showAddScriptDialog());
        binding.btnImport.setOnClickListener(v -> showImportDialog());
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
                if (!scripts.isEmpty()) {
                    adapter.notifyItemRangeInserted(0, scripts.size());
                }
            } catch (Exception e) {
                logger.error("加载脚本失败", e);
            }
        } else {
            // 添加三个默认脚本
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
            adapter.notifyItemRangeInserted(0, scripts.size());
        }
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
            showToast(getString(R.string.script_save_failure, e.getMessage()));
        }
    }

    private void showAddScriptDialog() {
        DialogAddScriptBinding dialogBinding = DialogAddScriptBinding.inflate(getLayoutInflater());

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_add))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(getString(R.string.general_save), (dialog, which) -> {
                    String name = dialogBinding.editScriptName.getText().toString().trim();
                    String description = dialogBinding.editScriptDescription.getText().toString().trim();
                    String commands = dialogBinding.editScriptCommands.getText().toString().trim();
                    String category = dialogBinding.editScriptCategory.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(commands)) {
                        showToast(getString(R.string.script_name_and_content_cant_be_empty));
                        return;
                    }

                    Script script = new Script(name, description, commands, category);
                    int newPosition = scripts.size(); // 新位置
                    scripts.add(script);
                    saveScripts();
                    adapter.notifyItemInserted(newPosition);
                    binding.scriptList.smoothScrollToPosition(newPosition); // 滚动到新项
                    logger.info("添加脚本: " + name);
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }

    private void showImportDialog() {
        DialogImportScriptBinding dialogBinding = DialogImportScriptBinding.inflate(getLayoutInflater());

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.import_script))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(getString(R.string.general_import), (dialog, which) -> {
                    String content = dialogBinding.editImportContent.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        showToast(getString(R.string.error_content_cannot_be_empty));
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
                        int newPosition = scripts.size();
                        scripts.add(script);
                        saveScripts();
                        adapter.notifyItemInserted(newPosition);
                        binding.scriptList.smoothScrollToPosition(newPosition);
                        logger.info("导入脚本: " + script.name);
                        showToast(getString(R.string.script_import_succeed));
                    } catch (JSONException e) {
                        logger.error("导入脚本失败", e);
                        showToast(getString(R.string.script_import_failed, e.getMessage()));
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
            if (!IOManager.createDirectory(parentDir)) {
                throw new IOException(getString(R.string.script_create_directory_failed_format, parentDir.getAbsolutePath()));
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
            ItemScriptBinding itemBinding = ItemScriptBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Script script = scripts.get(position);
            holder.bind(script, position);
        }

        @Override
        public int getItemCount() {
            return scripts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemScriptBinding binding;

            ViewHolder(ItemScriptBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Script script, int position) {
                binding.textScriptName.setText(script.name);
                binding.textScriptDescription.setText(TextUtils.isEmpty(script.description) ? getString(R.string.script_no_description) : script.description);
                binding.textScriptCategory.setText(script.category);
                binding.textScriptCommands.setText(script.command);
                itemView.setOnClickListener(v -> showScriptOptions(script, position));
            }
        }
    }

    private void showScriptOptions(Script script, int position) {
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
                            editScript(script, position);
                            break;
                        case 2:
                            exportScript(script);
                            break;
                        case 3:
                            deleteScript(script, position);
                            break;
                    }
                })
                .show();
    }

    private void executeScript(Script script) {
        logger.info("执行脚本: " + script.name);

        ThreadPoolManager.submitFastRunnable(() -> {
            String cmd = script.command;
            boolean succeed = false;
            SpannableStringBuilder coloredOutput = new SpannableStringBuilder();

            try {
                if (!TextUtils.isEmpty(cmd.trim())) {
                    String trimmedCmd = cmd.trim();
                    logger.info("执行命令: " + trimmedCmd);

                    coloredOutput.append(getString(R.string.script_result_info_command, trimmedCmd)).append("\n");

                    SocketCommandExecutor socketExecutor = new SocketCommandExecutor();
                    SocketCommandExecutor.ColoredExecutionResult result =
                            socketExecutor.executeWithResult(trimmedCmd, false);

                    runOnUiThread(() -> showToast(getString(R.string.script_execute_finished)));

                    if (result.success()) {
                        SpannableString resultSpan = buildColoredSpan(result.segments());
                        coloredOutput.append(resultSpan);
                        coloredOutput.append("\n");
                        succeed = true;
                    } else {
                        coloredOutput.append(getString(R.string.script_result_info_error, result.error())).append("\n");
                    }
                    coloredOutput.append("\n");
                }

                boolean finalSucceed = succeed;
                runOnUiThread(() -> {
                    if (finalSucceed) {
                        showToast(getString(R.string.script_result_toast_execution_succeed, script.name));
                    } else {
                        showToast(getString(R.string.script_result_toast_execution_failure, script.name));
                    }

                    DialogScriptResultBinding dialogBinding = DialogScriptResultBinding.inflate(getLayoutInflater());

                    dialogBinding.textTitle.setText(getString(R.string.script_result_info_result_of, script.name));
                    dialogBinding.textResult.setText(coloredOutput);

                    new AlertDialog.Builder(ScriptManagerActivity.this)
                            .setView(dialogBinding.getRoot())
                            .setPositiveButton(getString(R.string.general_confirm), null)
                            .show();
                });
            } catch (Exception e) {
                logger.error("执行脚本失败", e);
                runOnUiThread(() -> {
                    showToast(getString(R.string.script_result_toast_execution_unexpected_exception,
                            script.name, e.getMessage()));
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.script_result_info_unexpected_exception, script.name))
                            .setMessage(getString(R.string.script_result_info_exception_info, Log.getStackTraceString(e)))
                            .setPositiveButton(getString(R.string.general_confirm), null)
                            .show();
                });
            }
        });
    }

    private SpannableString buildColoredSpan(java.util.List<ColoredSegment> segments) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (ColoredSegment segment : segments) {
            int start = builder.length();
            builder.append(segment.text());
            int end = builder.length();

            int color = getColorFromCode(segment.color());
            if (color != 0) {
                builder.setSpan(
                        new ForegroundColorSpan(color),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        return new SpannableString(builder);
    }

    private int getColorFromCode(byte colorCode) {
        return switch (colorCode) {
            case Colors.BLACK -> Color.parseColor("#000000");
            case Colors.RED -> Color.parseColor("#F44336");
            case Colors.GREEN -> Color.parseColor("#4CAF50");
            case Colors.YELLOW -> Color.parseColor("#FFEB3B");
            case Colors.BLUE -> Color.parseColor("#2196F3");
            case Colors.CYAN -> Color.parseColor("#00BCD4");
            case Colors.MAGENTA -> Color.parseColor("#E91E63");
            case Colors.WHITE -> Color.parseColor("#FFFFFF");
            case Colors.GRAY -> Color.parseColor("#9E9E9E");
            case Colors.LIGHT_GRAY -> Color.parseColor("#BDBDBD");
            case Colors.LIGHT_RED -> Color.parseColor("#FFCDD2");
            case Colors.LIGHT_GREEN -> Color.parseColor("#C8E6C9");
            case Colors.LIGHT_YELLOW -> Color.parseColor("#FFF9C4");
            case Colors.LIGHT_BLUE -> Color.parseColor("#BBDEFB");
            case Colors.LIGHT_CYAN -> Color.parseColor("#B2EBF2");
            case Colors.LIGHT_MAGENTA -> Color.parseColor("#F8BBD9");
            case Colors.DARK_GRAY -> Color.parseColor("#616161");
            case Colors.ORANGE -> Color.parseColor("#FF9800");
            case Colors.PINK -> Color.parseColor("#FF1EA3");
            case Colors.BROWN -> Color.parseColor("#795548");
            case Colors.GOLD -> Color.parseColor("#FFD700");
            case Colors.SILVER -> Color.parseColor("#C0C0C0");
            case Colors.LIME -> Color.parseColor("#CDDC39");
            case Colors.TEAL -> Color.parseColor("#009688");
            case Colors.NAVY -> Color.parseColor("#0D47A1");
            case Colors.MAROON -> Color.parseColor("#800000");
            case Colors.OLIVE -> Color.parseColor("#808000");
            case Colors.AQUA -> Color.parseColor("#00FFFF");
            case Colors.CORAL -> Color.parseColor("#FF7F50");
            case Colors.SALMON -> Color.parseColor("#FA8072");
            case Colors.INDIGO -> Color.parseColor("#3F51B5");
            case Colors.VIOLET -> Color.parseColor("#9C27B0");
            default -> 0;
        };
    }

    private void editScript(Script script, int position) {
        DialogAddScriptBinding dialogBinding = DialogAddScriptBinding.inflate(getLayoutInflater());

        dialogBinding.editScriptName.setText(script.name);
        dialogBinding.editScriptDescription.setText(script.description);
        dialogBinding.editScriptCommands.setText(script.command);
        dialogBinding.editScriptCategory.setText(script.category);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_edit))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(getString(R.string.general_save), (dialog, which) -> {
                    script.name = dialogBinding.editScriptName.getText().toString().trim();
                    script.description = dialogBinding.editScriptDescription.getText().toString().trim();
                    script.command = dialogBinding.editScriptCommands.getText().toString().trim();
                    script.category = dialogBinding.editScriptCategory.getText().toString().trim();
                    saveScripts();
                    adapter.notifyItemChanged(position);
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
                        showToast(getString(R.string.toast_copied_to_clipboard));
                    })
                    .setNegativeButton(getString(R.string.general_close), null)
                    .show();
        } catch (JSONException e) {
            logger.error("导出脚本失败", e);
            showToast(getString(R.string.script_export_toast_failed));
        }
    }

    private void deleteScript(Script script, int position) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.script_confirm_deletion))
                .setMessage(getString(R.string.script_confirm_deletion_message, script.name))
                .setPositiveButton(R.string.general_delete, (dialog, which) -> {
                    scripts.remove(position);
                    saveScripts();
                    adapter.notifyItemRemoved(position);
                    logger.info("删除脚本: " + script.name);
                })
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show();
    }
}
