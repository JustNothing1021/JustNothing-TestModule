package com.justnothing.methodsclient.metadata;

import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.alias.AliasMain;
import com.justnothing.testmodule.command.functions.agent.AgentCliMain;
import com.justnothing.testmodule.command.functions.bytecode.impl.BytecodeMain;
import com.justnothing.testmodule.command.functions.breakpoint.impl.BreakpointMain;
import com.justnothing.testmodule.command.functions.bsh.impl.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.help.HelpMain;
import com.justnothing.testmodule.command.functions.hook.HookMain;
import com.justnothing.testmodule.command.functions.memory.MemoryMain;
import com.justnothing.testmodule.command.functions.nativecmd.NativeMain;
import com.justnothing.testmodule.command.functions.network.NetworkMain;
import com.justnothing.testmodule.command.functions.packages.PackagesMain;
import com.justnothing.testmodule.command.functions.performance.PerformanceMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.system.SystemMain;
import com.justnothing.testmodule.command.functions.threads.ThreadsMain;
import com.justnothing.testmodule.command.functions.trace.TraceMain;
import com.justnothing.testmodule.command.functions.watch.WatchMain;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 命令元数据扫描器。
 * <p>
 * 从 {@link CommandRouter} 的注册表和 {@code @Cmd} / {@code @CmdRoutes} / {@code @CmdParam}
 * 注解中提取完整的命令结构信息，用于驱动 JLine Completer / TailTip / Highlighter。
 * <p>
 * 由于客户端和服务端在同一 JAR 中，直接使用反射读取注解，零网络开销。
 *
 * <h3>输出数据模型</h3>
 * <pre>{@code
 * CommandMeta {
 *   name: "class"
 *   description: "类信息查询"
 *   routes: [
 *     RouteMeta { path: "info", description: "查看类的完整信息",
 *       params: [
 *         ParamMeta { name: "--class", aliases: ["-c"], description: "目标类名",
 *                    required: true, allowedValues: [], position: 1 },
 *         ...
 *       ]
 *     },
 *     RouteMeta { path: "list", ... },
 *   ]
 * }
 * }</pre>
 */
public final class CommandMetadataScanner {

    private static final Logger logger =
            Logger.getLoggerForName("CmdMetaScanner");

    /** 缓存：避免重复反射扫描 */
    private static volatile List<CommandMeta> cachedMetadata = null;

    private CommandMetadataScanner() {}

    // ==================== 公开 API ====================

    /**
     * 扫描所有已注册命令的完整元数据。
     * 结果会被缓存，后续调用直接返回缓存（除非调用 {@link #invalidateCache()}）。
     *
     * @return 所有命令的元数据列表（按命令名字母序排列）
     */
    public static List<CommandMeta> scanAllCommands() {
        if (cachedMetadata != null) {
            return cachedMetadata;
        }

        synchronized (CommandMetadataScanner.class) {
            if (cachedMetadata != null) {
                return cachedMetadata;
            }

            long start = System.currentTimeMillis();
            List<CommandMeta> result = doScan();
            cachedMetadata = Collections.unmodifiableList(result);
            long elapsed = System.currentTimeMillis() - start;
            logger.info("命令元数据扫描完成: %d 个命令, 耗时 %dms", result.size(), elapsed);
            return cachedMetadata;
        }
    }

    /**
     * 清除缓存，下次调用 {@link #scanAllCommands()} 时重新扫描。
     */
    public static void invalidateCache() {
        cachedMetadata = null;
    }

    // ==================== 内部实现 ====================

    /**
     * 确保所有命令类已注册到 CommandRouter 的 pathRegistry 中。
     * <p>
     * 客户端进程不会执行服务端的 CommandExecutor.autoRegister()，
     * 因此 pathRegistry 初始为空。此方法通过反射注册所有已知命令类的路由元数据，
     * 使后续的注解扫描能正常工作。
     * <p>
     * registerCommand() 只读取 @Cmd/@CmdRoutes 注解并填充 map，
     * 不创建实例、不依赖 Android 服务端环境，客户端调用安全。
     */
    private static volatile boolean commandsRegistered = false;

