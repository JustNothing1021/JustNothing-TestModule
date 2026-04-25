package com.justnothing.testmodule.command.functions.exportcontext;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.model.ContextFieldInfo;
import com.justnothing.testmodule.protocol.json.request.ExportContextRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.ExportContextResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.xtchttplib.ContextManager;
import com.xtc.sync.elt;
import com.xtc.sync.byw;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportContextRequestHandler implements RequestHandler<ExportContextRequest, ExportContextResult> {

    private static final Logger logger = Logger.getLoggerForName("ExportCtxHandler");

    private static final String CONTENT_URI = "content://com.xtc.initservice/item";
    private static final String WATCH_ID_URI = "content://com.xtc.provider/BaseDataProvider/watchId/1";

    @Override
    public String getCommandType() {
        return "ExportContext";
    }

    @Override
    public ExportContextRequest parseRequest(JSONObject obj) {
        return new ExportContextRequest().fromJson(obj);
    }

    @Override
    public ExportContextResult createResult(String requestId) {
        return new ExportContextResult(requestId);
    }

    @Override
    public ExportContextResult handle(ExportContextRequest request) {
        logger.debug("处理导出上下文请求");

        ExportContextResult result = new ExportContextResult(request.getRequestId());

        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                result.setError(new CommandResult.ErrorInfo("NO_CONTEXT", "无法获取应用上下文"));
                return result;
            }

            List<ContextFieldInfo> fields = new ArrayList<>();
            ContextManager ctx = new ContextManager();

            collectHttpConfig(appContext, ctx, fields);
            collectWatchId(appContext, ctx, fields);
            collectSystemProperties(ctx, fields);

            result.setFields(fields);
            logger.info("上下文导出成功, 共 " + fields.size() + " 个字段");
        } catch (Exception e) {
            logger.error("导出上下文失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "导出上下文失败: " + e.getMessage()));
        }

        return result;
    }

    private void collectHttpConfig(Context appContext, ContextManager ctx, List<ContextFieldInfo> fields) {
        Cursor cursor = null;
        try {
            ContentResolver resolver = appContext.getContentResolver();
            Uri uri = Uri.parse(CONTENT_URI);
            cursor = resolver.query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                addField(fields, "http_config", "Grey", getCursorStringValue(cursor, "grey"));
                addField(fields, "http_config", "Timestamp", String.valueOf(getCursorIntValue(cursor, "ts")));
                addField(fields, "http_config", "AE", getCursorStringValue(cursor, "ae"));
                addField(fields, "http_config", "RSA Public Key", maskKey(getCursorStringValue(cursor, "rsaPublicKey")));
                addField(fields, "http_config", "Self-signed RSA Key", maskKey(getCursorStringValue(cursor, "selfRsaPublicKey")));
                addField(fields, "http_config", "HTTP Header Params", getCursorStringValue(cursor, "httpHeadParam"));
                addField(fields, "http_config", "Encryption Switch", getCursorStringValue(cursor, "encSwitch"));
                logger.debug("HTTP配置信息收集完成");
            } else {
                logger.warn("无法从ContentProvider读取HTTP配置");
            }
        } catch (Exception e) {
            logger.error("收集HTTP配置信息失败", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void collectWatchId(Context appContext, ContextManager ctx, List<ContextFieldInfo> fields) {
        try {
            ContentResolver resolver = appContext.getContentResolver();
            Uri uri = Uri.parse(WATCH_ID_URI);
            String watchId = resolver.getType(uri);
            if (!TextUtils.isEmpty(watchId)) {
                ctx.setWatchId(watchId);
                addField(fields, "device_identity", "WatchID", watchId);
            }
        } catch (Exception e) {
            logger.error("收集watchId失败", e);
        }
    }

    private void collectSystemProperties(ContextManager ctx, List<ContextFieldInfo> fields) {
        addField(fields, "device_identity", "MAC Address", getMacAddress());
        addField(fields, "device_identity", "Bind Number", getSystemProperty("ro.boot.bindnumber"));
        addField(fields, "device_identity", "Inner Model", getSystemProperty("ro.product.innermodel", "IB"));
        addField(fields, "device_identity", "Server Inner ID", getSystemProperty("persist.sys.serverinner"));
        addField(fields, "device_identity", "ChipID", getSystemProperty("ro.boot.xtc.chipid"));

        addField(fields, "device_info", "Watch Model", getSystemProperty("ro.product.model", "Z3"));
        addField(fields, "device_info", "Show Model", getSystemProperty("ro.product.showmodel"));
        addField(fields, "device_info", "Primary Model", getSystemProperty("ro.product.pri.model"));
        addField(fields, "device_info", "Extended Model", getSystemProperty("ro.product.innermodel.ex"));
        addField(fields, "device_info", "Hardware Platform", getSystemProperty("ro.hardware", "qcom"));

        String buildType = getSystemProperty("ro.build.type", "user");
        if ("userdebug".equals(buildType)) buildType = "user";
        addField(fields, "system_info", "Build Type", buildType);
        addField(fields, "system_info", "Android Version", Build.VERSION.RELEASE);
        addField(fields, "system_info", "Android SDK", String.valueOf(Build.VERSION.SDK_INT));
        addField(fields, "system_info", "Build Version", getSystemProperty("ro.build.version.release"));
        addField(fields, "system_info", "Software Version", getSystemProperty("ro.product.current.softversion"));
        addField(fields, "system_info", "CaremeOS Version", getSystemProperty("ro.product.careme.version"));

        addField(fields, "locale_info", "System Locale", getSystemProperty("ro.product.locale"));
        addField(fields, "locale_info", "Region", getSystemProperty("ro.product.locale.region"));
        addField(fields, "locale_info", "Current Language", Locale.getDefault().getLanguage());
        addField(fields, "locale_info", "Timezone", elt.a());

        addField(fields, "network_service", "Data Center Code", new byw().mo2771a());
    }

    private void addField(List<ContextFieldInfo> fields, String category, String label, String value) {
        if (value != null && !value.isEmpty()) {
            fields.add(new ContextFieldInfo(category, label, value));
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 16) return "***";
        return key.substring(0, 8) + "****" + key.substring(key.length() - 8);
    }

    private Context getApplicationContext() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            logger.error("获取Application Context失败", e);
            return null;
        }
    }

    private String getSystemProperty(String key) {
        return getSystemProperty(key, null);
    }

    private String getSystemProperty(String key, String defaultValue) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            return (String) get.invoke(c, key, defaultValue);
        } catch (Exception e) {
            logger.error("读取系统属性失败: " + key, e);
            return defaultValue;
        }
    }

    private String getCursorStringValue(Cursor cursor, String columnName) {
        if (cursor == null || TextUtils.isEmpty(columnName)) return null;
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) return null;
            return cursor.getString(columnIndex);
        } catch (Exception e) {
            logger.error("getCursorStringValue error: " + columnName, e);
            return null;
        }
    }

    private int getCursorIntValue(Cursor cursor, String columnName) {
        if (cursor == null || TextUtils.isEmpty(columnName)) return 0;
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) return 0;
            return cursor.getInt(columnIndex);
        } catch (Exception e) {
            logger.error("getCursorIntValue error: " + columnName, e);
            return 0;
        }
    }

    private String getMacAddress() {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
            if (networkInterface == null) networkInterface = NetworkInterface.getByName("eth0");
            if (networkInterface != null) {
                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < macBytes.length; i++) {
                        sb.append(String.format("%02x", macBytes[i]));
                        if (i < macBytes.length - 1) sb.append(":");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            logger.error("获取MAC地址失败", e);
        }
        return null;
    }
}
