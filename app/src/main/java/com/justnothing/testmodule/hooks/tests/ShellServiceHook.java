package com.justnothing.testmodule.hooks.tests;

import de.robv.android.xposed.*;

import android.os.IBinder;
import com.justnothing.testmodule.service.ShellService;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.hooks.PackageHook;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShellServiceHook extends PackageHook {

    public static final String SERVICE_NAME = "justnothing_xposed_method_cli";
    private static boolean serviceRegistered = false;
    private static final AtomicBoolean isRegistering = new AtomicBoolean(false);

    private boolean testServiceManager() {
        try {
            info("testServiceManager: 开始测试服务管理器");

            try {
                Method checkService = MethodFinder.find(
                        "android.os.ServiceManager",
                        "checkService", String.class);

                if (checkService != null) {
                    info("testServiceManager: 调用静态方法checkService");
                    Object result = checkService.invoke(null, SERVICE_NAME);
                    info("testServiceManager: checkService返回: " + result);

                    if (result != null) {
                        info("testServiceManager: 服务存在: " + result);
                        return true;
                    } else {
                        info("testServiceManager: 服务不存在");
                    }
                }
            } catch (Exception e) {
                info("testServiceManager: 直接调用checkService失败，尝试其他方式", e);
            }

            // 方法2：尝试调用getService
            try {
                Method getService = MethodFinder.find(
                        "android.os.ServiceManager",
                        "getService", String.class);

                if (getService != null) {
                    info("testServiceManager: 直接调用静态方法getService");
                    Object service = getService.invoke(null, SERVICE_NAME);
                    info("testServiceManager: getService返回: " + service);

                    if (service != null) {
                        info("testServiceManager: 通过getService获取到服务");
                        return true;
                    }
                }
            } catch (Exception e) {
                info("testServiceManager: 调用getService失败", e);
            }

            // 方法3：使用命令行检测
            info("testServiceManager: 尝试用命令行检测");
            CmdUtils.CommandOutput res = CmdUtils.runCommand("service check " + SERVICE_NAME);
            info("testServiceManager: 命令行返回: " + res.finalOutput());

            boolean stat = !res.finalOutput().contains("not found");
            if (stat) {
                info("testServiceManager: 服务注册成功");
            } else {
                error("testServiceManager: 服务注册失败");
            }
            return stat;

        } catch (Exception e) {
            error("testServiceManager: 测试ServiceManager失败", e);
            return false;
        }
    }


    private void registerService() {
        synchronized (this) {
            if (serviceRegistered) {
                info("服务已经注册，跳过重复注册");
                return;
            }
            if (isRegistering.get()) {
                info("服务正在注册中，跳过重复注册");
                return;
            }
            isRegistering.set(true);
        }

        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                info("开始注册命令行服务");

                if (HookEntry.mService == null) {
                    info("创建服务实例");
                    HookEntry.mService = new ShellService();
                    info("服务实例创建成功");
                } else {
                    info("服务实例已存在，跳过创建");
                }

                boolean registered = false;

                try {
                    Method addServiceMethod = MethodFinder.find(
                            "android.os.ServiceManager", "addService",
                            String.class, IBinder.class);

                    if (addServiceMethod != null) {
                        info("使用ServiceManager.addService注册");
                        addServiceMethod.invoke(null, SERVICE_NAME, HookEntry.mService);
                        info("ServiceManager.addService调用完成");
                        Thread.sleep(300);
                        registered = true;
                    }
                } catch (Exception e) {
                    error("ServiceManager.addService注册失败", e);
                }

                if (!registered) {
                    try {
                        Class<?> serviceManagerClass = ClassFinder.find("android.os.ServiceManager");
                        Object serviceManagerInstance = XposedHelpers.getStaticObjectField(
                                serviceManagerClass, "sServiceManager");

                        if (serviceManagerInstance != null) {
                            Method addServiceProxy = MethodFinder.find(
                                    "android.os.ServiceManager", "addService",
                                    String.class, IBinder.class);

                            if (addServiceProxy != null) {
                                info("使用ServiceManagerProxy.addService注册");
                                addServiceProxy.invoke(serviceManagerInstance, SERVICE_NAME, HookEntry.mService);
                                info("ServiceManagerProxy.addService调用完成");
                                Thread.sleep(300);
                                registered = true;
                            }
                        }
                    } catch (Exception e) {
                        error("ServiceManagerProxy注册失败", e);
                    }
                }

                if (registered) {
                    for (int i = 0; i < 3; i++) {
                        if (testServiceManager()) {
                            info("服务注册成功: " + SERVICE_NAME);
                            serviceRegistered = true;
                            logServiceStatus();
                            return;
                        }
                        Thread.sleep(500);
                    }
                    warn("服务注册后验证失败");
                } else {
                    warn("所有注册方式都失败");
                }

            } catch (Throwable e) {
                error("服务注册失败", e);
            } finally {
                isRegistering.set(false);
            }
        });
    }

    private void logServiceStatus() {
        try {
            CmdUtils.CommandOutput listOutput = CmdUtils.runCommand("service list");
            boolean foundInList = listOutput.finalOutput().contains(SERVICE_NAME);
            info("服务在service list中" + (foundInList ? "已找到" : "未找到"));

            CmdUtils.CommandOutput checkOutput = CmdUtils.runCommand("service check " + SERVICE_NAME);
            info("service check结果: " + checkOutput.finalOutput().trim());

            CmdUtils.CommandOutput callOutput = CmdUtils.runCommand("service call " + SERVICE_NAME + " 1");
            info("service call测试: " + callOutput.finalOutput().trim());

        } catch (Exception e) {
            error("记录服务状态失败", e);
        }
    }

    protected void hookImplements() {
        setHookDisplayName("Shell服务挂钩");
        setHookDescription("用来启动methods的命令行接口, 如果感觉很卡可以关掉, 但是会影响很多模块功能");
        setHookCondition(
                param -> param.packageName.equals("android")
        );

        hookMethod(
                "android.os.ServiceManager",
                "addService",
                String.class, IBinder.class,
                new XC_MethodHook() {
                    private boolean hasTriggeredRegistration = false;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String serviceName = (String) param.args[0];

                        if (!hasTriggeredRegistration && shouldRegisterAfterService(serviceName) && !serviceRegistered) {
                            hasTriggeredRegistration = true;
                            info("服务已启动，延迟注册ShellService");
                            
                            ThreadPoolManager.submitFastRunnable(() -> {
                                try {
                                    Thread.sleep(3000);
                                    registerService();
                                } catch (Exception e) {
                                    error("延迟注册失败", e);
                                }
                            });
                        }
                    }
                }
        );
    }

    private boolean shouldRegisterAfterService(String serviceName) {
        String[] keyServices = {
                "activity",
                "package",
                "window",
                "input_method",
                "batterystats"
        };
        for (String key : keyServices) {
            if (serviceName.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getTag() {
        return "ShellServiceHook";
    }
}