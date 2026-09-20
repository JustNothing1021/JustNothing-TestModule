package com.justnothing.testmodule.command.framework.model;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.command.framework.utils.CmdParamProcessor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

public class CommandRouter {

    private static final Logger logger = Logger.getLoggerForName("CommandRouter");

    private static final CommandRouter INSTANCE = new CommandRouter();

    private final Map<String, RouteNode> routeTree = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends MainCommand<?>>> commandRegistry = new ConcurrentHashMap<>();
    private final Map<Class<? extends CommandRequest<?>>, RouteConfig> requestRegistry = new ConcurrentHashMap<>();
    private final Map<String, RouteConfig> pathRegistry = new ConcurrentHashMap<>(); // 反向索引
    private final Set<Class<? extends CommandRequest<?>>> duplicateRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> duplicatePaths = ConcurrentHashMap.newKeySet();

    public static CommandRouter getInstance() {
        return INSTANCE;
    }

    private static Method executeMethod;

    static {
        try {
            executeMethod = Command.class.getDeclaredMethod("execute", CommandExecutor.CmdExecContext.class);
        } catch (NoSuchMethodException ignored) { }
    }

    public void registerCommand(Class<? extends MainCommand<?>> cmdClass) {
        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation == null) {
            logger.warn(cmdClass.getSimpleName() + " 缺少 @Cmd 注解，跳过注册");
            return;
        }

        String commandName = cmdAnnotation.name();

        // 注册必须幂等：同一个进程里存在两条注册路径，作用域还不一样 ——
        // 服务端 CommandExecutor 注册 CommandCatalog.ALL（含仅本地调试的 demo 命令），
        // 客户端 CommandMetadataScanner 只注册对用户可见的那部分。谁先跑取决于加载顺序
        // （单元测试里尤其如此），二次注册会把每条路由都塞进 duplicateRequests，
        // 让"路由冲突"的检测满屏假阳性。同一个类注册第二次本来就是同一件事，直接跳过。
        Class<? extends MainCommand<?>> previous = commandRegistry.putIfAbsent(commandName, cmdClass);
        if (previous != null) {
            if (previous != cmdClass) {
                // 这才是真冲突：两个不同的类声明了同一个命令名，后到的被忽略。
                logger.warn("命令名重复: " + commandName + " 已由 " + previous.getSimpleName()
                        + " 声明, 忽略 " + cmdClass.getSimpleName());
            }
            return;
        }

        CmdRoutes routesAnnotation = cmdClass.getAnnotation(CmdRoutes.class);
        if (routesAnnotation != null) {
            for (CmdRoutes.Route route : routesAnnotation.value()) {
                registerRoute(commandName, route);
            }
        }

