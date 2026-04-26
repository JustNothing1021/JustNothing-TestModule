package com.justnothing.javainterpreter.builtins;

import java.util.List;
import java.util.Random;

public class JustNothing {
    private static final Random random = new Random();
    private int stability;
    private boolean exploded;
    public static final String DESCRIPTION = "这个引擎的开发者!";
    public static final List<String> REAL_NAMES = List.of("JustNothing1021", "JustNothing", "真的啥也不是啊");
    public JustNothing(int stability) {
        this.stability = stability;
    }

    public JustNothing() {
        this(114514);
    }

    public int getStability() {
        return stability;
    }
    public void reportError() {
        int next = random.nextInt(114514);
        System.out.println("你向JustNothing1021报告了一个神秘的bug!");
        if (next > 100000) {
            String[] results = {
                "JustNothing1021 被自己的石山代码气笑了!",
                "JustNothing1021 debug了几个小时发现是一个地方大于号打成大于等于了!",
                "JustNothing1021 改完代码之后发现应用装设备上之后设备卡在开机界面了!",
                "JustNothing1021 改完代码装设备上调试的时候才注意到java.lang.ClassNotFoundException和de.robv.android.xposed.XposedHelpers$ClassNotFoundError不是一个类!"
            };

            System.out.println(results[random.nextInt(results.length)]);
            int val = random.nextInt(10000);
            System.out.println("** JustNothing的稳定值下降了 " + val + " !");
            stability -= val;
        }
        checkStability();
    }

    private void checkStability() {
        if (stability <= 0) {
            System.out.println("** JustNothing1021 爆炸了! " + (exploded ? "" : "再一次!") + "**");
            exploded = true;
        }
    }
}
