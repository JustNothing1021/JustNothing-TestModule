package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.justnothing.engine.ScriptRunner;
import com.justnothing.engine.security.SandboxConfig;
import com.justnothing.engine.security.PermissionType;
import com.justnothing.engine.codegen.DynamicClassGenerator;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.testmodule.utils.data.DataBridge;

public abstract class AbstractScriptCommand<Req extends CommandRequest<?>, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    public static final Logger logger = Logger.getLoggerForName("Script");
    public static final ConcurrentHashMap<ClassLoader, ScriptRunner> scriptRunners = new ConcurrentHashMap<>();
    public static final ScriptRunner systemScriptRunner;
    public static final AtomicReference<SandboxConfig> currentPermissionConfig = new AtomicReference<>(null);

    static {
        DynamicClassGenerator.setDefaultClassDefiner(DexClassDefiner.getInstance());
        systemScriptRunner = new ScriptRunner(null);
        systemScriptRunner.setClassFinder(new AppClassFinder());
    }

    protected CommandExecutor.CmdExecContext<Req> context;

    protected AbstractScriptCommand(String commandName, Class<Req> requestType, Class<Res> returnType) {
        super(commandName, requestType, returnType);
    }

    protected void out(Object obj, byte color) {
        if (context != null) context.print(obj, color);
    }

    protected void outln(Object obj, byte color) {
        if (context != null) context.println(obj, color);
    }

    protected ScriptRunner getScriptExecutor(ClassLoader cl) {
        if (cl == null) return systemScriptRunner;
        return scriptRunners.computeIfAbsent(cl, ScriptRunner::new);
    }

    /**
     * 基类负责把"执行上下文"适配成"请求对象"，子类只关心自己的请求类型。
     * 异常兜底、请求类型护栏由 {@link AbstractCommand} 统一提供
     * （此前这里是把异常包成 RuntimeException 直接往外抛，现在统一成返回失败结果）。
     */
    @Override
    protected final Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        this.context = context;
        try {
            return executeRequest(context.getRequest());
        } finally {
            this.context = null;
        }
    }

    protected abstract Res executeRequest(Req request) throws Exception;

    protected String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        else return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    protected String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    protected boolean isValidScriptName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    protected String getPermissionList() {
        StringBuilder sb = new StringBuilder();
        for (PermissionType pt : PermissionType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pt.getId());
        }
        return sb.toString();
    }

    protected File getScriptsDirectory() {
        return DataBridge.getScriptsDirectory();
    }
}