        logger.info("注册命令: " + commandName + " (" + cmdClass.getSimpleName() + ")");
    }

    private void registerRoute(String parentPath, CmdRoutes.Route route) {
        // 路由 key 统一用 "/" 分层：class/info、threads/profile/start；
        // path 为空的路由就用父路径本身（export-context），不再产生 "xxx:" 这种带尾冒号的怪 key。
        String fullPath = route.path().isEmpty() ? parentPath : parentPath + "/" + route.path();
        String[] segments = route.path().split("/");

        RouteNode currentNode = routeTree.computeIfAbsent(parentPath, k -> new RouteNode(parentPath));

        // 空路径路由（path=""）：直接挂载到根节点，不创建子节点
        // 这样匹配时不会消费任何参数，所有参数都作为 remainingArgs 传给 CmdParamProcessor
        if (segments.length == 1 && segments[0].isEmpty()) {
            mountRoute(fullPath, route, currentNode);
            return;
        }

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            RouteNode existingChild = currentNode.getChild(segment);
            if (existingChild != null) {
                currentNode = existingChild;
            } else {
                RouteNode childNode = new RouteNode(segment);
                currentNode.addChild(segment, childNode);
                currentNode = childNode;
            }

            if (isLast) {
                mountRoute(fullPath, route, currentNode);
            }
        }
    }

    /**
     * 挂载一条路由：创建 {@link RouteConfig} 并写入三张表，同时记录重复注册。
     *
     * <p>结果类型不手写，而是从 handler 的泛型签名推导（见 {@link #resolveResultType}），
     * 因此它与 handler 永远一致；重复的 Request / path 会被记录，由构建期测试兜住。</p>
     */
    private void mountRoute(String fullPath, CmdRoutes.Route route, RouteNode node) {
        Class<? extends CommandResult> resultType = resolveResultType(route.handler());
        // 无参路由用具体的占位 Request（NoArgRequest），因此注解元素本身已是完整类型，无需强转。
        Class<? extends CommandRequest<?>> requestType = route.request();
        RouteConfig config = new RouteConfig(fullPath, requestType, resultType,
                route.handler(), route.description());
        node.addConfig(config);

        RouteConfig previousForRequest = requestRegistry.put(requestType, config);
        if (previousForRequest != null) {
            duplicateRequests.add(requestType);
            logger.warn("Request 被多条路由复用（key 会被覆盖）: " + requestType.getSimpleName()
                    + " — " + previousForRequest.path() + " 与 " + fullPath);
        }
        RouteConfig previousForPath = pathRegistry.put(fullPath, config);
        if (previousForPath != null) {
            duplicatePaths.add(fullPath);
            logger.warn("路由 key 重复（会被覆盖）: " + fullPath);
        }

        logger.debug("  └─ 注册路由: " + fullPath
                + " → " + route.request().getSimpleName()
                + " [" + route.handler().getSimpleName() + "]"
                + " → " + (resultType != null ? resultType.getSimpleName() : "?"));
    }

    /**
     * 从 handler 的泛型签名推导它的结果类型，纯反射、不实例化 handler。
     *
     * <p>所有 handler 的根都收敛到 {@code AbstractCommand<Req, Res>}：命令根
     * {@code MainCommand<Res>} 现在是它的薄子类，因此只需沿继承链把类型实参逐层代换，
     * 最终取出第 2 个类型实参 {@code Res}。</p>
     *
     * @return 结果类型；推导不出来时返回 null（会被构建期测试判为失败）
     */
    static Class<? extends CommandResult> resolveResultType(Class<?> handlerClass) {
        Class<?> current = handlerClass;
        Map<TypeVariable<?>, Type> bindings = new HashMap<>();
        while (current != null && current != Object.class) {
            Type superType = current.getGenericSuperclass();
            if (!(superType instanceof ParameterizedType parameterized)) {
                current = current.getSuperclass();
                continue;
            }
            Class<?> raw = (Class<?>) parameterized.getRawType();
            TypeVariable<?>[] vars = raw.getTypeParameters();
            Type[] args = parameterized.getActualTypeArguments();
            Map<TypeVariable<?>, Type> local = new HashMap<>();
            for (int i = 0; i < vars.length && i < args.length; i++) {
                local.put(vars[i], substitute(args[i], bindings));
            }
            if (raw == AbstractCommand.class) {
                Type resolved = local.get(vars[1]);
                return resolved instanceof Class<?> c && CommandResult.class.isAssignableFrom(c)
                        ? c.asSubclass(CommandResult.class) : null;
            }
            bindings = local;
            current = raw;
        }
        return null;
    }

    private static Type substitute(Type type, Map<TypeVariable<?>, Type> bindings) {
        if (type instanceof TypeVariable<?> variable) {
            Type bound = bindings.get(variable);
            if (bound != null) return bound;
        }
        return type;
    }
    public RouteMatch matchRoute(String commandName, String[] args) {
        logger.debug("[matchRoute] 开始匹配: command=" + commandName +
                    ", args=" + Arrays.toString(args));
        
        RouteNode rootNode = routeTree.get(commandName);
        if (rootNode == null) {
            logger.warn("[matchRoute] 未找到根节点: " + commandName);
            return null;
        }

        logger.debug("[matchRoute] 找到根节点: " + rootNode.name +
                   ", 子节点数: " + rootNode.children.size() +
                   ", 自身configs: " + rootNode.routeConfigs.size());

        if (args.length == 0) {
            // 空参数：检查根节点自身是否有配置（空路径路由，如 help 的 path=""）
            if (rootNode.hasConfig()) {
                logger.debug("[matchRoute] 无参数，匹配到根节点自身的空路径路由");
                return new RouteMatch(rootNode.getFirstRouteConfig(), new String[0]);
            }
            // 检查是否有空字符串 key 的子节点（path="" 注册为子节点的情况）
            RouteNode emptyChild = rootNode.getChild("");
            if (emptyChild != null && emptyChild.hasConfig()) {
                logger.debug("[matchRoute] 无参数，匹配到空路径子节点");
                return new RouteMatch(emptyChild.getFirstRouteConfig(), new String[0]);
            }
            logger.debug("[matchRoute] 无参数且无空路径路由，返回null以显示帮助");
            return null;
        }

        RouteNode current = rootNode;
        int consumedArgs = 0;
        List<String> matchedSegments = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            logger.debug("   [matchRoute] 处理参数[" + i + "] = '" + arg + "'");
            
            if (!current.hasChild(arg)) {
                logger.debug("   [matchRoute] 未找到子节点: '" + arg + "'" +
                           ", 可用子节点: " + current.children.keySet());
                
                RouteNode fuzzyMatch = tryFuzzyMatch(current, arg);
                if (fuzzyMatch == null) {
                    break;
                }
                
                current = fuzzyMatch;
                logger.debug("   [matchRoute] 模糊匹配到中间节点!");
                matchedSegments.add(arg);
                consumedArgs++;
                continue;
            }
            
            RouteNode child = current.getChild(arg);
            
            if (child.hasConfig() && child.isLeaf()) {
                String[] remainingArgs = Arrays.copyOfRange(args, i + 1, args.length);
                logger.debug("   [matchRoute] 匹配到叶子节点! path=" +
                           child.getFirstRouteConfig().path + 
                           ", remainingArgs=" + java.util.Arrays.toString(remainingArgs));
                return new RouteMatch(child.getFirstRouteConfig(), remainingArgs);
            }
            
            current = child;
            matchedSegments.add(arg);
            consumedArgs++;
        }

        if (current.hasConfig()) {
            RouteConfig config = current.getFirstRouteConfig();
            String[] remainingArgs = Arrays.copyOfRange(args, consumedArgs, args.length);
            logger.debug("[matchRoute] 最终匹配! path=" + config.path);
            return new RouteMatch(config, remainingArgs);
        }

        if (!current.isLeaf()) {
            logger.warn("[matchRoute] 停在中间节点 '" + current.name +
                       "', 需要更多参数。可用子节点: " + current.children.keySet());
            return null;
        }

        logger.warn("[matchRoute] 完全未匹配! 已匹配段: " + matchedSegments);
        return null;
    }
    
    /**
     * 精确匹配失败后的模糊匹配。
     */
    private RouteNode tryFuzzyMatch(RouteNode parent, String arg) {
        String lowerArg = arg.toLowerCase(Locale.ROOT);

        RouteNode match = uniqueCandidate(parent, lowerArg, true);
        if (match != null) {
            logger.debug("      [模糊匹配] 大小写不敏感全等命中: '" + arg + "'");
            return match;
        }

        if (!lowerArg.isEmpty()) {
            match = uniqueCandidate(parent, lowerArg, false);
            if (match != null) {
                logger.debug("      [模糊匹配] 唯一前缀命中: '" + arg + "'");
                return match;
            }
        }

        logger.debug("      [模糊匹配] 无候选或候选不唯一（按未匹配处理）: '" + arg + "'");
        return null;
    }

    /**
     * 在子节点里找出唯一命中的那个。
     *
     * @param exact true 表示全等匹配，false 表示「子命令名以输入为前缀」
     * @return 唯一命中的子节点；没有命中或有多个命中（歧义）都返回 null
     */
    private RouteNode uniqueCandidate(RouteNode parent, String lowerArg, boolean exact) {
        RouteNode found = null;
        // 排序遍历：候选是否唯一与遍历顺序无关，但确定的顺序让日志可复现
        for (String key : new TreeSet<>(parent.children.keySet())) {
            String lowerKey = key.toLowerCase(Locale.ROOT);
            boolean hit = exact ? lowerKey.equals(lowerArg) : lowerKey.startsWith(lowerArg);
            if (!hit) {
                continue;
            }
            if (found != null) {
                logger.debug("      [模糊匹配] 候选不唯一: '" + lowerArg + "' 同时命中多个子命令");
                return null;
            }
            found = parent.children.get(key);
        }
        return found;
    }

    /**
     * 取某个 Request 类型对应的路由 key（即 JSON 里的 {@code commandType}）。
     *
     * <p>key 由路由注册时按 "父路径/子路径" 自动生成，因此它与路由永远一致——
     * Request 侧不需要（也没有）任何手写 key 的注解。</p>
     *
     * @return 路由 key；该 Request 没有对应路由时返回 null
     */
    public String getCommandTypeFor(Class<? extends CommandRequest<?>> requestType) {
        RouteConfig config = requestRegistry.get(requestType);
        return config != null ? config.path() : null;
    }

    /**
     * 取某个 Request 类型对应的结果类型（由 handler 泛型签名推导）。
     */
    public Class<? extends CommandResult> getResultTypeFor(Class<? extends CommandRequest<?>> requestType) {
        RouteConfig config = requestRegistry.get(requestType);
        return config != null ? config.resultType() : null;
    }

    /** 注册期间发现的"被多条路由复用的 Request"（key 会被覆盖）。 */
    public Set<Class<? extends CommandRequest<?>>> getDuplicateRequests() {
        return duplicateRequests;
    }

    /** 注册期间发现的"重复路由 key"。 */
    public Set<String> getDuplicatePaths() {
        return duplicatePaths;
    }

    /**
     * 通过 commandType 字符串（如 "class/info"）解析 JSON 请求。
     * 用于 UI/Socket 客户端发送的 JSON 命令请求
     *
     * @param json 包含 commandType 字段的 JSON 字符串
     * @return 解析后的 CommandRequest 实例，如果 commandType 未注册则返回 null
     */
    public CommandRequest<?> resolveRequestFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String commandType = obj.optString("commandType");
            if (commandType.isEmpty()) {
                logger.warn("[resolveRequest] JSON 中缺少 commandType 字段");
                return null;
            }

            RouteConfig config = pathRegistry.get(commandType);
            if (config == null) {
                logger.warn("[resolveRequest] 未注册的命令类型: " + commandType +
                           ", 已注册类型: " + pathRegistry.keySet());
                return null;
            }

            Class<? extends CommandRequest<?>> requestClass = config.requestType;
            CommandRequest<?> request = GsonFactory
                    .getInstance().fromJson(json, requestClass);
            logger.info("[resolveRequest] 成功解析: " + commandType + " → " + requestClass.getSimpleName());
            return request;

        } catch (Exception e) {
            logger.error("[resolveRequest] 解析失败", e);
            return null;
        }
    }

    /**
     * 命令执行的唯一入口。
     *
     * <p>统一收敛原 CommandExecutor 的两条路径：</p>
     * <ol>
     *   <li>命中路由 → 交给路由处理器（统一调用 {@link Command#execute}；
     *       命令根与子命令都实现该接口）</li>
     *   <li>无路由定义的命令 → 回退到命令注册表的 {@link MainCommand}，同样走 {@code execute()}</li>
     * </ol>
     *
     * <p>命令已注册路由但参数不匹配（如子命令拼写错误）时抛 {@link IllegalArgumentException}，
     * 由上层决定是展示帮助（CLI）还是返回错误结果（JSON/UI）。</p>
     */
    public CommandResult dispatch(CommandExecutor.CmdExecContext<?> context) throws Throwable {
        String cmdName = context.cmdName();
        String[] args = context.args();

        RouteMatch match = matchRoute(cmdName, args);
        if (match == null) {
            if (!getRoutesForCommand(cmdName).isEmpty()) {
                throw new IllegalArgumentException("未找到匹配的路由: " + cmdName + " " + String.join(" ", args));
            }
            return dispatchToRegisteredCommand(cmdName, context);
        }

        RouteConfig config = match.routeConfig;
        Class<? extends CommandRequest<?>> requestType = config.requestType;

        // 抽象类（如 CommandRequest 本身）无法实例化，直接用 null
        int requestModifiers = requestType.getModifiers();
        CommandRequest<?> request = (requestModifiers & java.lang.reflect.Modifier.ABSTRACT) != 0
                ? null
                : requestType.getDeclaredConstructor().newInstance();

        @SuppressWarnings("unchecked")
        CommandExecutor.CmdExecContext<CommandRequest<?>> typedContext = (CommandExecutor.CmdExecContext<CommandRequest<?>>) context;

        // 上下文已有正确类型的请求（来自 JSON/UI 解析或 CLI 预解析）→ 直接使用，不再二次解析。
        // 否则会用一个空的命令行参数去解析，误触发必填校验、误伤 JSON 请求。
        CommandRequest<?> existingRequest = typedContext.getRequest();
        if (existingRequest != null && requestType.isAssignableFrom(existingRequest.getClass())) {
            logger.info("dispatch: 使用已有请求: %s", existingRequest.getClass().getSimpleName());
            request = existingRequest;
        } else {
            // 命令行路径：即使 remainingArgs 为空也要解析，否则 required 参数会被静默放行
            if (request != null) {
                request = CmdParamProcessor.parseRequest(request, match.remainingArgs);
            }
            typedContext.setRequest(request);
        }

        Object handlerInstance = config.handlerType.getDeclaredConstructor().newInstance();
        logger.info("handler实例已创建: %s", config.handlerType.getSimpleName());

        // 命令根与子命令都实现 Command（命令根继承 AbstractCommand），统一走 execute()。
        Method method = findExecuteMethod(config.handlerType);
        if (method == null) {
            throw new UnsupportedOperationException(
                "Handler " + config.handlerType.getSimpleName() + " 不支持执行");
        }
        logger.info("调用 execute(): %s.%s()",
                config.handlerType.getSimpleName(), method.getName());
        Object result;
        try {
            result = method.invoke(handlerInstance, context);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause == null) cause = e;
            throw cause;
        }
        logger.info("execute() 返回: %s",
                result != null ? result.getClass().getSimpleName() : "null");
        return (CommandResult) result;
    }

    /**
     * 无路由命令的统一回退：从命令注册表取出命令根实例并调用统一的 {@code execute()}。
     *
     * <p>用于只标注 {@code @Cmd}（没有 {@code @CmdRoutes}）的命令，使它们与路由命令
     * 共用 {@link #dispatch} 这一个执行入口，并共享 {@link AbstractCommand} 的集中异常兜底。</p>
     */
    private CommandResult dispatchToRegisteredCommand(String commandName,
                                                     CommandExecutor.CmdExecContext<?> context) throws Throwable {
        Class<? extends MainCommand<?>> cmdClass = commandRegistry.get(commandName);
        if (cmdClass == null) {
            throw new IllegalArgumentException("未知的命令: " + commandName);
        }

        MainCommand<?> handler = cmdClass.getDeclaredConstructor().newInstance();
        logger.info("命令无路由定义，回退 execute(): %s (%s)",
                commandName, cmdClass.getSimpleName());

        return handler.execute(context);
    }

    /**
     * 取命令的执行方法。执行契约只有 {@link Command#execute} 一个：命令根与子命令都（间接）
     * 继承 {@link AbstractCommand}，因此按接口判定类型，而不是看"直接实现的接口列表"，
     * 也不再按方法名反射（旧名 executeWithResult 已不存在，会让所有路由命令报"不支持执行"）。
     */
    private Method findExecuteMethod(Class<?> handlerClass) {
        if (executeMethod != null && Command.class.isAssignableFrom(handlerClass)) {
            return executeMethod;
        }
        try {
            return handlerClass.getMethod("execute", CommandExecutor.CmdExecContext.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public List<RouteConfig> getRoutesForCommand(String commandName) {
        List<RouteConfig> routes = new ArrayList<>();
        RouteNode node = routeTree.get(commandName);
        if (node != null) {
            collectRoutes(node, routes);
        }
        return routes;
    }

    /**
     * 获取完整路径注册表（反向索引）。
     * <p>
     * Key 为完整路径字符串（如 "class/info"），Value 为对应的路由配置。
     * 供 {@link com.justnothing.methodsclient.metadata.CommandMetadataScanner} 等外部组件使用。
     */
    public Map<String, RouteConfig> getPathRegistry() {
        return Collections.unmodifiableMap(pathRegistry);
    }

    private void collectRoutes(RouteNode node, List<RouteConfig> routes) {
        routes.addAll(node.getRouteConfigs());

        for (RouteNode child : node.getChildren()) {
            collectRoutes(child, routes);
        }
    }

    public String generateHelpForCommand(String commandName) {
        Class<? extends MainCommand<?>> cmdClass = commandRegistry.get(commandName);
        if (cmdClass == null) {
            return "未知命令: " + commandName;
        }

        return CmdParamProcessor.generateHelpText(cmdClass);
    }

    /**
     * 生成指定子命令的帮助文档
     * @param commandName 命令名（如 "class"）
     * @param args 参数数组（如 ["info"]），用于匹配子命令
     * @return 如果匹配到子命令则返回该子命令的帮助，否则返回完整帮助
     */
    public String generateHelpForRoute(String commandName, String[] args) {
        // 尝试匹配路由以确定是哪个子命令
        RouteMatch match = matchRoute(commandName, args);

        if (match != null && match.routeConfig != null) {
            // 匹配到具体子命令，只显示该子命令的帮助
            Class<? extends MainCommand<?>> cmdClass = commandRegistry.get(commandName);
            if (cmdClass != null) {
                return CmdParamProcessor.generateHelpForRoute(cmdClass, match.routeConfig);
            }
        }

        // 未匹配到子命令或匹配失败，显示完整帮助
        return generateHelpForCommand(commandName);
    }

    public record RouteMatch(RouteConfig routeConfig, String[] remainingArgs) {
    }

    public record RouteConfig(String path, Class<? extends CommandRequest<?>> requestType,
                              Class<? extends CommandResult> resultType,
                              Class<?> handlerType,
                              String description) {

        @NonNull
        @Override
            public String toString() {
                return String.format("Route[%s → %s (%s)]", path, requestType.getSimpleName(), handlerType.getSimpleName());
            }
        }

    static class RouteNode {
        private final String name;
        private final Map<String, RouteNode> children = new HashMap<>();
        private final List<RouteConfig> routeConfigs = new ArrayList<>();

        public RouteNode(String name) {
            this.name = name;
        }

        public void addChild(String segment, RouteNode child) {
            children.put(segment.toLowerCase(), child);
        }

        public RouteNode getChild(String segment) {
            return children.get(segment.toLowerCase());
        }
        
        public boolean hasChild(String segment) {
            return children.containsKey(segment.toLowerCase());
        }

        public void addConfig(RouteConfig config) {
            routeConfigs.add(config);
        }

        public boolean hasConfig() {
            return !routeConfigs.isEmpty();
        }

        public List<RouteConfig> getRouteConfigs() {
            return routeConfigs;
        }

        public RouteConfig getFirstRouteConfig() {
            return routeConfigs.isEmpty() ? null : routeConfigs.get(0);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public List<RouteNode> getChildren() {
            return new ArrayList<>(children.values());
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node[").append(name);
            if (!routeConfigs.isEmpty()) {
                sb.append(" ").append(routeConfigs.size()).append("configs");
            }
            if (!children.isEmpty()) {
                sb.append(" {").append(children.keySet()).append("}");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
