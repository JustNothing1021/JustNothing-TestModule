package com.justnothing.methodsclient.completer;

import com.justnothing.methodsclient.metadata.CommandMetadataScanner;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.utils.logging.Logger;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.*;

/**
 * 基于命令系统元数据的智能补全器。
 * <p>
 * 数据来源：{@link CommandMetadataScanner} 反射扫描 {@code @Cmd} / {@code @CmdRoutes} / {@code @CmdParam}
 * 注解自动生成，与服务端命令注册表实时同步。无需手动维护。
 * <p>
 * 补全策略（按输入位置自动切换）：
 * <ol>
 *   <li>空行 / 首词 → 补全顶层命令名（class, memory, script...）</li>
 *   <li>command: → 补全子路由（如 class:info, class:list）</li>
 *   <li>command:sub -- → 补全参数名（如 --class, --verbose）</li>
 *   <li>command:sub --param= → 补全参数值（allowedValues 枚举）</li>
 * </ol>
 */
public class CommandCompleter implements Completer {

    // ==================== 扫描数据（启动时从注解自动填充）====================

    /** 顶层命令名集合 */
    private final Set<String> topLevelCommands = new TreeSet<>();

    /** 子路由映射：command → Set&lt;subRoute&gt; */
    private final Map<String, Set<String>> subCommands = new LinkedHashMap<>();

    /** 参数映射：fullPath(command:sub) → Set&lt;paramName&gt; */
    private final Map<String, Set<String>> routeParams = new LinkedHashMap<>();

    /** 参数值枚举：paramName → List&lt;allowedValue&gt; */
    private final Map<String, List<String>> paramValueEnums = new HashMap<>();

    /** 路由描述映射：fullPath → description（供 TailTip 使用） */
    private final Map<String, String> routeDescriptions = new LinkedHashMap<>();

    // ==================== 运行时扩展注册表（向后兼容）====================

    private final Set<String> customCommands = new HashSet<>();
    private final Map<String, Set<String>> customSubCommands = new HashMap<>();
    private final Map<String, Set<String>> customParams = new HashMap<>();
    private final Map<String, List<String>> customValueEnums = new HashMap<>();

    /** 是否已执行过扫描 */
    private boolean scanned = false;

    /** 首次 complete() 时的回调（用于在 readLine 上下文中延迟启用 TailTip 等 widget） */
    private Runnable firstUseCallback;

    public CommandCompleter() {
        // 延迟扫描（首次 complete 时触发），避免在构造函数中做重反射操作
    }

    /**
     * 注册首次使用回调。
     * 该回调会在 {@link #complete} 方法首次被调用时触发，
     * 此时一定处于 JLine readLine 上下文内，可以安全调用 widget 的 enable() 等方法。
     */
    public void setFirstUseCallback(Runnable callback) {
        this.firstUseCallback = callback;
    }

    // ==================== Completer 接口实现 ====================

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // 首次使用时触发扫描
        ensureScanned();

        // 首次 complete 时执行回调（此时一定在 readLine 上下文内，可安全启用 widget）
        if (firstUseCallback != null) {
            Runnable cb = firstUseCallback;
            firstUseCallback = null; // 只执行一次
            Logger.getLoggerForName("CmdCompleter").info("首次 complete() 触发，执行 firstUseCallback");
            cb.run();
        }

        String word = line.word();
        String fullLine = line.line().trim();

        if (word.isEmpty() && line.cursor() == 0) {
            addCandidates(candidates, topLevelCommands, "", "命令");
            return;
        }

