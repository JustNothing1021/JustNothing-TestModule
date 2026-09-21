package com.justnothing.testmodule.command.functions.hook.util;



import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.SystemOutputRedirector;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.engine.ScriptRunner;
import com.justnothing.engine.ast.ASTNode;
import com.justnothing.engine.eval.EvalContext;
import com.justnothing.engine.eval.Value;
import com.justnothing.engine.eval.Value.NullValue;
import com.justnothing.engine.eval.Value.VoidValue;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.framework.output.HookOutputHandler;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.hook.model.HookInfo;
import com.justnothing.testmodule.command.functions.hook.response.HookAddResult;
import com.justnothing.testmodule.hooks.api.HookAPI;
import com.justnothing.testmodule.hooks.api.LoadPackageInfo;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.expr.SignatureUtils;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.stream.Collectors;

import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodHook;
import com.justnothing.testmodule.hooks.api.UnhookHandle;

public class HookManager {
    private static final String TAG = "HookManager";

    private static final ConcurrentHashMap<String, HookInfo> hooks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UnhookHandle> activeHooks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScriptRunner> scriptRunners = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ICommandOutputHandler> outputHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ICommandOutputHandler> errorHandlers = new ConcurrentHashMap<>();


    private static LoadPackageInfo currentLoadPackageInfo;
    private static final List<String> imports = new ArrayList<>(Arrays.asList("java.lang.*", "java.util.*"));

    private static final Logger logger = Logger.getLoggerForName(TAG);

    private HookManager() {
        throw new UnsupportedOperationException(
                Text.zhEn("不能实例化HookManager...", "HookManager cannot be instantiated...").text());
    }

    public record AddHookResult(boolean success, String hookId, String errorMessage) {

        public static AddHookResult success(String hookId) {
                return new AddHookResult(true, hookId, null);
            }

            public static AddHookResult failure(String errorMessage) {
                return new AddHookResult(false, null, errorMessage);
            }
        }

    /**
     * 取当前方法调用上下文的两个等价名字。
     * <p>
     * 内置示例脚本（{@code assets/codebase/*}）用的是 {@code getParam}，早期代码用的是
     * {@code getMethodHookParam}，两个都注册，谁都不用改。
     * </p>
     */
    private static final String[] PARAM_ACCESSOR_NAMES = {"getParam", "getMethodHookParam"};

