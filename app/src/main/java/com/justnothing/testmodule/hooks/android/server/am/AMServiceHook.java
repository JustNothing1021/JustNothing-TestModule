package com.justnothing.testmodule.hooks.android.server.am;

import com.justnothing.testmodule.hooks.PackageHook;
import com.justnothing.testmodule.utils.functions.CmdUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodReplacement;

public class AMServiceHook extends PackageHook {

    public final String TAG = "AMServiceHook";
    public final String AMS_CLASSNAME = "com.android.server.am.ActivityManagerService";
    private static final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();

    

    @Override
    protected void hookImplements() {
        setHookDisplayName("活动管理服务拦截");
        setHookDescription("hook掉xseException防止手表卡xsebootloader, 开了可能会变卡, 如果你相信你手表的防护程度的话可以不用开");
        setHookCondition(param -> param.packageName.equals("android"));
        hookMethod(AMS_CLASSNAME, "xseException",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    try {
                        warn("xseException被调用了; 可能是xsesService的某些检测方法没有被成功hook到?");
                        warn("如果你看到了这条日志，请尽快加强手表防护（比如使用某些大佬的更强大的反破解模块）");
                        warn("这样可以有效防止云控问题（虽然这里hook掉了XTCBehaviorUtil但是不知道会不会有未知问题）");
                        warn("将会尝试重置persist.sys.xtc.alxcse为false");
                        asyncExecutor.execute(() -> {
                            try {
                                CmdUtils.CommandOutput res = CmdUtils.runRootCommand("setprop persist.sys.xtc.alxcse false");
                                warn("异步执行结果: " + res);
                            } catch (Exception e) {
                                warn("异步执行命令失败: " + e.getMessage());
                            }
                        });
                        return null;
                    } catch (Exception e) {
                        error("replaceHookedMethod异常: " + e.getMessage(), e);
                        return null;
                    }
                }
            }
        );
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
