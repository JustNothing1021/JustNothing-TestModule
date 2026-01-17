package com.justnothing.testmodule.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.justnothing.testmodule.R;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.ui.ThemeSettings;
import com.justnothing.testmodule.utils.ui.UISettings;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class SettingsActivity extends AppCompatActivity {

    private final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "SettingsActivity";
        }
    };

    private UISettings uiSettings;
    private ThemeSettings themeSettings;
    private SeekBar scaleSeekBar;
    private TextView scaleValueText;
    private TextView defaultLabel;
    private Button btnApply;
    private Button btnReset;
    private RadioGroup themeRadioGroup;
    private RadioButton radioThemeLight;
    private RadioButton radioThemeDark;
    private RadioButton radioThemeSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        uiSettings = UISettings.getInstance(this);
        themeSettings = ThemeSettings.getInstance(this);
        logger.info("设置界面启动");

        setupViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void setupViews() {
        scaleSeekBar = findViewById(R.id.seekbar_ui_scale);
        scaleValueText = findViewById(R.id.text_scale_value);
        defaultLabel = findViewById(R.id.text_default_label);
        btnApply = findViewById(R.id.btn_apply_scale);
        btnReset = findViewById(R.id.btn_reset_scale);
        themeRadioGroup = findViewById(R.id.radiogroup_theme);
        radioThemeLight = findViewById(R.id.radio_theme_light);
        radioThemeDark = findViewById(R.id.radio_theme_dark);
        radioThemeSystem = findViewById(R.id.radio_theme_system);
    }

    private void loadCurrentSettings() {
        float currentScale = uiSettings.getUIScale();
        int progress = (int) ((currentScale - uiSettings.getMinScale()) / 
                             (uiSettings.getMaxScale() - uiSettings.getMinScale()) * 100);
        scaleSeekBar.setProgress(progress);
        updateScaleText(currentScale);
        updateDefaultLabelPosition();
        
        int currentThemeMode = themeSettings.getThemeMode();
        switch (currentThemeMode) {
            case ThemeSettings.MODE_LIGHT:
                radioThemeLight.setChecked(true);
                break;
            case ThemeSettings.MODE_DARK:
                radioThemeDark.setChecked(true);
                break;
            case ThemeSettings.MODE_AUTO:
                radioThemeSystem.setChecked(true);
                break;
        }
    }

    private void setupListeners() {
        scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = uiSettings.getMinScale() + 
                             (progress / 100.0f) * (uiSettings.getMaxScale() - uiSettings.getMinScale());
                updateScaleText(scale);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newThemeMode;
            if (checkedId == R.id.radio_theme_light) {
                newThemeMode = ThemeSettings.MODE_LIGHT;
            } else if (checkedId == R.id.radio_theme_dark) {
                newThemeMode = ThemeSettings.MODE_DARK;
            } else {
                newThemeMode = ThemeSettings.MODE_AUTO;
            }
            themeSettings.setThemeMode(newThemeMode);
            themeSettings.applyTheme(SettingsActivity.this);
            logger.info("应用主题: " + themeSettings.getThemeModeName(newThemeMode));
            recreate();
        });

        btnApply.setOnClickListener(v -> {
            int progress = scaleSeekBar.getProgress();
            float scale = uiSettings.getMinScale() + 
                         (progress / 100.0f) * (uiSettings.getMaxScale() - uiSettings.getMinScale());
            uiSettings.setUIScale(scale);
            uiSettings.applyUIScale(SettingsActivity.this);
            logger.info("应用界面缩放: " + scale);
            finish();
        });

        btnReset.setOnClickListener(v -> {
            uiSettings.resetToDefault();
            themeSettings.resetToDefault();
            loadCurrentSettings();
            uiSettings.applyUIScale(SettingsActivity.this);
            themeSettings.applyTheme(SettingsActivity.this);
            logger.info("重置界面缩放和主题为默认值");
            recreate();
        });
    }

    private void updateDefaultLabelPosition() {
        // 强迫症有福了
        float defaultScale = uiSettings.getDefaultScale();
        float minScale = uiSettings.getMinScale();
        float maxScale = uiSettings.getMaxScale();
        
        // 将比例值映射到 SeekBar 的进度范围 (0-100)
        float defaultPosition = (defaultScale - minScale) / (maxScale - minScale);
        
        // 获取 SeekBar 的宽度，而不是屏幕宽度
        int seekBarWidth = scaleSeekBar.getWidth();
        
        // 如果 SeekBar 还没有测量完成，延迟计算
        if (seekBarWidth <= 0) {
            scaleSeekBar.post(() -> updateDefaultLabelPosition());
            return;
        }
        
        AtomicInteger labelWidth = new AtomicInteger(defaultLabel.getWidth());
        
        // 计算 SeekBar 的可用宽度（减去左右边距）
        int availableWidth = seekBarWidth - scaleSeekBar.getPaddingLeft() - scaleSeekBar.getPaddingRight();
        
        // 计算默认标识的位置（考虑 SeekBar 的 padding）
        // 注意：这里不需要乘以100，因为 defaultPosition 已经是0-1的比例
        AtomicInteger margin = new AtomicInteger((int) (availableWidth * defaultPosition) + scaleSeekBar.getPaddingLeft());
        
        // 调试信息
        logger.info("默认比例: " + defaultScale + ", 位置比例: " + defaultPosition + 
                   ", SeekBar宽度: " + seekBarWidth + ", 可用宽度: " + availableWidth + 
                   ", 左边距: " + margin);
        
        if (labelWidth.get() > 0) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) defaultLabel.getLayoutParams();
            params.leftMargin = margin.get() - labelWidth.get() / 2;
            defaultLabel.setLayoutParams(params);
        } else {
            defaultLabel.post(() -> {
                labelWidth.set(defaultLabel.getWidth());
                margin.set((int) (availableWidth * defaultPosition) + scaleSeekBar.getPaddingLeft());
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) defaultLabel.getLayoutParams();
                params.leftMargin = margin.get() - labelWidth.get() / 2;
                defaultLabel.setLayoutParams(params);
                
                logger.info("延迟计算 - 标签宽度: " + labelWidth.get() + ", 最终左边距: " + params.leftMargin);
            });
        }
    }

    private void updateScaleText(float scale) {
        scaleValueText.setText(String.format(Locale.getDefault(), "%.1fx", scale));
    }
}
