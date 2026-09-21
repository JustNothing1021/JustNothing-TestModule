package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.ScriptBaseRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermDenyRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermGrantRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermListRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermPresetRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermResetRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermShowConfigRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.engine.security.PermissionType;
import com.justnothing.engine.security.SandboxConfig;

@SubCommandInfo(
    description = ScriptTexts.SUB_SCRIPT_PERMISSION_DESC,
    usage = "script permission <grant|deny|preset|reset|list|show-config> [args]",
    examples = {
        "script permission list",
        "script permission show-config",
        "script permission grant file.read,network",
        "script permission deny reflection,thread",
        "script permission preset sandbox",
        "script permission reset"
    },
    optionsDesc = ScriptTexts.SUB_SCRIPT_PERMISSION_OPTIONS
)
public class ScriptPermissionCommand extends AbstractScriptCommand<ScriptBaseRequest<?>, ScriptResult> {

    @SuppressWarnings("unchecked")
    public ScriptPermissionCommand() {
        super("script permission", (Class) ScriptBaseRequest.class, ScriptResult.class);
    }

    @Override
    protected ScriptResult executeRequest(ScriptBaseRequest<?> request) throws Exception {
        if (request instanceof ScriptPermGrantRequest grantReq) {
            return handlePermission(grantReq.getPermissions(), true);
        }
        if (request instanceof ScriptPermDenyRequest denyReq) {
            return handlePermission(denyReq.getPermissions(), false);
        }
        if (request instanceof ScriptPermPresetRequest presetReq) {
            return applyPreset(presetReq.getPresetName());
        }
        if (request instanceof ScriptPermResetRequest) {
            return handleReset();
        }
        if (request instanceof ScriptPermListRequest) {
            return handleList();
        }
        if (request instanceof ScriptPermShowConfigRequest) {
            return showPermissionStatus();
        }

        ScriptResult result = new ScriptResult(request.getRequestId());
        result.setSuccess(false);
        result.setOutput(Text.zhEn("未知的权限请求类型", "Unknown permission request type").text());
        return result;
    }

    protected ScriptResult handlePermission(String permList, boolean grant) {
        ScriptResult result = new ScriptResult();
        result.setSubCommand(grant ? "grant" : "deny");

        if (permList == null || permList.isEmpty()) {
            this.context.println(CliMessages.HELP_USAGE_INLINE.text()
                    + "script permission " + (grant ? "grant" : "deny") + " <PERM1,PERM2,...>", Colors.GRAY);
            this.context.println(Text.zhEn("可用权限: ", "Available permissions: ").text() + getPermissionList(), Colors.GRAY);
            result.setSuccess(false);
            return result;
        }

        modifyPermissions(permList, grant);

        result.setSuccess(true);
        result.setPermissionMask(buildPermissionMask(currentPermissionConfig.get()));
        return result;
    }

