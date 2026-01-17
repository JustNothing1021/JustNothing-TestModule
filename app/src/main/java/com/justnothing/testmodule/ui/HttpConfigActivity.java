package com.justnothing.testmodule.ui;

import static com.justnothing.testmodule.constants.FileDirectory.DATA_PATH;
import static com.justnothing.testmodule.utils.functions.Logger.MAIN_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;


import com.justnothing.testmodule.R;
import com.justnothing.testmodule.databinding.ActivityGetHttpconfBinding;
import com.justnothing.testmodule.utils.functions.FileUtils;
import com.justnothing.testmodule.utils.functions.Logger;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

import com.justnothing.testmodule.constants.FileDirectory;


public class HttpConfigActivity extends AppCompatActivity {


    public static class MainLogger extends Logger {
        MainLogger() {
        }

        @Override
        public String getTag() {
            return MAIN_TAG;
        }
    }

    public static MainLogger logger = new MainLogger();

    public static HttpConfigActivity instance;

    private static final String CONTENT_URI = "content://com.xtc.initservice/item";
    private ActivityGetHttpconfBinding binding;


    static {
        DATA_PATH = FileDirectory.SDCARD_PATH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        binding = ActivityGetHttpconfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.button2.setOnClickListener(l -> refreshContent());
        binding.imageButton.setOnClickListener(l -> saveContent());
    }

    @SuppressLint("SetTextI18n")
    public void refreshContent() {
        logger.debug("按钮被点击");

        Cursor cursor = null;
        try {

            Context context = getApplicationContext();
            ContentResolver resolver = context.getContentResolver();
            Uri uri = Uri.parse(CONTENT_URI);
            logger.debug("查询URI: " + uri.toString());

            cursor = resolver.query(
                    uri,
                    null, null, null, null
            );

            if (cursor == null) {
                logger.error("Cursor为null, 查询返回空");
                binding.textView5.setText(getString(R.string.cursor_null_error));
                return;
            }

            logger.debug("Cursor列数: " + cursor.getColumnCount());
            logger.debug("Cursor行数: " + cursor.getCount());

            String[] columnNames = cursor.getColumnNames();
            logger.debug("可用列: " + TextUtils.join(", ", columnNames));

            if (cursor.moveToFirst()) {
                do {
                    String grey = getCursorStringValue(cursor, "grey");
                    String rsaPublicKey = getCursorStringValue(cursor, "rsaPublicKey");
                    String encSwitch = getCursorStringValue(cursor, "encSwitch");
                    String selfRsaPublicKey = getCursorStringValue(cursor, "selfRsaPublicKey");
                    String httpHeadParam = getCursorStringValue(cursor, "httpHeadParam");
                    int ts = getCursorIntValue(cursor, "ts");
                    String ae = getCursorStringValue(cursor, "ae");
                    String tag = getCursorStringValue(cursor, "tag");

                    logger.debug(
                        "成功获取数据 " + "\n" +
                        "grey = " + grey + "\n" +
                        "rsaPublicKey = " + rsaPublicKey + "\n" +
                        "encSwitch = " + encSwitch + "\n" +
                        "selfRsaPublicKey = " + selfRsaPublicKey + "\n" +
                        "httpHeadParam = " + httpHeadParam + "\n" +
                        "tag = " + tag + "\n" +
                        "ts = " + ts + "\n" +
                        "ae = " + ae
                    );

                    binding.textView5.setText(
                        "数据\n\n\n" +
                        "tag\n" +
                        tag + "\n\n" +
                        "grey\n" +
                        grey + "\n\n" +
                        "encSwitch\n" +
                        encSwitch + "\n\n" +
                        "rsaPublicKey\n" +
                        rsaPublicKey + "\n\n" +
                        "selfRsaPublicKey\n"+
                        selfRsaPublicKey + "\n\n" +
                        "httpHeadParam\n" +
                        httpHeadParam + "\n\n" +
                        "ae\n" +
                        ae + "\n\n" +
                        "ts\n" +
                        ts + "\n\n"
                    );

                } while (cursor.moveToNext());
            } else {
                logger.error("Cursor没有数据(moveToFirst返回false)");
                binding.textView5.setText(getString(R.string.cursor_no_data));
            }

        } catch (SecurityException e) {
            logger.error("没有访问ContentProvider的权限", e);
            binding.textView5.setText("没有访问ContentProvider的权限\n\n" + e);
        } catch (IllegalArgumentException e) {
            logger.error("refreshContent函数参数错误: URI可能不正确", e);
            binding.textView5.setText("URI可能不正确\n\n" + e);
        } catch (Throwable e) {
            logger.error("refreshContent函数出现未知错误", e);
            binding.textView5.setText("出现未知错误\n\n" + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void saveContent() {
        if (!FileUtils.checkStoragePermission(this))
            FileUtils.requestPermission(this);
        String text = (String) binding.textView5.getText();
        if (!FileUtils.checkStoragePermission(this)) {
            logger.warn("保存文件时没有权限");
            showToast("没有保存文件的权限");
            return;
        }

        String fileDir = DATA_PATH + File.separator + FileDirectory.CONTENTS_DIR_NAME + File.separator + FileDirectory.CONTENT_FILE_PREFIX +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date()) +
                ".txt";
        if (!FileUtils.saveObjectToFile(text, fileDir)) {
            logger.warn("内容保存失败");
            showToast("保存失败，请查看日志");
        } else {
            logger.info("内容保存成功");
            showToast("文件保存到" + fileDir);
        }
    }

    private static String getCursorStringValue(Cursor cursor, String columnName) {
        if (cursor == null) {
            return null;
        }
        if (TextUtils.isEmpty(columnName)) {
            return null;
        }

        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) {
                return null;
            }
            return cursor.getString(columnIndex);
        } catch (Exception e) {
            Log.e(MAIN_TAG, "getCursorStringValue error: ", e);
            return null;
        }
    }

    private static int getCursorIntValue(Cursor cursor, String columnName) {
        if (cursor == null) {
            return 0;
        }
        if (TextUtils.isEmpty(columnName)) {
            return 0;
        }

        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) {
                return 0;
            }
            return cursor.getInt(columnIndex);
        } catch (Exception e) {
            Log.e(MAIN_TAG, "getCursorIntValue error: ", e);
            return 0;
        }
    }


}