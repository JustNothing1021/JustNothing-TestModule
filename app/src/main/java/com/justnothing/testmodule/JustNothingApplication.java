package com.justnothing.testmodule;

import android.app.Application;

import com.justnothing.testmodule.utils.ui.ThemeSettings;

/**
 * 进程入口。目前只做一件事：把用户存下来的主题设置应用上。
 *
 * 为什么必须放在这里：
 * {@code AppCompatDelegate.setDefaultNightMode()} 决定的是「整个进程」渲染成浅色还是深色，
 * 而它只有两个生效时机 —— {@code Application.onCreate()}，或者 Activity 的
 * {@code super.onCreate()} 之前。
 *
 * 之前只有 SettingsActivity 在用户点设置项的那一刻调了一次，结果就是：
 *   点了设置 → 当前进程变过去
 *   重启 App → 没人再调，退回「跟随系统」，用户存的选择被静默忽略
 * 放在这里之后，进程一起来就应用，和用户这次有没有点过设置无关。
 */
public class JustNothingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeSettings.getInstance(this).applyTheme();
    }
}