    protected ScriptResult showPermissionStatus() {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("show-config");
        SandboxConfig config = currentPermissionConfig.get();

        this.context.println(Text.zhEn("===== 当前权限配置 =====", "===== Current permission configuration =====").text(), Colors.CYAN);
        this.context.println("", Colors.WHITE);

        if (config == null) {
            this.context.println(Text.zhEn("  未配置权限限制 (完全权限)",
                    "  No permission restrictions configured (full permissions)").text(), Colors.GREEN);
            r.setSuccess(true);
            r.setPermissionMask(0x1FFL);
            return r;
        }

        this.context.print(Text.zhEn("  磁盘读取: ", "  Disk read: ").text(), Colors.CYAN);
        this.context.println(config.isDiskReadAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isDiskReadAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  磁盘写入: ", "  Disk write: ").text(), Colors.CYAN);
        this.context.println(config.isDiskWriteAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isDiskWriteAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  网络操作: ", "  Network: ").text(), Colors.CYAN);
        this.context.println(config.isNetworkAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isNetworkAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  创建线程: ", "  Thread creation: ").text(), Colors.CYAN);
        this.context.println(config.isThreadCreateAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isThreadCreateAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  创建进程: ", "  Process creation: ").text(), Colors.CYAN);
        this.context.println(config.isProcessCreateAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isProcessCreateAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  反射操作: ", "  Reflection: ").text(), Colors.CYAN);
        this.context.println(config.isReflectionAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isReflectionAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  系统退出: ", "  System exit: ").text(), Colors.CYAN);
        this.context.println(config.isSystemExitAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isSystemExitAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  系统属性: ", "  System properties: ").text(), Colors.CYAN);
        this.context.println(config.isSystemPropertyAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isSystemPropertyAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print(Text.zhEn("  类加载器: ", "  Class loader: ").text(), Colors.CYAN);
        this.context.println(config.isClassLoaderAllowed() ? ScriptTexts.PERMISSION_ALLOWED.text() : ScriptTexts.PERMISSION_DENIED.text(),
                config.isClassLoaderAllowed() ? Colors.GREEN : Colors.RED);

        this.context.println("", Colors.WHITE);
        this.context.println(Text.zhEn("使用 'script permission grant/deny <PERM>' 修改",
                "use 'script permission grant/deny <PERM>' to change them").text(), Colors.GRAY);
        this.context.println(Text.zhEn("使用 'script permission preset <name>' 应用预设",
                "use 'script permission preset <name>' to apply a preset").text(), Colors.GRAY);

        r.setSuccess(true);
        r.setPermissionMask(buildPermissionMask(config));
        return r;
    }

    protected void modifyPermissions(String permList, boolean grant) {
        SandboxConfig current = currentPermissionConfig.get();
        SandboxConfig.Builder builder = SandboxConfig.builder();

        if (current != null) {
            if (current.isDiskReadAllowed())
                builder.allowDiskRead();
            else
                builder.denyDiskRead();
            if (current.isDiskWriteAllowed())
                builder.allowDiskWrite();
            else
                builder.denyDiskWrite();
            if (current.isNetworkAllowed())
                builder.allowNetwork();
            else
                builder.denyNetwork();
            if (current.isThreadCreateAllowed())
                builder.allowThreadCreate();
            else
                builder.denyThreadCreate();
            if (current.isProcessCreateAllowed())
                builder.allowProcessCreate();
            else
                builder.denyProcessCreate();
            if (current.isExecAllowed())
                builder.allowExec();
            else
                builder.denyExec();
            if (current.isReflectionAllowed())
                builder.allowReflection();
            else
                builder.denyReflection();
            if (current.isSystemExitAllowed())
                builder.allowSystemExit();
            else
                builder.denySystemExit();
            if (current.isSystemPropertyAllowed())
                builder.allowSystemProperty();
            else
                builder.denySystemProperty();
            if (current.isClassLoaderAllowed())
                builder.allowClassLoader();
            else
                builder.denyClassLoader();
        } else {
            builder.allowDiskRead().allowDiskWrite().allowNetwork()
                    .allowThreadCreate().allowProcessCreate().allowExec().allowReflection()
                    .allowSystemExit().allowSystemProperty().allowClassLoader();
        }

        String[] perms = permList.split(",");
        int count = 0;

        for (String perm : perms) {
            String p = perm.trim().toLowerCase();
            switch (p) {
                case "file.read", "disk.read" -> {
                    if (grant)
                        builder.allowDiskRead();
                    else
                        builder.denyDiskRead();
                    count++;
                }
                case "file.write", "disk.write" -> {
                    if (grant)
                        builder.allowDiskWrite();
                    else
                        builder.denyDiskWrite();
                    count++;
                }
                case "network" -> {
                    if (grant)
                        builder.allowNetwork();
                    else
                        builder.denyNetwork();
                    count++;
                }
                case "thread", "thread.create" -> {
                    if (grant)
                        builder.allowThreadCreate();
                    else
                        builder.denyThreadCreate();
                    count++;
                }
                case "exec", "run" -> {
                    if (grant)
                        builder.allowExec();
                    else
                        builder.denyExec();
                    count++;
                }
                case "process", "process.create" -> {
                    if (grant)
                        builder.allowProcessCreate();
                    else
                        builder.denyProcessCreate();
                    count++;
                }
                case "reflection" -> {
                    if (grant)
                        builder.allowReflection();
                    else
                        builder.denyReflection();
                    count++;
                }
                case "system.exit" -> {
                    if (grant)
                        builder.allowSystemExit();
                    else
                        builder.denySystemExit();
                    count++;
                }
                case "system.property" -> {
                    if (grant)
                        builder.allowSystemProperty();
                    else
                        builder.denySystemProperty();
                    count++;
                }
                case "classloader" -> {
                    if (grant)
                        builder.allowClassLoader();
                    else
                        builder.denyClassLoader();
                    count++;
                }
                default -> {
                    this.context.print(Text.zhEn("  未知权限: ", "  Unknown permission: ").text(), Colors.RED);
                    this.context.println(p, Colors.YELLOW);
                }
            }
        }

        currentPermissionConfig.set(builder.build());
        this.context.print((grant ? Text.zhEn("已授予", "Granted") : Text.zhEn("已拒绝", "Denied")).text()
                + " " + count + Text.zhEn(" 项权限", " permissions").text(), Colors.GREEN);
        this.context.println(" (" + permList + ")", Colors.GRAY);
    }

    protected ScriptResult applyPreset(String preset) {
        SandboxConfig config;

        switch (preset.toLowerCase()) {
            case "sandbox" -> {
                config = SandboxConfig.DEFAULT;
                this.context.println(Text.zhEn("已应用预设: sandbox (沙箱模式)",
                        "Applied preset: sandbox (sandbox mode)").text(), Colors.GREEN);
            }
            case "expression" -> {
                config = SandboxConfig.EXPRESSION_ONLY;
                this.context.println(Text.zhEn("已应用预设: expression (表达式模式)",
                        "Applied preset: expression (expression mode)").text(), Colors.GREEN);
            }
            case "minimal" -> {
                config = SandboxConfig.MINIMAL;
                this.context.println(Text.zhEn("已应用预设: minimal (最小权限)",
                        "Applied preset: minimal (minimal permissions)").text(), Colors.GREEN);
            }
            case "full" -> {
                config = null;
                this.context.println(Text.zhEn("已应用预设: full (完全权限)",
                        "Applied preset: full (full permissions)").text(), Colors.GREEN);
            }
            default -> {
                this.context.print(Text.zhEn("未知预设: ", "Unknown preset: ").text(), Colors.RED);
                this.context.println(preset, Colors.YELLOW);
                this.context.println(Text.zhEn("可用预设: sandbox, expression, minimal, full",
                        "Available presets: sandbox, expression, minimal, full").text(), Colors.GRAY);

                ScriptResult result = new ScriptResult();
                result.setSuccess(false);
                return result;
            }
        }

        currentPermissionConfig.set(config);

        ScriptResult result = new ScriptResult();
        result.setSuccess(true);
        result.setPermissionMask(buildPermissionMask(config));
        return result;
    }

    protected ScriptResult handleReset() {
        currentPermissionConfig.set(null);
        this.context.println(Text.zhEn("权限配置已重置为默认 (无限制)",
                "Permission configuration reset to the default (unrestricted)").text(), Colors.GREEN);

        ScriptResult result = new ScriptResult();
        result.setSuccess(true);
        result.setPermissionMask(0x1FFL);
        return result;
    }

    protected ScriptResult handleList() {
        this.context.println(Text.zhEn("可用权限类型:", "Available permission types:").text(), Colors.CYAN);
        for (PermissionType pt : PermissionType.values()) {
            this.context.print("  " + pt.getId(), Colors.YELLOW);
            this.context.println(" - " + pt.getDescription(), Colors.GRAY);
        }
        this.context.println("", Colors.WHITE);
        this.context.println(Text.zhEn("预设:", "Presets:").text(), Colors.CYAN);
        this.context.print("  sandbox    ", Colors.YELLOW);
        this.context.println(Text.zhEn("- 沙箱模式 (禁止文件/网络/线程/反射)",
                "- sandbox (no file, network, thread or reflection access)").text(), Colors.GRAY);
        this.context.print("  expression ", Colors.YELLOW);
        this.context.println(Text.zhEn("- 表达式模式 (仅允许计算)",
                "- expression (arithmetic only)").text(), Colors.GRAY);
        this.context.print("  minimal    ", Colors.YELLOW);
        this.context.println(Text.zhEn("- 最小权限 (允许读文件)",
                "- minimal (file reads allowed)").text(), Colors.GRAY);
        this.context.print("  full       ", Colors.YELLOW);
        this.context.println(Text.zhEn("- 完全权限 (无限制)",
                "- full (unrestricted)").text(), Colors.GRAY);

        ScriptResult result = new ScriptResult();
        result.setSuccess(true);
        return result;
    }

    private ScriptResult okResult(String subCmd) {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand(subCmd);
        return r;
    }

    private static long buildPermissionMask(SandboxConfig config) {
        if (config == null) return 0x3FFL;
        long mask = 0L;
        if (config.isDiskReadAllowed()) mask |= 0x001L;
        if (config.isDiskWriteAllowed()) mask |= 0x002L;
        if (config.isNetworkAllowed()) mask |= 0x004L;
        if (config.isThreadCreateAllowed()) mask |= 0x008L;
        if (config.isProcessCreateAllowed()) mask |= 0x010L;
        if (config.isReflectionAllowed()) mask |= 0x020L;
        if (config.isSystemExitAllowed()) mask |= 0x040L;
        if (config.isSystemPropertyAllowed()) mask |= 0x080L;
        if (config.isClassLoaderAllowed()) mask |= 0x100L;
        if (config.isExecAllowed()) mask |= 0x200L;
        return mask;
    }
}
