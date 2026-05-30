package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.ScriptBaseRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermDenyRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermGrantRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermListRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermPresetRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermResetRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptPermShowConfigRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.javainterpreter.security.PermissionType;
import com.justnothing.javainterpreter.security.SandboxConfig;

@SubCommandInfo(
    description = "管理脚本执行权限配置, 支持授权/拒绝/预设/重置/列表/查看",
    usage = "script permission <grant|deny|preset|reset|list|show-config> [args]",
    examples = {
        "script permission list",
        "script permission show-config",
        "script permission grant file.read,network",
        "script permission deny reflection,thread",
        "script permission preset sandbox",
        "script permission reset"
    },
    optionsDesc = """
            子命令:
              grant <PERM1,PERM2,...>  - 授予指定权限
              deny <PERM1,PERM2,...>   - 拒绝指定权限
              preset <name>            - 应用预设配置
              reset                    - 重置为默认(无限制)
              list                     - 列出所有可用权限和预设
              show-config              - 显示当前权限配置状态
            """
)
public class ScriptPermissionCommand extends AbstractScriptCommand<ScriptBaseRequest, ScriptResult> {

    public ScriptPermissionCommand() {
    }

    @Override
    protected ScriptResult executeInternal(ScriptBaseRequest request) throws Exception {
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
        result.setOutput("未知的权限请求类型");
        return result;
    }

    protected ScriptResult handlePermission(String permList, boolean grant) {
        ScriptResult result = new ScriptResult();
        result.setSubCommand(grant ? "grant" : "deny");

        if (permList == null || permList.isEmpty()) {
            this.context.println("用法: script permission " + (grant ? "grant" : "deny") + " <PERM1,PERM2,...>", Colors.GRAY);
            this.context.println("可用权限: " + getPermissionList(), Colors.GRAY);
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

        this.context.println("===== 当前权限配置 =====", Colors.CYAN);
        this.context.println("", Colors.WHITE);

        if (config == null) {
            this.context.println("  未配置权限限制 (完全权限)", Colors.GREEN);
            r.setSuccess(true);
            r.setPermissionMask(0x1FFL);
            return r;
        }

        this.context.print("  磁盘读取: ", Colors.CYAN);
        this.context.println(config.isDiskReadAllowed() ? "允许" : "禁止",
                config.isDiskReadAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  磁盘写入: ", Colors.CYAN);
        this.context.println(config.isDiskWriteAllowed() ? "允许" : "禁止",
                config.isDiskWriteAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  网络操作: ", Colors.CYAN);
        this.context.println(config.isNetworkAllowed() ? "允许" : "禁止", config.isNetworkAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  创建线程: ", Colors.CYAN);
        this.context.println(config.isThreadCreateAllowed() ? "允许" : "禁止",
                config.isThreadCreateAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  创建进程: ", Colors.CYAN);
        this.context.println(config.isProcessCreateAllowed() ? "允许" : "禁止",
                config.isProcessCreateAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  反射操作: ", Colors.CYAN);
        this.context.println(config.isReflectionAllowed() ? "允许" : "禁止",
                config.isReflectionAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  系统退出: ", Colors.CYAN);
        this.context.println(config.isSystemExitAllowed() ? "允许" : "禁止",
                config.isSystemExitAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  系统属性: ", Colors.CYAN);
        this.context.println(config.isSystemPropertyAllowed() ? "允许" : "禁止",
                config.isSystemPropertyAllowed() ? Colors.GREEN : Colors.RED);
        this.context.print("  类加载器: ", Colors.CYAN);
        this.context.println(config.isClassLoaderAllowed() ? "允许" : "禁止",
                config.isClassLoaderAllowed() ? Colors.GREEN : Colors.RED);

        this.context.println("", Colors.WHITE);
        this.context.println("使用 'script permission grant/deny <PERM>' 修改", Colors.GRAY);
        this.context.println("使用 'script permission preset <name>' 应用预设", Colors.GRAY);

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
                    .allowThreadCreate().allowProcessCreate().allowReflection()
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
                case "exec", "process", "process.create" -> {
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
                    this.context.print("  未知权限: ", Colors.RED);
                    this.context.println(p, Colors.YELLOW);
                }
            }
        }

        currentPermissionConfig.set(builder.build());
        this.context.print((grant ? "已授予" : "已拒绝") + " " + count + " 项权限", Colors.GREEN);
        this.context.println(" (" + permList + ")", Colors.GRAY);
    }

    protected ScriptResult applyPreset(String preset) {
        SandboxConfig config;

        switch (preset.toLowerCase()) {
            case "sandbox" -> {
                config = SandboxConfig.DEFAULT;
                this.context.println("已应用预设: sandbox (沙箱模式)", Colors.GREEN);
            }
            case "expression" -> {
                config = SandboxConfig.EXPRESSION_ONLY;
                this.context.println("已应用预设: expression (表达式模式)", Colors.GREEN);
            }
            case "minimal" -> {
                config = SandboxConfig.MINIMAL;
                this.context.println("已应用预设: minimal (最小权限)", Colors.GREEN);
            }
            case "full" -> {
                config = null;
                this.context.println("已应用预设: full (完全权限)", Colors.GREEN);
            }
            default -> {
                this.context.print("未知预设: ", Colors.RED);
                this.context.println(preset, Colors.YELLOW);
                this.context.println("可用预设: sandbox, expression, minimal, full", Colors.GRAY);

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
        this.context.println("权限配置已重置为默认 (无限制)", Colors.GREEN);

        ScriptResult result = new ScriptResult();
        result.setSuccess(true);
        result.setPermissionMask(0x1FFL);
        return result;
    }

    protected ScriptResult handleList() {
        this.context.println("可用权限类型:", Colors.CYAN);
        for (PermissionType pt : PermissionType.values()) {
            this.context.print("  " + pt.getId(), Colors.YELLOW);
            this.context.println(" - " + pt.getDescription(), Colors.GRAY);
        }
        this.context.println("", Colors.WHITE);
        this.context.println("预设:", Colors.CYAN);
        this.context.print("  sandbox    ", Colors.YELLOW);
        this.context.println("- 沙箱模式 (禁止文件/网络/线程/反射)", Colors.GRAY);
        this.context.print("  expression ", Colors.YELLOW);
        this.context.println("- 表达式模式 (仅允许计算)", Colors.GRAY);
        this.context.print("  minimal    ", Colors.YELLOW);
        this.context.println("- 最小权限 (允许读文件)", Colors.GRAY);
        this.context.print("  full       ", Colors.YELLOW);
        this.context.println("- 完全权限 (无限制)", Colors.GRAY);

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
        if (config == null) return 0x1FFL;
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
        return mask;
    }
}