    public static void addHookBuiltIn(EvalContext context,
                                      HookParam methodHookParam,
                                      LoadPackageInfo loadPackageInfo,
                                      HookInfo hookInfo,
                                      String phase,
                                      AtomicBoolean returnValueSet
    ) {
            for (String name : PARAM_ACCESSOR_NAMES) {
                context.addBuiltIn(name, args -> {
                    if (!args.isEmpty()) {
                        logger.warn(name + "() 不接受任何参数，忽略参数");
                    }
                    return Value.of(methodHookParam);
                });
            }

            context.addBuiltIn("getPhase", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getPhase() 不接受任何参数，忽略参数");
                }
                return Value.of(phase);
            });

            context.addBuiltIn("getHookId", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getHookId() 不接受任何参数，忽略参数");
                }
                return Value.of(hookInfo.getId());
            });

            context.addBuiltIn("getLoadPackageParam", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getLoadPackageParam() 不接受任何参数，忽略参数");
                }
                return Value.of(loadPackageInfo);
            });

            context.addBuiltIn("getHookInfo", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getHookInfo() 不接受任何参数，忽略参数");
                }
                return Value.of(hookInfo);
            });

            context.addBuiltIn("setReturnValue", args -> {
                if (args.isEmpty()) {
                    logger.warn("setReturnValue() 必须提供一个返回值");
                    return NullValue.INSTANCE;
                } else if (args.size() > 1) {
                    logger.warn("setReturnValue() 只接受一个参数，忽略其他参数");
                }
                methodHookParam.setResult(args.get(0).asJavaObject());
                if (returnValueSet != null) {
                    returnValueSet.set(true);
                }
                return VoidValue.INSTANCE;
            });
            context.addBuiltIn("setThrowable", args -> {
                if (args.isEmpty()) {
                    logger.warn("setThrowable() 必须提供一个异常");
                    return NullValue.INSTANCE;
                } else if (args.size() > 1) {
                    logger.warn("setThrowable() 只接受一个参数，忽略其他参数");
                }
                methodHookParam.setThrowable((Throwable) args.get(0).asJavaObject());
                return VoidValue.INSTANCE;
            });

            context.addBuiltIn("getReturnValue", args -> {
                if (!args.isEmpty()) {
                    logger.warn("getReturnValue() 不接受任何参数，忽略参数");
                }
                return Value.of(methodHookParam.getResult());
            });
    }

    public static void setLoadPackageParam(LoadPackageInfo param) {
        currentLoadPackageInfo = param;
    }

    public static LoadPackageInfo getLoadPackageParam() {
        // 因为一个线程里边的LoadPackageParam不会变（虽然只是理论上，也有一个线程初始化多个包的情况）
        // 所以直接设置就行
        if (currentLoadPackageInfo == null) setLoadPackageParam(HookEntry.getLastLoadPackageInfo());
        return currentLoadPackageInfo;
    }

    public static AddHookResult addHook(String className, String methodName, String signature,
                               String beforeCode, String afterCode, String replaceCode,
                               String beforeCodebase, String afterCodebase, String replaceCodebase,
                               CommandExecutor.CmdExecContext<?> context) {
        ClassLoader classLoader = context.classLoader();
        HookInfo hookInfo = new HookInfo(className, methodName, signature,
                                        beforeCode, afterCode, replaceCode,
                                        beforeCodebase, afterCodebase, replaceCodebase, classLoader);

        try {
            logger.info("验证Hook代码: " + hookInfo.getId());
            context.print(Text.zhEn("验证Hook代码...", "Validating hook code...").text(), Colors.CYAN);
            validateHookCode(hookInfo, classLoader);
            context.println(Text.zhEn(" 验证成功!", " Verification passed!").text(), Colors.LIGHT_GREEN);
            logger.info("Hook 代码验证成功: " + hookInfo.getId());
        } catch (Exception e) {
            context.println(Text.zhEn(" 验证失败!", " Verification failed!").text(), Colors.RED);
            context.print(HookTexts.ERR_HOOK_CODE_VALIDATION_FAILED.text() + ": ", Colors.RED);
            String errorMsg = Objects.requireNonNullElse(e.getMessage(), HookTexts.VALUE_NO_DETAILS.text());
            context.println(errorMsg, Colors.YELLOW);
            CommandExceptionHandler.handleException(
                    "hook add",
                    e,
                    context,
                    HookTexts.ERR_HOOK_CODE_VALIDATION_FAILED.text()
            );
            return AddHookResult.failure(
                    Text.zhEn("代码验证失败: %s", "Code validation failed: %s").format(errorMsg));
        }

        hooks.put(hookInfo.getId(), hookInfo);
        context.println("", Colors.DEFAULT);

        try {
            logger.info("开始应用Hook: " + hookInfo.getId() +
                      " 类名: " + className +
                      " 方法名: " + methodName +
                      " 签名: " + (signature != null ? signature : "默认"));
            context.print(Text.zhEn("开始应用Hook: ", "Applying hook: ").text(), Colors.CYAN);
            context.print(className, Colors.GREEN);
            context.print(".", Colors.WHITE);
            context.print(methodName, Colors.YELLOW);
            if (signature != null && !signature.isEmpty()) {
                context.print(", signature = ", Colors.GRAY);
                context.print(signature, Colors.LIGHT_GREEN);
            }
            context.println("", Colors.DEFAULT);

            applyHook(hookInfo);
            logger.info("Hook添加成功: " + hookInfo.getId());
            context.print(Text.zhEn("Hook添加成功!", "Hook added!").text(), Colors.LIGHT_GREEN);
            context.print(" ID: ", Colors.CYAN);
            context.println(hookInfo.getId(), Colors.YELLOW);
            context.println("");
            hookInfo.printDisplayInfo(context);
            return AddHookResult.success(hookInfo.getId());
        } catch (Exception e) {
            hooks.remove(hookInfo.getId());
            context.print(HookTexts.ERR_HOOK_ADD_FAILED.text() + ": ", Colors.RED);
            String errorMsg = Objects.requireNonNullElse(e.getMessage(), HookTexts.VALUE_NO_DETAILS.text());
            context.println(errorMsg, Colors.YELLOW);
            context.println("", Colors.DEFAULT);

            Map<String, Object> errContext = new HashMap<>();
            errContext.put(CliMessages.CONTEXT_CLASS_NAME.text(), className);
            errContext.put(CliMessages.CONTEXT_METHOD_NAME.text(), methodName);
            errContext.put(CliMessages.CONTEXT_SIGNATURE.text(),
                    signature != null ? signature : Text.zhEn("默认", "default").text());
            errContext.put("Hook ID", hookInfo.getId());
            CommandExceptionHandler.handleException(
                    "hook add",
                    e,
                    context,
                    errContext,
                    HookTexts.ERR_HOOK_ADD_FAILED.text()
            );
            return AddHookResult.failure(
                    Text.zhEn("应用Hook失败: %s", "Failed to apply hook: %s").format(errorMsg));
        }
    }

    private static final String[] HOOK_BUILTIN_NAMES = {
        "getParam", "getMethodHookParam", "getPhase", "getHookId", "getLoadPackageParam",
        "getHookInfo", "setReturnValue", "setThrowable", "getReturnValue"
    };

    private static void registerHookBuiltinPlaceholders(ScriptRunner runner) {
        for (String name : HOOK_BUILTIN_NAMES) {
            runner.addBuiltin(name, args -> com.justnothing.engine.eval.Value.NullValue.INSTANCE);
        }
    }

    private static void validateHookCode(HookInfo hookInfo, ClassLoader classLoader) throws RuntimeException {
        ScriptRunner runner = new ScriptRunner(classLoader);
        runner.setClassFinder(new AppClassFinder());
        registerHookBuiltinPlaceholders(runner);
        if (hookInfo.getBeforeCode() != null && !hookInfo.getBeforeCode().isEmpty()) {
            logger.info("验证before代码");
            hookInfo.setBeforeParsed(validateCode(runner, hookInfo.getBeforeCode(), "before"));
        }

        if (hookInfo.getAfterCode() != null && !hookInfo.getAfterCode().isEmpty()) {
            logger.info("验证after代码");
            hookInfo.setAfterParsed(validateCode(runner, hookInfo.getAfterCode(), "after"));
        }

        if (hookInfo.getReplaceCode() != null && !hookInfo.getReplaceCode().isEmpty()) {
            logger.info("验证replace代码");
            hookInfo.setReplaceParsed(validateCode(runner, hookInfo.getReplaceCode(), "replace"));
        }

        if (hookInfo.getBeforeCodebase() != null && !hookInfo.getBeforeCodebase().isEmpty()) {
            logger.info("验证before codebase");
            String code = loadCodeFromCodebase(hookInfo.getBeforeCodebase());
            if (code == null) {
                throw new IllegalArgumentException(
                        HookTexts.ERR_CODEBASE_LOAD_FAILED.format(hookInfo.getBeforeCodebase()));
            }
            hookInfo.setBeforeParsed(validateCode(runner, code, "before"));
        }

        if (hookInfo.getAfterCodebase() != null && !hookInfo.getAfterCodebase().isEmpty()) {
            logger.info("验证after codebase");
            String code = loadCodeFromCodebase(hookInfo.getAfterCodebase());
            if (code == null) {
                throw new IllegalArgumentException(
                        HookTexts.ERR_CODEBASE_LOAD_FAILED.format(hookInfo.getAfterCodebase()));
            }
            hookInfo.setAfterParsed(validateCode(runner, code, "after"));
        }

        if (hookInfo.getReplaceCodebase() != null && !hookInfo.getReplaceCodebase().isEmpty()) {
            logger.info("验证replace codebase");
            String code = loadCodeFromCodebase(hookInfo.getReplaceCodebase());
            if (code == null) {
                throw new IllegalArgumentException(
                        HookTexts.ERR_CODEBASE_LOAD_FAILED.format(hookInfo.getReplaceCodebase()));
            }
            hookInfo.setReplaceParsed(validateCode(runner, code, "replace"));
        }
    }

    private static List<ASTNode> validateCode(ScriptRunner runner, String code, String phase) throws RuntimeException {
        try {
            return runner.tryParse(code);
        } catch (Exception e) {
            throw new RuntimeException(
                    Text.zhEn("%s 代码验证失败: %s", "%s code validation failed: %s")
                            .format(phase, e.getMessage()), e);
        }
    }

    private static void applyHook(HookInfo hookInfo) throws ClassNotFoundException, NoSuchMethodException {
        logger.info("开始查找类: " + hookInfo.getClassName());
        Class<?> targetClass = ClassResolver.findClassOrFail(hookInfo.getClassName(), hookInfo.getClassLoader());
        logger.info("类查找成功: " + targetClass.getName());

        boolean isConstructor = hookInfo.getMethodName().equals("<init>");
        boolean isStaticInitializer = hookInfo.getMethodName().equals("<clinit>");

        if (isStaticInitializer) {
            throw new UnsupportedOperationException(Text.zhEn(
                    "不支持hook静态初始化块<clinit>，因为它们在类加载时就执行了",
                    "Cannot hook static initializers (<clinit>): they run when the class is loaded").text());
        }

        logger.info("是否为构造函数: " + isConstructor);

        Class<?>[] paramTypes = SignatureUtils.parseParamList(hookInfo.getSignature(), hookInfo.getClassLoader());
        logger.info("参数类型: " + Arrays.toString(paramTypes));

        if (paramTypes.length == 0) {
            logger.info("未指定签名，尝试自动查找方法重载");
            if (isConstructor) {
                paramTypes = findConstructorParameters(targetClass);
            } else {
                paramTypes = findMethodParameters(targetClass, hookInfo.getMethodName());
            }
            if (paramTypes == null) {
                String target = isConstructor
                        ? Text.zhEn("构造函数", "constructor").text()
                        : Text.zhEn("方法", "method").text();
                throw new NoSuchMethodException(Text.zhEn("找不到%s: %s", "No such %s: %s")
                        .format(target, hookInfo.getMethodName()));
            }
            logger.info("自动找到参数类型: " + Arrays.toString(paramTypes));
        }

        boolean hasBefore = hookInfo.hasBefore();
        boolean hasAfter = hookInfo.hasAfter();
        boolean hasReplace = hookInfo.hasReplace();

        if (!hasBefore && !hasAfter && !hasReplace) {
            throw new IllegalArgumentException(Text.zhEn(
                    "Hook必须指定至少一个阶段（before/after/replace）",
                    "A hook must specify at least one phase (before/after/replace)").text());
        }

        if (hasReplace) {
            logger.info("创建替换Hook");
            MethodHook replacementHook = new MethodHook() {
                @Override
                protected void beforeHookedMethod(HookParam param) {
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过Hook执行，id = " + hookInfo.getId());
                        return;
                    }

                    // 计数放在这里、且只放一次：beforeHookedMethod 每次方法调用必被调用，
                    // 之前是 before 和 replace 各数一次，挂了两个阶段的方法会被数成两次。
                    hookInfo.incrementCallCount();

                    if (hasBefore) {
                        logger.info("准备执行before Hook，id = " + hookInfo.getId());
                        executeHookCode(hookInfo, hookInfo.getBeforeParsed(), param, "before");
                    }

                    logger.info("准备执行replace Hook，id = " + hookInfo.getId());

                    AtomicBoolean returnValueSet = new AtomicBoolean(false);
                    executeHookCodeWithReturnFlag(hookInfo, hookInfo.getReplaceParsed(), param, "replace", returnValueSet);
                }

                @Override
                protected void afterHookedMethod(HookParam param) {
                    if (!hasAfter) {
                        return;
                    }
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过after Hook执行，id = " + hookInfo.getId());
                        return;
                    }

                    logger.info("准备执行after Hook，id = " + hookInfo.getId());
                    executeHookCode(hookInfo, hookInfo.getAfterParsed(), param, "after");
                }
            };

            logger.info("开始应用替换Hook: " + hookInfo.getMethodName());
            UnhookHandle unhook;
            if (isConstructor) {
                if (paramTypes.length == 0) {
                    unhook = HookAPI.findAndHookConstructor(targetClass, replacementHook);
                } else {
                    Object[] args = new Object[paramTypes.length + 1];
                    System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
                    args[paramTypes.length] = replacementHook;
                    unhook = HookAPI.findAndHookConstructor(targetClass, args);
                }
            } else {
                if (paramTypes.length == 0) {
                    unhook = HookAPI.findAndHookMethod(targetClass, hookInfo.getMethodName(), replacementHook);
                } else {
                    Object[] args = new Object[paramTypes.length + 1];
                    System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
                    args[paramTypes.length] = replacementHook;
                    unhook = HookAPI.findAndHookMethod(targetClass, hookInfo.getMethodName(), args);
                }
            }
            activeHooks.put(hookInfo.getId(), unhook);
            logger.info("替换Hook应用成功: " + hookInfo.getId());
        } else {
            logger.info("创建普通Hook (before: " + hasBefore + ", after: " + hasAfter + ")");
            MethodHook methodHook = new MethodHook() {
                @Override
                protected void beforeHookedMethod(HookParam param) {
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过Hook执行，id = " + hookInfo.getId());
                        return;
                    }

                    // 只在这里数一次。只挂了 after 阶段时也要在这里数，否则 beforeHookedMethod
                    // 提前返回，after 那边再数就又多一次，两边加起来才是「方法被调用了多少次」。
                    hookInfo.incrementCallCount();

                    if (!hasBefore) {
                        return;
                    }

                    logger.info("准备执行before Hook，id = " + hookInfo.getId());
                    executeHookCode(hookInfo, hookInfo.getBeforeParsed(), param, "before");
                }

                @Override
                protected void afterHookedMethod(HookParam param) {
                    if (!hasAfter) {
                        return;
                    }
                    if (!hookInfo.isEnabled() || !hookInfo.isActive()) {
                        logger.debug("hook未启用或未激活，跳过after Hook执行，id = " + hookInfo.getId());
                        return;
                    }
                    logger.info("准备执行after Hook，id = " + hookInfo.getId());
                    executeHookCode(hookInfo, hookInfo.getAfterParsed(), param, "after");
                }
            };

            logger.info("开始应用普通Hook: " + hookInfo.getMethodName());
            UnhookHandle unhook;
            if (isConstructor) {
                if (paramTypes.length == 0) {
                    unhook = HookAPI.findAndHookConstructor(targetClass, methodHook);
                } else {
                    Object[] args = new Object[paramTypes.length + 1];
                    System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
                    args[paramTypes.length] = methodHook;
                    unhook = HookAPI.findAndHookConstructor(targetClass, args);
                }
            } else {
                if (paramTypes.length == 0) {
                    unhook = HookAPI.findAndHookMethod(targetClass, hookInfo.getMethodName(), methodHook);
                } else {
                    Object[] args = new Object[paramTypes.length + 1];
                    System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
                    args[paramTypes.length] = methodHook;
                    unhook = HookAPI.findAndHookMethod(targetClass, hookInfo.getMethodName(), args);
                }
            }
            activeHooks.put(hookInfo.getId(), unhook);
            logger.info("普通Hook应用成功: " + hookInfo.getId());
        }

        hookInfo.setActive(true);
        logger.info("Hook状态设置为活跃: " + hookInfo.getId());
    }

    private static void executeHookCode(HookInfo hookInfo, List<ASTNode> nodes,
                                        HookParam param, String phase) {
        executeHookCodeWithReturnFlag(hookInfo, nodes, param, phase, null);
    }

    private static void executeHookCodeWithReturnFlag(HookInfo hookInfo, List<ASTNode> nodes,
                                        HookParam param, String phase,
                                        AtomicBoolean returnValueSet) {
        if (nodes == null || nodes.isEmpty()) return;

        String prefix = "[" + hookInfo.getId() + "][" + phase + "] ";
        ICommandOutputHandler outputHandler = outputHandlers.computeIfAbsent(hookInfo.getId(), k -> new HookOutputHandler(logger, prefix));
        ICommandOutputHandler errorHandler = errorHandlers.computeIfAbsent(hookInfo.getId(), k -> new HookOutputHandler(logger, prefix));
        SystemOutputRedirector redirector = new SystemOutputRedirector(outputHandler, errorHandler);
        redirector.startRedirect();
        try {
            ClassLoader cl = hookInfo.getClassLoader();
            ScriptRunner runner = scriptRunners.computeIfAbsent(
                hookInfo.getId(), k -> {
                    ScriptRunner r = new ScriptRunner(cl);
                    r.setClassFinder(new AppClassFinder());
                    return r;
                });
            logger.debug("运行预编译AST, hook id = " + hookInfo.getId());

            EvalContext evalContext = runner.getEvalContext();

            // executeNodes 在脚本模式下会内部重置会话（ParseContext + 变量），
            // 那样在它之前 addImport 的内容会被冲掉。这里改为自己控制重置时机：
            // 先重置，再配置 import / builtin，随后执行阶段不再重置。
            runner.setReplMode(true);
            runner.resetSession();

            for (String item : imports) runner.addImport(item);
            addHookBuiltIn(evalContext, param, getLoadPackageParam(), hookInfo, phase, returnValueSet);
            runner.executeNodes(nodes, outputHandler, errorHandler);
        } catch (Exception e) {
            logger.error("Hook代码执行失败: " + hookInfo.getId(), e);
        } finally {
            redirector.stopRedirect();
        }
    }

    private static String loadCodeFromCodebase(String codebase) {
        try {
            File scriptFile;


            if (!codebase.contains("/") && !codebase.contains("\\")) {
                // 名字带不带 .java 后缀都认：脚本目录里的文件是「名字 + .java」，
                // 而命令参数里大家习惯写裸名字
                scriptFile = DataBridge.resolveScriptFile(codebase);

                if (scriptFile.exists()) {
                    logger.info("从codebase目录加载脚本: " + codebase);
                } else {
                    logger.warn("codebase目录中未找到脚本: " + codebase);
                    return null;
                }
            } else {
                String fileName = new File(codebase).getName();
                scriptFile = new File(DataBridge.getScriptsDirectory(), fileName);

                if (scriptFile.exists()) {
                    logger.info("从codebase目录加载脚本（使用文件名）: " + fileName);
                } else {
                    scriptFile = new File(codebase);
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
            Method[] methods = targetClass.getDeclaredMethods();
            Method matchedMethod = null;

            for (Method method : methods) {
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
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            Constructor<?> matchedConstructor = null;

            for (Constructor<?> constructor : constructors) {
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

    public static boolean removeHook(String hookId) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            return false;
        }

        UnhookHandle unhook = activeHooks.remove(hookId);
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
        return true;
    }

    /**
     * 按 ID 移除 Hook，并把结果告诉调用方。
     * <p>
     * 找不到时返回 false 而不是安静地当成功 —— 以前这里只打一行日志就走了，调用方照样报
     * 「移除成功」，于是 {@code hook remove 随便编一个ID} 也能「成功」。
     * </p>
     *
     * @return 真的移除了才为 true
     */
    public static boolean removeHook(String hookId, CommandExecutor.CmdExecContext<?> ctx) {
        if (!removeHook(hookId)) {
            ctx.print(HookTexts.ERR_HOOK_NOT_FOUND.text(), Colors.RED);
            ctx.println(hookId, Colors.YELLOW);
            return false;
        }

        ctx.print(Text.zhEn("Hook移除成功: ", "Hook removed: ").text(), Colors.LIGHT_GREEN);
        ctx.println(hookId, Colors.YELLOW);
        return true;
    }

    public static void listHooks(CommandExecutor.CmdExecContext<?> ctx) {
        if (hooks.isEmpty()) {
            ctx.println(Text.zhEn("没有活动的Hook", "No active hooks").text(), Colors.GRAY);
            return;
        }

        ctx.println(Text.zhEn("===== Hook列表 =====", "===== Hook list =====").text(), Colors.CYAN);
        ctx.println("");

        for (HookInfo hookInfo : hooks.values()) {
            hookInfo.printDisplayInfo(ctx);
            ctx.println("------------------------", Colors.GRAY);
            ctx.println("");
        }

        ctx.print(Text.zhEn("总计: ", "Total: ").text(), Colors.CYAN);
        ctx.print(String.valueOf(hooks.size()), Colors.YELLOW);
        ctx.println(Text.zhEn(" 个Hook", " hooks").text(), Colors.CYAN);
    }

    public static void getHookInfo(String hookId, CommandExecutor.CmdExecContext<?> ctx) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            ctx.print(HookTexts.ERR_HOOK_NOT_FOUND.text(), Colors.RED);
            ctx.println(hookId, Colors.YELLOW);
            return;
        }

        hookInfo.printDisplayInfo(ctx);
    }

    public static boolean enableHook(String hookId, CommandExecutor.CmdExecContext<?> ctx) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            ctx.print(HookTexts.ERR_HOOK_NOT_FOUND.text(), Colors.RED);
            ctx.println(hookId, Colors.YELLOW);
            return false;
        }

        hookInfo.setEnabled(true);
        logger.info("启用Hook: " + hookId);
        ctx.print(Text.zhEn("Hook已启用: ", "Hook enabled: ").text(), Colors.LIGHT_GREEN);
        ctx.println(hookId, Colors.YELLOW);
        return true;
    }

    public static boolean disableHook(String hookId, CommandExecutor.CmdExecContext<?> ctx) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            ctx.print(HookTexts.ERR_HOOK_NOT_FOUND.text(), Colors.RED);
            ctx.println(hookId, Colors.YELLOW);
            return false;
        }

        hookInfo.setEnabled(false);
        logger.info("禁用Hook: " + hookId);
        ctx.print(Text.zhEn("Hook已禁用: ", "Hook disabled: ").text(), Colors.GRAY);
        ctx.println(hookId, Colors.YELLOW);
        return true;
    }

    public static void getHookOutput(String hookId, CommandExecutor.CmdExecContext<?> ctx, int count) {
        HookInfo hookInfo = hooks.get(hookId);
        if (hookInfo == null) {
            ctx.print(HookTexts.ERR_HOOK_NOT_FOUND.text(), Colors.RED);
            ctx.println(hookId, Colors.YELLOW);
            return;
        }
        ctx.println(Text.zhEn("Hook基本信息: ", "Hook info: ").text(), Colors.CYAN);

        ctx.println("------------------------", Colors.GRAY);
        ctx.print("HookID: ", Colors.CYAN);
        ctx.println(hookId, Colors.GREEN);
        ctx.print(Text.zhEn("Hook状态: ", "Hook status: ").text(), Colors.CYAN);
        ctx.println(hookInfo.isActive() ? Text.zhEn("已激活", "Active").text()
                        : Text.zhEn("未激活", "Inactive").text(),
                hookInfo.isActive() ? Colors.GREEN : Colors.RED);
        ctx.println("------------------------", Colors.GRAY);
        ctx.println("");
        ctx.println("");
        ICommandOutputHandler outputHandler = outputHandlers.get(hookId);
        ICommandOutputHandler errorHandler = errorHandlers.get(hookId);


        ctx.println(Text.zhEn("============ 输出 ============", "============ Output ============").text(),
                Colors.GREEN);
        if (outputHandler != null) {
            ctx.println(Arrays.stream(outputHandler.getString().split("\n"))
                            .limit(count)
                            .collect(Collectors.joining("\n")));
        } else {
            ctx.println(Text.zhEn("还没有被执行过，没有输出", "Not invoked yet, no output").text(), Colors.GRAY);
        }
        ctx.println("=============================", Colors.GRAY);
        ctx.println("");
        if (errorHandler != null && !errorHandler.getString().isEmpty()) {
            ctx.println(Text.zhEn("============ 错误输出 ============", "============ Error output ============").text(),
                    Colors.ORANGE);
            ctx.println(Arrays.stream(errorHandler.getString().split("\n"))
                            .limit(count)
                            .collect(Collectors.joining("\n")));
            ctx.println("=============================", Colors.GRAY);
        }

    }

    public static int getHookCount() {
        return hooks.size();
    }

    public static void clearAllHooks() {
        for (String hookId : hooks.keySet()) {
            removeHook(hookId);
        }
        logger.info("所有Hook已清除");
    }

    public static List<Map<String, Object>> getAllHooksAsMap() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (HookInfo info : hooks.values()) {
            result.add(info.toMap());
        }
        return result;
    }

    public static List<HookAddResult.HookDetailInfo> getHookInfoDetail(String hookId) {
        HookInfo info = hooks.get(hookId);
        if (info == null) return null;

        List<HookAddResult.HookDetailInfo> detailList = new ArrayList<>();
        detailList.add(new HookAddResult.HookDetailInfo("ID", info.getId()));
        detailList.add(new HookAddResult.HookDetailInfo("ClassName", info.getClassName()));
        detailList.add(new HookAddResult.HookDetailInfo("MethodName", info.getMethodName()));
        if (info.getSignature() != null && !info.getSignature().isEmpty()) {
            detailList.add(new HookAddResult.HookDetailInfo("Signature", info.getSignature()));
        }
        detailList.add(new HookAddResult.HookDetailInfo("CallCount", String.valueOf(info.getCallCount())));
        detailList.add(new HookAddResult.HookDetailInfo("Active", String.valueOf(info.isActive())));
        detailList.add(new HookAddResult.HookDetailInfo("Enabled", String.valueOf(info.isEnabled())));
        detailList.add(new HookAddResult.HookDetailInfo("CreateTime", String.valueOf(info.getCreateTime())));

        if (info.hasBefore()) {
            String code = info.getBeforeCode() != null ? info.getBeforeCode() : "[codebase: " + info.getBeforeCodebase() + "]";
            detailList.add(new HookAddResult.HookDetailInfo("BeforeCode", code));
        }
        if (info.hasAfter()) {
            String code = info.getAfterCode() != null ? info.getAfterCode() : "[codebase: " + info.getAfterCodebase() + "]";
            detailList.add(new HookAddResult.HookDetailInfo("AfterCode", code));
        }
        if (info.hasReplace()) {
            String code = info.getReplaceCode() != null ? info.getReplaceCode() : "[codebase: " + info.getReplaceCodebase() + "]";
            detailList.add(new HookAddResult.HookDetailInfo("ReplaceCode", code));
        }

        return detailList;
    }

    public static List<HookAddResult.HookDetailInfo> getHookOutputDetail(String hookId, int count) {
        HookInfo info = hooks.get(hookId);
        if (info == null) return null;

        List<HookAddResult.HookDetailInfo> detailList = new ArrayList<>();

        ICommandOutputHandler outputHandler = outputHandlers.get(hookId);
        ICommandOutputHandler errorHandler = errorHandlers.get(hookId);

        if (outputHandler != null && !outputHandler.getString().isEmpty()) {
            String[] lines = outputHandler.getString().split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(count, lines.length); i++) {
                sb.append(lines[i]).append("\n");
            }
            detailList.add(new HookAddResult.HookDetailInfo("Output", sb.toString()));
        } else {
            detailList.add(new HookAddResult.HookDetailInfo("Output", "(no output yet)"));
        }

        if (errorHandler != null && !errorHandler.getString().isEmpty()) {
            String[] lines = errorHandler.getString().split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(count, lines.length); i++) {
                sb.append(lines[i]).append("\n");
            }
            detailList.add(new HookAddResult.HookDetailInfo("ErrorOutput", sb.toString()));
        }

        return detailList;
    }
}
