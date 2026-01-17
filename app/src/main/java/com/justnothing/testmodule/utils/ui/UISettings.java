package com.justnothing.testmodule.utils.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.justnothing.testmodule.utils.functions.Logger;

public class UISettings extends Logger {
    private static final String TAG = "UISettings";
    private static final String PREF_NAME = "ui_settings";
    private static final String KEY_UI_SCALE = "ui_scale";
    
    private static final float DEFAULT_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.4f;
    private static final float MAX_SCALE = 2.0f;
    private static final float SCALE_FACTOR = 0.8f;
    
    private static UISettings instance;
    private SharedPreferences prefs;
    private float currentScale;
    
    private UISettings(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentScale = prefs.getFloat(KEY_UI_SCALE, DEFAULT_SCALE);
        debug("UISettings初始化，当前缩放: " + currentScale);
    }
    
    public static synchronized UISettings getInstance(Context context) {
        if (instance == null) {
            instance = new UISettings(context.getApplicationContext());
        }
        return instance;
    }
    
    @Override
    public String getTag() {
        return TAG;
    }
    
    public float getUIScale() {
        return currentScale;
    }
    
    public void setUIScale(float scale) {
        if (scale < MIN_SCALE) scale = MIN_SCALE;
        if (scale > MAX_SCALE) scale = MAX_SCALE;
        
        this.currentScale = scale;
        prefs.edit().putFloat(KEY_UI_SCALE, scale).apply();
        debug("设置UI缩放: " + scale);
    }
    
    public void applyUIScale(Context context) {
        debug("应用UI缩放: " + currentScale);
        
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            android.content.res.Configuration config = new android.content.res.Configuration(activity.getResources().getConfiguration());
            android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            
            float effectiveScale = currentScale * SCALE_FACTOR;
            
            float fontScale;
            if (currentScale >= 1.0f) {
                fontScale = 1.0f + (currentScale - 1.0f) * 0.4f;
            } else {
                fontScale = 1.0f - (1.0f - currentScale) * 0.25f;
            }
            
            config.fontScale = fontScale;
            metrics.density *= effectiveScale;
            metrics.scaledDensity *= effectiveScale;
            
            activity.getResources().updateConfiguration(config, metrics);
            debug("已更新Configuration，UI缩放: " + currentScale + ", 有效缩放: " + effectiveScale + ", 字体缩放: " + fontScale);
        }
    }
    
    public float getScaledDimension(float dimension) {
        return dimension * currentScale;
    }
    
    public float getScaledTextSize(float textSize) {
        return textSize * currentScale;
    }
    
    public float getMinScale() {
        return MIN_SCALE;
    }
    
    public float getMaxScale() {
        return MAX_SCALE;
    }
    
    public float getDefaultScale() {
        return DEFAULT_SCALE;
    }
    
    public void resetToDefault() {
        setUIScale(DEFAULT_SCALE);
    }
}
