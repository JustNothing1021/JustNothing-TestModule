package com.justnothing.testmodule.command.functions.exportcontext;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.xtchttplib.ContextManager;
import com.xtc.sync.elt;
import com.xtc.sync.byw;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Locale;

public class ExportContextMain extends CommandBase {

    public ExportContextMain() {
        super("ExportContextExecutor");
    }

    private static final String CONTENT_URI = "content://com.xtc.initservice/item";
    private static final String WATCH_ID_URI = "content://com.xtc.provider/BaseDataProvider/watchId/1";

    @Override
    public String getHelpText() {
        return """
                语法: export-context
                
                导出设备上下文信息为JSON格式，包括：
                - HTTP配置信息 (从ContentProvider读取)
                - 设备信息 (从系统属性读取)
                
                示例:
                    export-context
                
                (Submodule export-context)
                """;
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        logger.debug("执行export-context命令");
        
        Context appContext = getApplicationContext();
        if (appContext == null) {
            logger.error("无法获取应用上下文");
            return "错误: 无法获取应用上下文";
        }

        try {
            ContextManager ctx = new ContextManager();
            
            logger.info("开始收集设备上下文信息");
            
            Cursor cursor = null;
            try {
                ContentResolver resolver = appContext.getContentResolver();
                Uri uri = Uri.parse(CONTENT_URI);
                
                cursor = resolver.query(uri, null, null, null, null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    ctx.setGrey(getCursorStringValue(cursor, "grey"));
                    ctx.setTs(getCursorIntValue(cursor, "ts"));
                    ctx.setAe(getCursorStringValue(cursor, "ae"));
                    ctx.setRsaPublicKey(getCursorStringValue(cursor, "rsaPublicKey"));
                    ctx.setSelfRsaPublicKey(getCursorStringValue(cursor, "selfRsaPublicKey"));
                    ctx.setHttpHeadParam(getCursorStringValue(cursor, "httpHeadParam"));
                    ctx.setEncSwitch(getCursorStringValue(cursor, "encSwitch"));
                    
                    logger.debug("HTTP配置信息收集完成");
                } else {
                    logger.warn("无法从ContentProvider读取HTTP配置");
                }
            } catch (Exception e) {
                logger.error("收集HTTP配置信息失败", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            try {
                ContentResolver resolver = appContext.getContentResolver();
                Uri uri = Uri.parse(WATCH_ID_URI);
                String watchId = resolver.getType(uri);
                if (!TextUtils.isEmpty(watchId)) {
                    ctx.setWatchId(watchId);
                    logger.debug("watchId: " + watchId);
                }
            } catch (Exception e) {
                logger.error("收集watchId失败", e);
            }
            
            ctx.setMacAddr(getMacAddress());
            ctx.setBindNumber(getSystemProperty("ro.boot.bindnumber"));
            ctx.setInnerModel(getSystemProperty("ro.product.innermodel", "IB"));
            ctx.setServerInner(getSystemProperty("persist.sys.serverinner"));
            ctx.setLocale(getSystemProperty("ro.product.locale"));
            ctx.setRegion(getSystemProperty("ro.product.locale.region"));
            ctx.setLanguage(Locale.getDefault().getLanguage());
            ctx.setInnerModelEx(getSystemProperty("ro.product.innermodel.ex"));
            ctx.setWatchModel(getSystemProperty("ro.product.model", "Z3"));
            ctx.setWatchPriModel(getSystemProperty("ro.product.pri.model"));
            String buildType = getSystemProperty("ro.build.type", "user");
            if ("userdebug".equals(buildType)) {
                buildType = "user";
            }
            ctx.setBuildType(buildType);
            ctx.setHardware(getSystemProperty("ro.hardware", "qcom"));
            ctx.setCaremeOsVersion(getSystemProperty("ro.product.careme.version"));
            ctx.setShowModel(getSystemProperty("ro.product.showmodel"));
            ctx.setTimeZone(elt.a());
            ctx.setDataCenterCode(new byw().mo2771a());
            ctx.setChipId(getSystemProperty("ro.boot.xtc.chipid"));
            ctx.setBuildRelease(getSystemProperty("ro.build.version.release"));
            ctx.setSoftVersion(getSystemProperty("ro.product.current.softversion"));
            ctx.setPackageName(null);
            ctx.setPackageVersionCode(null);
            ctx.setPackageVersionName(null);
            ctx.setAndroidSdk(Build.VERSION.SDK_INT);
            
            String json = ctx.toJson();
            
            logger.info("成功导出设备上下文信息");
            logger.debug("导出的JSON:\n" + json);
            
            return json;
            
        } catch (Exception e) {
            logger.error("导出设备上下文信息失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private Context getApplicationContext() {
        try {
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
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            return (String) get.invoke(c, key, defaultValue);
        } catch (Exception e) {
            logger.error("读取系统属性失败: " + key, e);
            return defaultValue;
        }
    }

    private String getCursorStringValue(Cursor cursor, String columnName) {
        if (cursor == null || TextUtils.isEmpty(columnName)) {
            return null;
        }

        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) {
                return null;
            }
            return cursor.getString(columnIndex);
        } catch (Exception e) {
            logger.error("getCursorStringValue error: " + columnName, e);
            return null;
        }
    }

    private int getCursorIntValue(Cursor cursor, String columnName) {
        if (cursor == null || TextUtils.isEmpty(columnName)) {
            return 0;
        }

        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex < 0) {
                return 0;
            }
            return cursor.getInt(columnIndex);
        } catch (Exception e) {
            logger.error("getCursorIntValue error: " + columnName, e);
            return 0;
        }
    }

    private String getMacAddress() {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("eth0");
            }
            if (networkInterface != null) {
                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < macBytes.length; i++) {
                        sb.append(String.format("%02x", macBytes[i]));
                        if (i < macBytes.length - 1) {
                            sb.append(":");
                        }
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
