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
    private SharedPreferences prefs;
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
    
    public void applyTheme(Context context) {
        int appCompatMode;
        switch (currentThemeMode) {
            case MODE_LIGHT:
                appCompatMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case MODE_DARK:
                appCompatMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case MODE_AUTO:
            default:
                appCompatMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode);
        debug("应用主题模式: " + getThemeModeName(currentThemeMode));
    }
    
    public boolean isDarkMode() {
        return currentThemeMode == MODE_DARK || 
               (currentThemeMode == MODE_AUTO && isSystemDarkMode());
    }
    
    private boolean isSystemDarkMode() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES;
    }
    
    public String getThemeModeName(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return "浅色模式";
            case MODE_DARK:
                return "深色模式";
            case MODE_AUTO:
                return "跟随系统";
            default:
                return "未知";
        }
    }
    
    public void resetToDefault() {
        setThemeMode(MODE_AUTO);
    }
}