    private static void ensureCommandsRegistered(CommandRouter router) {
        if (commandsRegistered) return;
        synchronized (CommandMetadataScanner.class) {
            if (commandsRegistered) return;

            // 与 CommandExecutor.autoRegister() 保持一致的命令列表
            Class<?>[] commandClasses = {
                HelpMain.class,
                WatchMain.class,
                TraceMain.class,
                ExportContextMain.class,
                MemoryMain.class,
                ThreadsMain.class,
                SystemMain.class,
                BreakpointMain.class,
                HookMain.class,
                BytecodeMain.class,
                NativeMain.class,
                PerformanceMain.class,
                AliasMain.class,
                NetworkMain.class,
                BeanShellExecutorMain.class,
                ScriptExecutorMain.class,
                ClassMain.class,
                PackagesMain.class,
                AgentCliMain.class,
            };

            for (Class<?> cmdClass : commandClasses) {
                try {
                    // registerCommand 接受 Class<? extends MainCommand<?>>，需要强制转换
                    @SuppressWarnings("unchecked")
                    var mainCmdClass = (Class<? extends com.justnothing.testmodule.command.base.MainCommand<?>>) cmdClass;
                    router.registerCommand(mainCmdClass);
                } catch (Exception e) {
                    logger.warn("注册命令元数据失败（已跳过）: " + cmdClass.getSimpleName() + " - " + e.getMessage());
                }
            }

            commandsRegistered = true;
            logger.info("客户端命令元数据注册完成: %d 个类", commandClasses.length);
        }
    }

    private static List<CommandMeta> doScan() {
        CommandRouter router = CommandRouter.getInstance();

        // 客户端进程中 CommandRouter 的 pathRegistry 是空的（命令只在服务端注册）。
        // 这里先执行一次元数据注册，只填充路由信息到 pathRegistry，不创建命令实例。
        ensureCommandsRegistered(router);

        // 1. 收集所有顶层命令名
        Set<String> topLevelCommands = new TreeSet<>();
        for (String fullPath : router.getPathRegistry().keySet()) {
            String topCommand = extractTopLevelCommand(fullPath);
            topLevelCommands.add(topCommand);
        }

        // 2. 对每个顶层命令构建 CommandMeta
        List<CommandMeta> commands = new ArrayList<>();
        for (String cmdName : topLevelCommands) {
            CommandMeta meta = buildCommandMeta(router, cmdName);
            if (meta != null) {
                commands.add(meta);
            }
        }

        return commands;
    }

    private static String extractTopLevelCommand(String fullPath) {
        int colonIdx = fullPath.indexOf(':');
        return colonIdx > 0 ? fullPath.substring(0, colonIdx) : fullPath;
    }

    private static CommandMeta buildCommandMeta(CommandRouter router, String cmdName) {
        List<CommandRouter.RouteConfig> routes = router.getRoutesForCommand(cmdName);
        if (routes.isEmpty()) {
            return null;
        }

        // 尝试从 @Cmd 注解获取描述
        String description = findCommandDescription(cmdName);

        // 构建子路由列表
        List<RouteMeta> routeMetas = new ArrayList<>();
        for (CommandRouter.RouteConfig route : routes) {
            RouteMeta routeMeta = buildRouteMeta(route);
            routeMetas.add(routeMeta);
        }

        return new CommandMeta(cmdName, description, routeMetas);
    }

    private static String findCommandDescription(String cmdName) {
        // 遍历已注册的 handler 类找 @Cmd 注解
        CommandRouter router = CommandRouter.getInstance();
        for (var entry : router.getPathRegistry().entrySet()) {
            if (entry.getKey().startsWith(cmdName + ":") || entry.getKey().equals(cmdName)) {
                var config = entry.getValue();
                try {
                    Cmd cmdAnnotation = config.handlerType().getAnnotation(Cmd.class);
                    if (cmdAnnotation != null && !cmdAnnotation.description().isEmpty()) {
                        return cmdAnnotation.description();
                    }
                } catch (Exception ignored) {}
                break; // 找到第一个匹配就停止
            }
        }
        return "";
    }

