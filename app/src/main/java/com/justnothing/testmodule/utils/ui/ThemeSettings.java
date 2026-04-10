package com.justnothing.testmodule.utils.ui;

import androidx.appcompat.app.AppCompatDelegate;
import android.content.Context;
import android.content.SharedPreferences;

import com.justnothing.testmodule.utils.functions.Logger;

public class ThemeSettings extends Logger {
    private static final String TAG = "ThemeSettings";
    private static final String PREF_NAME = "theme_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    private static ThemeSettings instance;
    private final SharedPreferences prefs;
    private int currentThemeMode;
    
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;
    public static final int MODE_AUTO = 3;
    
    private ThemeSettings(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentThemeMode = prefs.getInt(KEY_THEME_MODE, MODE_AUTO);
        debug("ThemeSettings初始化，当前主题模式: " + getThemeModeName(currentThemeMode));
    }
    
    public static synchronized ThemeSettings getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeSettings(context.getApplicationContext());
        }
        return instance;
    }
    
    @Override
    public String getTag() {
        return TAG;
    }
    
    public int getThemeMode() {
        return currentThemeMode;
    }
    
    public void setThemeMode(int mode) {
        this.currentThemeMode = mode;
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        debug("设置主题模式: " + getThemeModeName(mode));
    }
    
    public void applyTheme() {
        int appCompatMode = switch (currentThemeMode) {
            case MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
        AppCompatDelegate.setDefaultNightMode(appCompatMode);
        debug("应用主题模式: " + getThemeModeName(currentThemeMode));
    }


    public String getThemeModeName(int mode) {
        // TODO: i18n
        return switch (mode) {
            case MODE_LIGHT -> "浅色模式";
            case MODE_DARK -> "深色模式";
            case MODE_AUTO -> "跟随系统";
            default -> "未知";
        };
    }
    
    public void resetToDefault() {
        setThemeMode(MODE_AUTO);
    }
}
