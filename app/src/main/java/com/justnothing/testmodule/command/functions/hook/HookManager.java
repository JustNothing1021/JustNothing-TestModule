package com.justnothing.testmodule.command.functions.hook;



import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.script.ScriptModels;
import com.justnothing.testmodule.command.functions.script.ScriptRunner;
import com.justnothing.testmodule.command.functions.script.ScriptModels.*;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.OutputHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.SignatureUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookManager {
    private static final String TAG = "HookManager";
    
    private static final ConcurrentHashMap<String, HookInfo> hooks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, XC_MethodHook.Unhook> activeHooks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScriptRunner> scriptRunners = new ConcurrentHashMap<>();
    
    private static XC_LoadPackage.LoadPackageParam currentLoadPackageParam;
    private static final List<String> imports = new ArrayList<>(Arrays.asList("java.lang.*", "java.util.*"));

    private static final Logger logger = Logger.getLoggerForName(TAG);


    public static void setLoadPackageParam(XC_LoadPackage.LoadPackageParam param) {
        currentLoadPackageParam = param;
    }

    public static void addHookBuiltIn(ScriptModels.ExecutionContext context,
                                      MethodHookParam methodHookParam,
                                      LoadPackageParam loadPackageParam,
                                      HookInfo hookInfo,
                                      String phase
    ) {
            context.addBuiltIn("getMethodHookParam", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getMethodHookParam() 不接受任何参数，忽略参数");
                }
                return methodHookParam;
            });

            context.addBuiltIn("getPhase", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getPhase() 不接受任何参数，忽略参数");
                }
                return phase;
            });

            context.addBuiltIn("getHookId", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getHookId() 不接受任何参数，忽略参数");
                }
                return hookInfo.getId();
            });

            context.addBuiltIn("getLoadPackageParam", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getLoadPackageParam() 不接受任何参数，忽略参数");
                }
                return loadPackageParam;
            });

            context.addBuiltIn("getHookInfo", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getHookInfo() 不接受任何参数，忽略参数");
                }
                return hookInfo;
            });

            context.addBuiltIn("setReturnValue", args -> {
                if (args.isEmpty()) {
                    logger.warn("setReturnValue() 必须提供一个返回值");
                    return null;
                } else if (args.size() > 1) {
                    logger.warn("setReturnValue() 只接受一个参数，忽略其他参数");
                }
                methodHookParam.setResult(args.get(0));
                return null;
            });

            context.addBuiltIn("setThrowable", args -> {
                if (args.isEmpty()) {
                    logger.warn("setThrowable() 必须提供一个异常");
                    return null;
                } else if (args.size() > 1) {
                    logger.warn("setThrowable() 只接受一个参数，忽略其他参数");
                }
                methodHookParam.setThrowable((Throwable) args.get(0));
                return null;
            });

            context.addBuiltIn("getReturnValue", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getReturnValue() 不接受任何参数，忽略参数");
                }
                return methodHookParam.getResult();
            });
    }

    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        return currentLoadPackageParam;
    }

    public static String addHook(String className, String methodName, String signature,
                                 String beforeCode, String afterCode, String replaceCode,
                                 String beforeCodebase, String afterCodebase, String replaceCodebase,
                                 CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        HookInfo hookInfo = new HookInfo(className, methodName, signature, 
                                        beforeCode, afterCode, replaceCode, 
                                        beforeCodebase, afterCodebase, replaceCodebase, classLoader);
        
        try {
            logger.info("验证Hook代码: " + hookInfo.getId());
            context.output().println("[INFO]  验证Hook代码...");
            validateHookCode(hookInfo, classLoader);
            logger.info("Hook 代码验证成功: " + hookInfo.getId());
        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException(
                "hook add", 
                e, 
                logger, 
                context.output(), 
                "Hook代码验证失败"
            );
            context.output().println("[ERROR] Hook代码验证失败... 信息: " + e + "\n\n");
            return errorMsg;
        }
        
        hooks.put(hookInfo.getId(), hookInfo);
        
        try {
            logger.info("开始应用Hook: " + hookInfo.getId() +
                      " 类名: " + className + 
                      " 方法名: " + methodName + 
                      " 签名: " + (signature != null ? signature : "默认"));
            context.output().println("[INFO]  开始应用Hook: " + className + "." + methodName
                                        + ", signature = " + signature);
            applyHook(hookInfo);
            logger.info("Hook添加成功: " + hookInfo.getId());
            context.output().println("[INFO]  Hook添加成功!\n\n");
            return "Hook添加成功\nID: " + hookInfo.getId() + "\n" + hookInfo.getDisplayInfo();
        } catch (Exception e) {
            hooks.remove(hookInfo.getId());
            Map<String, Object> errContext = new java.util.HashMap<>();
            errContext.put("类名", className);
            errContext.put("方法名", methodName);
            errContext.put("签名", signature != null ? signature : "默认");
            errContext.put("Hook ID", hookInfo.getId());
            String errorMsg = CommandExceptionHandler.handleException(
                "hook add", 
                e, 
                logger, 
                errContext,
                "Hook添加失败"
            );
            context.output().println("[ERROR] Hook添加失败... 信息: " + e + "\n\n");
            return errorMsg;
        }
    }

    private static void validateHookCode(HookInfo hookInfo, ClassLoader classLoader) throws Exception {
        ScriptRunner runner = new ScriptRunner(classLoader);
        if (hookInfo.getBeforeCode() != null && !hookInfo.getBeforeCode().isEmpty()) {
            logger.info("验证before代码");
            validateCode(runner, hookInfo.getBeforeCode(), "before");
        }
        
        if (hookInfo.getAfterCode() != null && !hookInfo.getAfterCode().isEmpty()) {
            logger.info("验证after代码");
            validateCode(runner, hookInfo.getAfterCode(), "after");
        }
        
        if (hookInfo.getReplaceCode() != null && !hookInfo.getReplaceCode().isEmpty()) {
            logger.info("验证replace代码");
            validateCode(runner, hookInfo.getReplaceCode(), "replace");
        }
        
        if (hookInfo.getBeforeCodebase() != null && !hookInfo.getBeforeCodebase().isEmpty()) {
            logger.info("验证before codebase");
            String code = loadCodeFromCodebase(hookInfo.getBeforeCodebase());
            if (code == null) {
                throw new IllegalArgumentException("无法加载codebase文件: " + hookInfo.getBeforeCodebase());
            }
            validateCode(runner, code, "before");
        }
        
        if (hookInfo.getAfterCodebase() != null && !hookInfo.getAfterCodebase().isEmpty()) {
            logger.info("验证after codebase");
            String code = loadCodeFromCodebase(hookInfo.getAfterCodebase());
            if (code == null) {
                throw new IllegalArgumentException("无法加载codebase文件: " + hookInfo.getAfterCodebase());
            }
            validateCode(runner, code, "after");
        }
        
        if (hookInfo.getReplaceCodebase() != null && !hookInfo.getReplaceCodebase().isEmpty()) {
            logger.info("验证replace codebase");
            String code = loadCodeFromCodebase(hookInfo.getReplaceCodebase());
            if (code == null) {
                throw new IllegalArgumentException("无法加载codebase文件: " + hookInfo.getReplaceCodebase());
            }
            validateCode(runner, code, "replace");
        }
    }

    private static void validateCode(ScriptRunner runner, String code, String phase) throws Exception {
        try {
            runner.tryParse(code);
        } catch (Exception e) {
            throw new Exception(phase + " 代码验证失败: " + e.getMessage(), e);
        }
    }

    private static void applyHook(HookInfo hookInfo) throws ClassNotFoundException, NoSuchMethodException {
        logger.info("开始查找类: " + hookInfo.getClassName());
        Class<?> targetClass = ClassResolver.findClassOrFail(hookInfo.getClassName(), hookInfo.getClassLoader());
        logger.info("类查找成功: " + targetClass.getName());
        
        boolean isConstructor = hookInfo.getMethodName().equals("<init>");
        boolean isStaticInitializer = hookInfo.getMethodName().equals("<clinit>");
        
        if (isStaticInitializer) {
            throw new UnsupportedOperationException("不支持hook静态初始化块<clinit>，因为它们在类加载时就执行了");
        }
        
        logger.info("是否为构造函数: " + isConstructor);
        
        Class<?>[] paramTypes = SignatureUtils.parseSignature(hookInfo.getSignature(), hookInfo.getClassLoader());
        logger.info("参数类型: " + Arrays.toString(paramTypes));
        
        if (paramTypes.length == 0) {
            logger.info("未指定签名，尝试自动查找方法重载");
            if (isConstructor) {
                paramTypes = findConstructorParameters(targetClass);
            } else {
                paramTypes = findMethodParameters(targetClass, hookInfo.getMethodName());
            }
            if (paramTypes == null) {
                throw new NoSuchMethodException("找不到" + (isConstructor ? "构造函数" : "方法") + ": " + hookInfo.getMethodName());
            }
            logger.info("自动找到参数类型: " + Arrays.toString(paramTypes));
        }
        
        boolean hasBefore = hookInfo.hasBefore();
        boolean hasAfter = hookInfo.hasAfter();
        boolean hasReplace = hookInfo.hasReplace();
        
        if (!hasBefore && !hasAfter && !hasReplace) {
            throw new IllegalArgumentException("Hook必须指定至少一个阶段（before/after/replace）");
        }
        
        if (hasReplace) {
            logger.info("创建替换Hook");
            XC_MethodHook replacementHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!hasBefore) {
                        return;
                    }
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过before Hook执行，id = " + hookInfo.getId());
                        return;
                    }
                    
                    logger.info("准备执行before Hook，id = " + hookInfo.getId());
                    hookInfo.incrementCallCount();
                    
                    if (hookInfo.getBeforeCode() != null && !hookInfo.getBeforeCode().isEmpty()) {
                        executeHookCode(hookInfo, hookInfo.getBeforeCode(), param, "before");
                    } else if (hookInfo.getBeforeCodebase() != null && !hookInfo.getBeforeCodebase().isEmpty()) {
                        String code = loadCodeFromCodebase(hookInfo.getBeforeCodebase());
                        if (code != null) {
                            executeHookCode(hookInfo, code, param, "before");
                        }
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过替换Hook执行，id = " + hookInfo.getId());
                        return;
                    }
                    logger.info("准备执行replace Hook，id = " + hookInfo.getId());
                    hookInfo.incrementCallCount();
                    
                    if (hookInfo.getReplaceCode() != null && !hookInfo.getReplaceCode().isEmpty()) {
                        executeHookCode(hookInfo, hookInfo.getReplaceCode(), param, "replace");
                    } else if (hookInfo.getReplaceCodebase() != null && !hookInfo.getReplaceCodebase().isEmpty()) {
                        String code = loadCodeFromCodebase(hookInfo.getReplaceCodebase());
                        if (code != null) {
                            executeHookCode(hookInfo, code, param, "replace");
                        }
                    }
                    
                    if (hasAfter) {
                        logger.info("准备执行after Hook，id = " + hookInfo.getId());
                        hookInfo.incrementCallCount();
                        
                        if (hookInfo.getAfterCode() != null && !hookInfo.getAfterCode().isEmpty()) {
                            executeHookCode(hookInfo, hookInfo.getAfterCode(), param, "after");
                        } else if (hookInfo.getAfterCodebase() != null && !hookInfo.getAfterCodebase().isEmpty()) {
                            String code = loadCodeFromCodebase(hookInfo.getAfterCodebase());
                            if (code != null) {
                                executeHookCode(hookInfo, code, param, "after");
                            }
                        }
                    }
                }
            };
            
            logger.info("开始应用替换Hook: " + hookInfo.getMethodName());
            XC_MethodHook.Unhook unhook;
            if (isConstructor) {
                unhook = XposedHelpers.findAndHookConstructor(targetClass, paramTypes, replacementHook);
            } else {
                Object[] hookArgs = buildHookArgs(paramTypes, replacementHook);
                unhook = XposedHelpers.findAndHookMethod(targetClass, hookInfo.getMethodName(), hookArgs);
            }
            activeHooks.put(hookInfo.getId(), unhook);
            logger.info("替换Hook应用成功: " + hookInfo.getId());
        } else {
            logger.info("创建普通Hook (before: " + hasBefore + ", after: " + hasAfter + ")");
            XC_MethodHook methodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!hasBefore) {
                        return;
                    }
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过before Hook执行，id = " + hookInfo.getId());
                        return;
                    }
                    
                    logger.info("准备执行before Hook，id = " + hookInfo.getId());
                    hookInfo.incrementCallCount();
                    
                    if (hookInfo.getBeforeCode() != null && !hookInfo.getBeforeCode().isEmpty()) {
                        executeHookCode(hookInfo, hookInfo.getBeforeCode(), param, "before");
                    } else if (hookInfo.getBeforeCodebase() != null && !hookInfo.getBeforeCodebase().isEmpty()) {
                        String code = loadCodeFromCodebase(hookInfo.getBeforeCodebase());
                        if (code != null) {
                            executeHookCode(hookInfo, code, param, "before");
                        }
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!hasAfter) {
                        return;
                    }
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过after Hook执行，id = " + hookInfo.getId());
                        return;
                    }
                    logger.info("准备执行after Hook，id = " + hookInfo.getId());
                    hookInfo.incrementCallCount();
                    
                    if (hookInfo.getAfterCode() != null && !hookInfo.getAfterCode().isEmpty()) {
                        executeHookCode(hookInfo, hookInfo.getAfterCode(), param, "after");
                    } else if (hookInfo.getAfterCodebase() != null && !hookInfo.getAfterCodebase().isEmpty()) {
                        String code = loadCodeFromCodebase(hookInfo.getAfterCodebase());
                        if (code != null) {
                            executeHookCode(hookInfo, code, param, "after");
                        }
                    }
                }
            };
            
            logger.info("开始应用普通Hook: " + hookInfo.getMethodName());
            XC_MethodHook.Unhook unhook;
            if (isConstructor) {
                unhook = XposedHelpers.findAndHookConstructor(targetClass, paramTypes, methodHook);
            } else {
                Object[] hookArgs = buildHookArgs(paramTypes, methodHook);
                unhook = XposedHelpers.findAndHookMethod(targetClass, hookInfo.getMethodName(), hookArgs);
            }
            activeHooks.put(hookInfo.getId(), unhook);
            logger.info("普通Hook应用成功: " + hookInfo.getId());
        }
        
        hookInfo.setActive(true);
        logger.info("Hook状态设置为活跃: " + hookInfo.getId());
    }

    private static Object[] buildHookArgs(Class<?>[] paramTypes, XC_MethodHook hook) {
        Object[] args = new Object[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
        args[paramTypes.length] = hook;
        return args;
    }

    private static void executeHookCode(HookInfo hookInfo, String code,
                                        MethodHookParam param, String phase) {
        try {
            ClassLoader cl = hookInfo.getClassLoader();
            ScriptRunner runner = scriptRunners.computeIfAbsent(
                hookInfo.getId(), k -> new ScriptRunner(cl));
            logger.debug("运行代码, hook id = " + hookInfo.getId() + "\n" + code);
            String prefix = "[" + hookInfo.getId() + "][" + phase + "] ";
            IOutputHandler outputHandler = new OutputHandler(logger, prefix);
            IOutputHandler errorHandler = new OutputHandler(logger, prefix);
            ExecutionContext context = new ExecutionContext(cl, outputHandler, errorHandler);
            addHookBuiltIn(context, param, getLoadPackageParam(), hookInfo, phase);
            runner.setContext(context);
            runner.execute(code);
            runner.getAllVariablesAsObject().get("result");
        } catch (Exception e) {
            logger.error("Hook 代码执行失败: " + hookInfo.getId(), e);
        }
    }

    private static String loadCodeFromCodebase(String codebase) {
        try {
            java.io.File scriptFile;
            
            File scriptsDir = DataBridge.getScriptsDirectory();
            
            if (!codebase.contains("/") && !codebase.contains("\\")) {
                scriptFile = new File(scriptsDir, codebase);
                
                if (scriptFile.exists()) {
                    logger.info("从codebase目录加载脚本: " + codebase);
                } else {
                    logger.warn("codebase目录中未找到脚本: " + codebase);
                    return null;
                }
            } else {
                // 包含路径分隔符，提取文件名
                String fileName = new java.io.File(codebase).getName();
                scriptFile = new File(scriptsDir, fileName);
                
                if (scriptFile.exists()) {
                    logger.info("从codebase目录加载脚本（使用文件名）: " + fileName);
                } else {
                    // 如果 codebase 目录中没有，尝试作为完整路径
                    scriptFile = new java.io.File(codebase);
                    if (scriptFile.exists()) {
                        logger.info("从指定路径加载脚本: " + codebase);
                    } else {
                        logger.warn("脚本文件不存在: " + scriptFile.getAbsolutePath());
                        return null;
                    }
                }
            }
            return IOManager.readFile(scriptFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("加载脚本失败: " + codebase, e);
            return null;
        }
    }


    private static Class<?>[] findMethodParameters(Class<?> targetClass, String methodName) {
        try {
            java.lang.reflect.Method[] methods = targetClass.getDeclaredMethods();
            java.lang.reflect.Method matchedMethod = null;
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(methodName)) {
                    if (matchedMethod == null) {
                        matchedMethod = method;
                    } else {
                        logger.debug("找到多个重载方法: " + methodName + 
                                   ", 参数数量: " + method.getParameterCount());
                    }
                }
            }
            
            if (matchedMethod != null) {
                Class<?>[] paramTypes = matchedMethod.getParameterTypes();
                logger.info("找到方法: " + methodName + ", 参数类型: " + Arrays.toString(paramTypes));
                return paramTypes;
            }
            
            logger.info("未找到方法: " + methodName);
            return null;
        } catch (Exception e) {
            logger.error("查找方法参数失败: " + methodName, e);
            return null;
        }
    }

    private static Class<?>[] findConstructorParameters(Class<?> targetClass) {
        try {
            java.lang.reflect.Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            java.lang.reflect.Constructor<?> matchedConstructor = null;
            
            for (java.lang.reflect.Constructor<?> constructor : constructors) {
                if (matchedConstructor == null) {
                    matchedConstructor = constructor;
                } else {
                    logger.debug("找到多个构造函数, 参数数量: " + constructor.getParameterCount());
                }
            }
            
            if (matchedConstructor != null) {
                Class<?>[] paramTypes = matchedConstructor.getParameterTypes();
                logger.info("找到构造函数, 参数类型: " + Arrays.toString(paramTypes));
                return paramTypes;
            }
            
            logger.info("未找到构造函数");
            return null;
        } catch (Exception e) {
            logger.error("查找构造函数参数失败", e);
            return null;
        }
    }

    public static String removeHook(String hookId) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            return "Hook不存在: " + hookId;
        }
        
        XC_MethodHook.Unhook unhook = activeHooks.remove(hookId);
        if (unhook != null) {
            unhook.unhook();
        }
        
        hookInfo.setActive(false);
        hooks.remove(hookId);
        
        ScriptRunner runner = scriptRunners.remove(hookId);
        if (runner != null) {
            runner.clearVariables();
        }
        
        logger.info("Hook移除成功: " + hookId);
        return "Hook移除成功: " + hookId;
    }

    public static String listHooks() {
        if (hooks.isEmpty()) {
            return "没有活动的Hook";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("===== Hook列表 =====\n\n");
        
        for (HookInfo hookInfo : hooks.values()) {
            sb.append(hookInfo.getDisplayInfo()).append("\n");
            sb.append("------------------------\n\n");
        }
        
        sb.append("总计: ").append(hooks.size()).append(" 个Hook");
        return sb.toString();
    }

    public static String getHookInfo(String hookId) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            return "Hook 不存在: " + hookId;
        }
        
        return hookInfo.getDisplayInfo();
    }

    public static String enableHook(String hookId) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            return "Hook 不存在: " + hookId;
        }
        
        hookInfo.setEnabled(true);
        logger.info("启用Hook: " + hookId);
        return "Hook已启用: " + hookId;
    }

    public static String disableHook(String hookId) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            return "Hook不存在: " + hookId;
        }
        
        hookInfo.setEnabled(false);
        logger.info("禁用Hook: " + hookId);
        return "Hook已禁用: " + hookId;
    }

    public static String getHookOutput(String hookId, int count) {
        return "Hook 输出功能待实现\n" +
               "HookID: " + hookId + "\n" +
               "调用次数: " + (hooks.get(hookId) != null ? Objects.requireNonNull(hooks.get(hookId)).getCallCount() : "N/A");
    }

    public static int getHookCount() {
        return hooks.size();
    }

    public static List<HookInfo> getAllHooks() {
        return new ArrayList<>(hooks.values());
    }

    public static void clearAllHooks() {
        for (String hookId : hooks.keySet()) {
            removeHook(hookId);
        }
        logger.info("所有Hook已清除");
    }
}