    private static RouteMeta buildRouteMeta(CommandRouter.RouteConfig route) {
        // 扫描 Request 类的所有 @CmdParam 字段
        List<ParamMeta> params = scanRequestParams(route.requestType());

        return new RouteMeta(
                route.path(),
                route.description(),
                route.requestType(),
                params
        );
    }

    /**
     * 反射扫描 Request 类的字段，提取所有 @CmdParam 注解信息
     */
    private static List<ParamMeta> scanRequestParams(Class<? extends CommandRequest> requestClass) {
        List<ParamMeta> paramMetas = new ArrayList<>();

        if (requestClass == null || requestClass == CommandRequest.class) {
            return paramMetas;
        }

        Field[] fields = requestClass.getDeclaredFields();
        for (Field field : fields) {
            CmdParam param = field.getAnnotation(CmdParam.class);
            if (param == null) {
                continue;
            }

            String paramName = buildParamName(param);
            paramMetas.add(new ParamMeta(
                    paramName,
                    Arrays.asList(param.aliases()),
                    param.description(),
                    param.required(),
                    param.position(),
                    param.varArgs(),
                    Arrays.asList(param.allowedValues()),
                    param.defaultValue()
            ));
        }

        // 排序：位置参数在前，flag 参数在后
        paramMetas.sort((a, b) -> {
            if (a.position() > 0 && b.position() > 0) return Integer.compare(a.position(), b.position());
            if (a.position() > 0) return -1;
            if (b.position() > 0) return 1;
            return a.name().compareTo(b.name());
        });

        return paramMetas;
    }

    /**
     * 构建参数显示名称：--name 或 -alias（取最短形式）
     */
    private static String buildParamName(CmdParam param) {
        String name = param.name();
        if (!name.startsWith("-")) {
            name = "--" + name;
        }
        return name;
    }

    // ==================== 数据模型（纯 POJO，供 Completer/TailTip 消费）====================

    /**
     * 顶层命令元数据（如 class, script, memory）
     */
    public record CommandMeta(
            String name,
            String description,
            List<RouteMeta> routes
    ) {
        /** 获取所有子路由路径名列表 */
        public Set<String> subRouteNames() {
            Set<String> names = new LinkedHashSet<>();
            for (RouteMeta r : routes) {
                names.add(r.path());
            }
            return names;
        }

        /** 获取所有已知参数名（合并所有路由的参数） */
        public Set<String> allParamNames() {
            Set<String> names = new LinkedHashSet<>();
            for (RouteMeta r : routes) {
                for (ParamMeta p : r.params()) {
                    names.add(p.name());
                    names.addAll(p.aliases());
                }
            }
            return names;
        }
    }

    /**
     * 子路由元数据（如 class:info, script:run）
     */
    public record RouteMeta(
            String path,
            String description,
            Class<?> requestType,
            List<ParamMeta> params
    ) {
        /** 获取此路由的参数名集合 */
        public Set<String> paramNames() {
            Set<String> names = new LinkedHashSet<>();
            for (ParamMeta p : params()) {
                names.add(p.name());
                names.addAll(p.aliases());
            }
            return names;
        }

        /** 获取有 allowedValues 的参数枚举映射 */
        public Map<String, List<String>> valueEnums() {
            Map<String, List<String>> enums = new LinkedHashMap<>();
            for (ParamMeta p : params()) {
                if (!p.allowedValues().isEmpty()) {
                    enums.put(p.name(), p.allowedValues());
                    for (String alias : p.aliases()) {
                        enums.put(alias, p.allowedValues());
                    }
                }
            }
            return enums;
        }
    }

    /**
     * 参数元数据（如 --class, -v, --format）
     */
    public record ParamMeta(
            String name,
            List<String> aliases,
            String description,
            boolean required,
            int position,
            boolean varArgs,
            List<String> allowedValues,
            String defaultValue
    ) {
        /** 是否为位置参数（position > 0） */
        public boolean isPositional() {
            return position > 0;
        }

        /** 是否为 flag 参数（无位置号且非 varArgs） */
        public boolean isFlag() {
            return position == 0 && !varArgs;
        }
    }
}