        if (isCompletingTopLevel(fullLine, line.cursor())) {
            String prefix = word.toLowerCase();
            addCandidates(candidates, filterPrefix(topLevelCommands, prefix), prefix, "命令");
            addCandidates(candidates, filterPrefix(customCommands, prefix), prefix, "自定义命令");
        } else if (word.contains(":") && !word.startsWith("-")) {
            completeSubRoute(word, candidates);
        } else if (word.startsWith("-")) {
            completeParameter(fullLine, word, candidates);
        } else {
            String lowerWord = word.toLowerCase();
            addCandidates(candidates, filterPrefix(topLevelCommands, lowerWord), lowerWord, "命令");
        }
    }

    // ==================== 子路由补全 ====================

    private void completeSubRoute(String word, List<Candidate> candidates) {
        int colonIdx = word.indexOf(':');
        String cmdPart = word.substring(0, colonIdx).toLowerCase();
        String subPart = colonIdx + 1 < word.length() ? word.substring(colonIdx + 1) : "";

        Set<String> subs = getSubCommands(cmdPart);
        if (subs != null && !subs.isEmpty()) {
            String prefix = subPart.toLowerCase();
            addCandidates(candidates, filterPrefix(subs, prefix), prefix, "子命令");
        }
    }

    // ==================== 参数补全 ====================

    private void completeParameter(String fullLine, String word, List<Candidate> candidates) {
        if (!word.contains("=")) {
            String paramPrefix = word.toLowerCase();
            // 从当前路由上下文获取参数
            Set<String> params = getParamsForContext(fullLine);
            addCandidates(candidates, filterPrefix(params, paramPrefix), paramPrefix, "选项");
        } else {
            int eqIdx = word.indexOf('=');
            String paramName = word.substring(0, eqIdx).toLowerCase();
            String valuePrefix = eqIdx + 1 < word.length() ? word.substring(eqIdx + 1) : "";
            completeParameterValue(paramName, valuePrefix, candidates);
        }
    }

    private void completeParameterValue(String paramName, String valuePrefix, List<Candidate> candidates) {
        // 先查扫描到的枚举值
        List<String> values = paramValueEnums.get(paramName);
        if ((values == null || values.isEmpty()) && paramName.length() > 2) {
            // 尝试去掉前缀再查
            values = paramValueEnums.get("--" + paramName.substring(2));
        }

        // 查自定义枚举
        if (values == null || values.isEmpty()) {
            values = customValueEnums.get(paramName);
        }

        if (values != null && !values.isEmpty()) {
            addCandidates(candidates, filterPrefix(values, valuePrefix), valuePrefix, "值");
        }
    }

    // ==================== 上下文分析 ====================

    private boolean isCompletingTopLevel(String line, int cursor) {
        if (line.isBlank()) return true;
        return line.indexOf(' ') >= cursor || !line.contains(" ");
    }

    /**
     * 从已输入行文中提取当前路由上下文的完整路径
     */
    private String extractCurrentRoutePath(String fullLine) {
        String[] tokens = fullLine.split("\\s+");
        if (tokens.length == 0) return "";

        String firstToken = tokens[0];
        if (firstToken.contains(":")) {
            // 已有子路由：返回 command:sub 形式
            return firstToken.toLowerCase();
        } else if (tokens.length > 1 && tokens[1].contains(":")) {
            // 第二个 token 是子路由（如 "class info" 空格分隔）
            return firstToken.toLowerCase() + ":" + tokens[1].toLowerCase();
        }
        return firstToken.toLowerCase(); // 只有顶层命令
    }

    private Set<String> getParamsForContext(String fullLine) {
        String routePath = extractCurrentRoutePath(fullLine);

        // 优先查精确路由匹配
        Set<String> params = routeParams.get(routePath);
        if (params != null) return params;

        // 模糊匹配：只取顶层命令
        String topCmd = routePath.contains(":")
                ? routePath.substring(0, routePath.indexOf(':'))
                : routePath;
        params = customParams.get(topCmd);
        if (params != null) return params;

        // 合并该顶层命令下所有路由的参数
        Set<String> merged = new LinkedHashSet<>();
        for (String key : routeParams.keySet()) {
            if (key.startsWith(topCmd + ":")) {
                merged.addAll(routeParams.get(key));
            }
        }
        return merged.isEmpty() ? params : merged;
    }

    // ==================== 数据查询 ====================

    private Set<String> getSubCommands(String command) {
        Set<String> subs = subCommands.get(command);
        if (subs != null) return subs;
        return customSubCommands.get(command);
    }

    // ==================== 扫描 & 数据加载 ====================

    /**
     * 确保已完成元数据扫描（懒初始化）
     */
    private synchronized void ensureScanned() {
        if (scanned) return;
        doLoadFromScanner();
        scanned = true;
    }

    private void doLoadFromScanner() {
        try {
            List<CommandMetadataScanner.CommandMeta> commands =
                    CommandMetadataScanner.scanAllCommands();

            for (CommandMetadataScanner.CommandMeta cmd : commands) {
                topLevelCommands.add(cmd.name());

                for (CommandMetadataScanner.RouteMeta route : cmd.routes()) {
                // 注册子路由（只取最后一个路径段，如 "class:info" → "info"）
                String routePath = route.path();
                String subName = routePath.contains(":")
                        ? routePath.substring(routePath.lastIndexOf(':') + 1)
                        : routePath;
                subCommands.computeIfAbsent(cmd.name(), k -> new LinkedHashSet<>())
                        .add(subName);

                // route.path() 已经是完整路径（如 "class:info"），直接使用
                String fullPath = routePath;

                    // 注册参数
                    Set<String> paramSet = new LinkedHashSet<>();
                    for (CommandMetadataScanner.ParamMeta param : route.params()) {
                        paramSet.add(param.name());
                        paramSet.addAll(param.aliases());

                        // 注册枚举值
                        if (!param.allowedValues().isEmpty()) {
                            paramValueEnums.put(param.name(), param.allowedValues());
                            for (String alias : param.aliases()) {
                                paramValueEnums.put(alias, param.allowedValues());
                            }
                        }
                    }
                    routeParams.put(fullPath, paramSet);

                    // 注册描述
                    if (!route.description().isEmpty()) {
                        routeDescriptions.put(fullPath, route.description());
                    }
                }
            }
        } catch (Exception e) {
            // 扫描失败时降级为空（不影响使用）
            Logger logger =
                    Logger.getLoggerForName("CmdCompleter");
            logger.warn("命令元数据扫描失败，补全将使用空数据集: " + e.getMessage());
        }
    }

    // ==================== 运行时扩展 API ====================

    /** 注册新命令（运行时动态添加） */
    public void addCommand(String name) {
        customCommands.add(name.toLowerCase());
    }

    /** 注册命令及其子路由 */
    public void addCommand(String name, String[] subCommands) {
        customCommands.add(name.toLowerCase());
        this.customSubCommands.put(name.toLowerCase(), new HashSet<>(Set.of(subCommands)));
    }

    /** 注册命令的参数列表 */
    public void addParams(String command, String[] params) {
        Set<String> paramSet = new HashSet<>();
        for (String p : params) {
            if (!p.startsWith("-")) p = "-" + p;
            paramSet.add(p.toLowerCase());
        }
        customParams.put(command.toLowerCase(), paramSet);
    }

    /** 手动添加参数值枚举 */
    public void addParamValues(String paramName, String[] values) {
        customValueEnums.put(paramName.toLowerCase(), Arrays.asList(values));
    }

    /** 强制重新扫描（用于热更新场景） */
    public void rescan() {
        topLevelCommands.clear();
        subCommands.clear();
        routeParams.clear();
        paramValueEnums.clear();
        routeDescriptions.clear();
        scanned = false;
    }

    // ==================== 内部工具方法 ====================

    private void addCandidates(List<Candidate> candidates, Iterable<String> items,
                               String prefix, String group) {
        if (items == null) return;
        for (String item : items) {
            candidates.add(new Candidate(item, item, group, null, null, null, false));
        }
    }

    private Iterable<String> filterPrefix(Iterable<String> source, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase().startsWith(prefix)) {
                result.add(s);
            }
        }
        return result;
    }

    // ==================== 供 TailTipManager 使用的访问接口 ====================

    /** 获取路由描述映射（TailTip 用） */
    public Map<String, String> getRouteDescriptions() {
        ensureScanned();
        return Collections.unmodifiableMap(routeDescriptions);
    }

    /** 获取所有已知的顶层命令名 */
    public Set<String> getTopLevelCommands() {
        ensureScanned();
        return Collections.unmodifiableSet(topLevelCommands);
    }
}
