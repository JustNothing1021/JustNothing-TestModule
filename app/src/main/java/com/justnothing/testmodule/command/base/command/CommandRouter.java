package com.justnothing.testmodule.command.base.command;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRouter {

    private static final Logger logger = Logger.getLoggerForName("CommandRouter");

    private static final CommandRouter INSTANCE = new CommandRouter();

    private final Map<String, RouteNode> routeTree = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends MainCommand<?>>> commandRegistry = new ConcurrentHashMap<>();
    private final Map<Class<? extends CommandRequest>, RouteConfig> requestRegistry = new ConcurrentHashMap<>();

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
        commandRegistry.put(commandName, cmdClass);

        CmdRoutes routesAnnotation = cmdClass.getAnnotation(CmdRoutes.class);
        if (routesAnnotation != null) {
            for (CmdRoutes.Route route : routesAnnotation.value()) {
                registerRoute(commandName, route, cmdClass);
            }
        }

        logger.info("注册命令: " + commandName + " (" + cmdClass.getSimpleName() + ")");
    }

    private void registerRoute(String parentPath, CmdRoutes.Route route, Class<? extends MainCommand<?>> cmdClass) {
        String fullPath = parentPath + ":" + route.path();
        String[] segments = route.path().split("/");

        RouteNode currentNode = routeTree.computeIfAbsent(parentPath, k -> new RouteNode(parentPath, cmdClass));

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            RouteNode existingChild = currentNode.getChild(segment);
            if (existingChild != null) {
                currentNode = existingChild;
            } else {
                RouteNode childNode = new RouteNode(segment, cmdClass);
                currentNode.addChild(segment, childNode);
                currentNode = childNode;
            }

            if (isLast) {
                RouteConfig config = new RouteConfig(
                    fullPath,
                    route.request(),
                    route.result(),
                    route.handler(),
                    route.description()
                );

                currentNode.addConfig(config);
                requestRegistry.put(route.request(), config);

                logger.debug("  └─ 注册路由: " + fullPath +
                           " → " + route.request().getSimpleName() +
                           " [" + route.handler().getSimpleName() + "]");
            }
        }
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
    
    private RouteNode tryFuzzyMatch(RouteNode parent, String arg) {
        for (Map.Entry<String, RouteNode> entry : parent.children.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(arg)) {
                return entry.getValue();
            }
        }
        
        for (Map.Entry<String, RouteNode> entry : parent.children.entrySet()) {
            if (entry.getKey().startsWith(arg.toLowerCase()) || 
                arg.toLowerCase().startsWith(entry.getKey())) {
                logger.debug("      [模糊匹配] 部分匹配: '" + arg + "' -> '" + entry.getKey() + "'");
                return entry.getValue();
            }
        }
        
        return null;
    }

    public RouteMatch matchRouteByRequest(Class<? extends CommandRequest> requestType) {
        RouteConfig config = requestRegistry.get(requestType);
        if (config != null) {
            return new RouteMatch(config, new String[0]);
        }
        
        for (Map.Entry<String, RouteNode> entry : routeTree.entrySet()) {
            RouteMatch match = findRouteByRequestInNode(entry.getValue(), requestType);
            if (match != null) return match;
        }
        
        return null;
    }
    
    private RouteMatch findRouteByRequestInNode(RouteNode node, Class<? extends CommandRequest> requestType) {
        for (RouteConfig config : node.getRouteConfigs()) {
            if (config.requestType == requestType) {
                return new RouteMatch(config, new String[0]);
            }
        }
        
        for (RouteNode child : node.getChildren()) {
            RouteMatch match = findRouteByRequestInNode(child, requestType);
            if (match != null) return match;
        }
        
        return null;
    }

    public CommandResult dispatch(CommandExecutor.CmdExecContext<?> context) throws Throwable {
        String cmdName = context.cmdName();
        String[] args = context.args();

        RouteMatch match = matchRoute(cmdName, args);
        if (match == null) {
            throw new IllegalArgumentException("未找到匹配的路由: " + cmdName + " " + String.join(" ", args));
        }

        RouteConfig config = match.routeConfig;
        Class<? extends CommandRequest> requestType = config.requestType;

        // 抽象类（如 CommandRequest 本身）无法实例化，直接用 null
        int requestModifiers = requestType.getModifiers();
        CommandRequest request = (requestModifiers & java.lang.reflect.Modifier.ABSTRACT) != 0
                ? null
                : requestType.getDeclaredConstructor().newInstance();

        // 使用统一的智能解析入口（request 为 null 时跳过，对应无参命令如 help）
        if (request != null && (match.remainingArgs.length > 0 || hasRequiredParams(requestType))) {
            request = CmdParamProcessor.parseRequest(request, match.remainingArgs);
        }

        @SuppressWarnings("unchecked")
        CommandExecutor.CmdExecContext<CommandRequest> typedContext = (CommandExecutor.CmdExecContext<CommandRequest>) context;
        typedContext.setRequest(request);

        Object handlerInstance = config.handlerType.getDeclaredConstructor().newInstance();
        logger.info("handler实例已创建: %s", config.handlerType.getSimpleName());

        if (handlerInstance instanceof MainCommand) {
            @SuppressWarnings("unchecked")
            MainCommand<CommandResult> mainCommand = (MainCommand<CommandResult>) handlerInstance;
            return mainCommand.runMain(typedContext);
        } else {
            Method executeMethod = findExecuteMethod(config.handlerType);
            if (executeMethod != null) {
                logger.info("反射调用 execute(): %s.%s()",
                        config.handlerType.getSimpleName(), executeMethod.getName());
                Object result;
                try {
                    result = executeMethod.invoke(handlerInstance, context);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause == null) cause = e;
                    throw cause;
                }
                logger.info("execute() 返回: %s",
                        result != null ? result.getClass().getSimpleName() : "null");
                return (CommandResult) result;
            } else {
                throw new UnsupportedOperationException(
                    "Handler " + config.handlerType.getSimpleName() + " 不支持执行");
            }
        }
    }

    private boolean hasRequiredParams(Class<? extends CommandRequest> requestType) {
        List<CmdParamProcessor.FieldInfo> fields = CmdParamProcessor.getCmdParamFields(requestType);
        return fields.stream().anyMatch(fi -> fi.param().required());
    }

    private Method findExecuteMethod(Class<?> handlerClass) {
        if (Arrays.asList(handlerClass.getInterfaces()).contains(Command.class) && executeMethod != null)
            return executeMethod;
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

    public record RouteConfig(String path, Class<? extends CommandRequest> requestType,
                              Class<? extends CommandResult> resultType, Class<?> handlerType,
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

        public RouteNode(String name, Class<? extends MainCommand<?>> ownerClass) {
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

        public RouteConfig getDefaultRoute() {
            if (!routeConfigs.isEmpty()) {
                return routeConfigs.get(0);
            }
            for (RouteNode child : children.values()) {
                RouteConfig nested = child.getDefaultRoute();
                if (nested != null) return nested;
            }
            return null;
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
