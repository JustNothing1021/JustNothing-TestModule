package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.methodsclient.metadata.CommandMetadataScanner;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.Command;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * 参数声明的"编译期"校验（以单元测试形式落地）。
 *
 * <p>Java 没有 C++ {@code static_assert} 那样的编译期断言，而 {@code @CmdParam} 涉及的约束
 * （类型与 min/max 的搭配、varArgs 与 position 的关系）无法用 Java 类型系统表达。
 * 这里扫描全部 {@link CommandRequest} 子类，把声明错误变成构建失败：</p>
 *
 * <ul>
 *   <li>具体 Request 类必须挂在一条路由上（commandType 由路由派生，没有路由就没有 key）；</li>
 *   <li>路由 handler 必须实现 {@link Command}（否则分派只能靠鸭子类型，也无法统一异常兜底）；</li>
 *   <li>Request → Result 必须是单射，且同一个 Request / key 不能被多条路由复用；</li>
 *   <li>min/max 只能声明在数值类型字段上（字符串无法比较大小）；</li>
 *   <li>每个类最多一个 varArgs 字段，且它必须是位置参数中 position 最大的那个；</li>
 *   <li>位置 varArgs 字段必须是 String（解析时会把剩余参数拼成一段文本）；</li>
 *   <li>同一类内 position 不能重复。</li>
 * </ul>
 */
public class CmdParamDeclarationTest {

    private static final Map<Class<?>, List<Class<?>>> scannedCache = new HashMap<>();
    private static final Map<Class<?>, List<String>> loadFailuresByType = new HashMap<>();
    private static final Map<Class<?>, String> diagnosticsByType = new HashMap<>();

    // ==================== 校验规则 ====================

    @Test
    public void scannerFindsRequestClasses() {
        // 兜底：若类路径扫描失效，下面所有校验都会"空跑通过"，必须显式失败暴露。
        List<Class<?>> classes = scanRequestClasses();
        assertTrue("类路径扫描未找到足够的 Request 类（" + classes.size() + " 个），扫描逻辑可能已失效；"
                        + "诊断: " + diagnosticsByType.get(CommandRequest.class),
                classes.size() > 50);
        List<String> failures = loadFailuresByType.getOrDefault(CommandRequest.class, List.of());
        assertTrue("以下 Request 类加载失败，其声明未被校验:\n" + String.join("\n", failures),
                failures.isEmpty());
    }

    /**
     * 校验每个 Request 的 commandType 都能解析到。
     *
     * <p>commandType 完全由路由派生（{@code CommandRouter} 注册路由时自动生成），
     * 所以该 Request 只需挂在某条路由上，key 便自动存在且与路由一致。
     *
     * <p>一个 Request 若没有路由，运行时构造出的 commandType 就是空的，服务端必然无法路由，
     * 因此这里必须失败。</p>
     */
    @Test
    public void everyRequestHasResolvableCommandType() {
        CommandMetadataScanner.ensureRegistered(); // 建立路由表
        Set<String> registered = CommandRouter.getInstance().getPathRegistry().keySet();

        List<String> offenders = new ArrayList<>();
        for (Class<?> c : scanRequestClasses()) {
            @SuppressWarnings("unchecked")
            Class<? extends CommandRequest<?>> requestType = (Class<? extends CommandRequest<?>>) c;

            String derived = CommandRouter.getInstance().getCommandTypeFor(requestType);
            if (derived == null) {
                offenders.add(c.getSimpleName() + " 没有注册路由，无法派生 commandType");
            } else if (!registered.contains(derived)) {
                offenders.add(c.getSimpleName() + " 派生的 key 不在路由表: " + derived);
            }
        }
        assertTrue("以下 Request 无法解析 commandType（运行时会发出空 commandType）:\n"
                + String.join("\n", offenders), offenders.isEmpty());
    }

    /**
     * 校验每个路由 handler 都实现了 {@link Command}。
     *
     * <p>handler 层的唯一标准是 AbstractCommand（它提供请求类型护栏、异常兜底与结果类型）。
     * 此前有 5 个中间基类（AbstractHookCommand / AbstractNativeCommand / AbstractNetworkCommand /
     * AbstractScriptCommand / AbstractTraceCommand）另起炉灶，既不实现 Command 也不带结果类型，
     * 导致 CommandRouter 只能靠鸭子类型找执行方法、异常兜底各自为政。
     * 这里把约定变成构建失败，防止再退化。</p>
     */
    @Test
    public void everyRouteHandlerImplementsCommand() {
        CommandMetadataScanner.ensureRegistered(); // 建立路由表

        List<String> offenders = new ArrayList<>();
        for (CommandRouter.RouteConfig config : CommandRouter.getInstance().getPathRegistry().values()) {
            Class<?> handler = config.handlerType();
            if (!Command.class.isAssignableFrom(handler)) {
                offenders.add(config.path() + " → " + handler.getSimpleName());
            }
        }
        assertTrue("以下路由的 handler 没有实现 Command（无法统一分派与异常兜底）:\n"
                + String.join("\n", offenders), offenders.isEmpty());
    }

    /**
     * 校验 Request → Result 是单射，且路由 key 不重复。
     *
     * <p>结果类型由 handler 的泛型签名推导（见 CommandRouter.resolveResultType），不手写、
     * 因此不会与 handler 漂移。但"单射"这个架构假设仍需要构建期断言来钉死：</p>
     * <ul>
     *   <li>每条路由的 handler 都必须能推导出结果类型；</li>
     *   <li>同一个 Request 类型不能映射到两个不同的 Result；</li>
     *   <li>同一个 Request / 同一个 key 不能被两条路由复用（否则 requestRegistry 静默覆盖，
     *       其中一条在客户端不可达）。</li>
     * </ul>
     */
    @Test
    public void requestToResultIsInjective() {
        CommandMetadataScanner.ensureRegistered(); // 建立路由表
        CommandRouter router = CommandRouter.getInstance();

        List<String> offenders = new ArrayList<>();
        Map<Class<? extends CommandRequest<?>>, Class<? extends CommandResult>> seen = new HashMap<>();
        for (CommandRouter.RouteConfig config : router.getPathRegistry().values()) {
            if (config.resultType() == null) {
                offenders.add(config.path() + " 的 handler " + config.handlerType().getSimpleName()
                        + " 推导不出结果类型");
                continue;
            }
            Class<? extends CommandResult> previous =
                    seen.putIfAbsent(config.requestType(), config.resultType());
            if (previous != null && previous != config.resultType()) {
                offenders.add(config.requestType().getSimpleName() + " 同时映射到 "
                        + previous.getSimpleName() + " 与 " + config.resultType().getSimpleName());
            }
        }
        for (Class<? extends CommandRequest<?>> reused : router.getDuplicateRequests()) {
            offenders.add(reused.getSimpleName() + " 被多条路由复用（key 会被覆盖）");
        }
        for (String dupPath : router.getDuplicatePaths()) {
            offenders.add("路由 key 重复: " + dupPath);
        }

        assertTrue("Request → Result 单射假设被破坏:\n" + String.join("\n", offenders),
                offenders.isEmpty());
    }

    @Test
    public void minMaxOnlyOnNumericFields() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> c : scanRequestClasses()) {
            for (Field field : c.getDeclaredFields()) {
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param == null) continue;

                boolean hasRange = param.min() > Double.NEGATIVE_INFINITY
                        || param.max() < Double.POSITIVE_INFINITY;
                if (hasRange && !isNumeric(field.getType())) {
                    offenders.add(c.getSimpleName() + "." + field.getName()
                            + " (" + field.getType().getSimpleName() + ") 声明了 min/max");
                }
            }
        }
        assertTrue("min/max 只能用于数值类型字段，以下声明无法生效:\n"
                + String.join("\n", offenders), offenders.isEmpty());
    }

    @Test
    public void varArgsAndPositionsAreValid() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> c : scanRequestClasses()) {
            List<Field> varArgsFields = new ArrayList<>();
            List<Field> positionalFields = new ArrayList<>();
            Map<Integer, String> positionOwners = new HashMap<>();

            for (Field field : c.getDeclaredFields()) {
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param == null) continue;

                if (param.varArgs()) {
                    varArgsFields.add(field);
                }
                if (param.position() > 0) {
                    positionalFields.add(field);
                    String previous = positionOwners.put(param.position(), field.getName());
                    if (previous != null) {
                        offenders.add(c.getSimpleName() + " 的 position=" + param.position()
                                + " 被 " + previous + " 与 " + field.getName() + " 重复使用");
                    }
                }
            }

            if (varArgsFields.size() > 1) {
                offenders.add(c.getSimpleName() + " 声明了多个 varArgs 字段: " + varArgsFields);
            }

            int maxPosition = positionalFields.stream()
                    .mapToInt(f -> f.getAnnotation(CmdParam.class).position())
                    .max().orElse(0);

            for (Field field : varArgsFields) {
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param.position() <= 0) continue; // 关键字形式的 varArgs（--name），不参与位置分配

                if (field.getType() != String.class) {
                    offenders.add(c.getSimpleName() + "." + field.getName()
                            + " 是位置 varArgs，类型必须是 String（当前 " + field.getType().getSimpleName() + "）");
                }
                if (param.position() != maxPosition) {
                    offenders.add(c.getSimpleName() + "." + field.getName()
                            + " 是位置 varArgs，position 必须是最大值 " + maxPosition
                            + "（当前 " + param.position() + "）");
                }
            }
        }
        assertTrue("varArgs / position 声明非法:\n" + String.join("\n", offenders), offenders.isEmpty());
    }

    /**
     * 校验每个被路由引用的 Request 都在类型签名里声明了它的结果类型。
     *
     * <p>{@code CommandRequest<Res>} 把 "Request → Result 单射" 从运行时约定提升成类型系统事实：
     * 调用处可以据此写出 {@code FooResult r = ...} 而无需显式传 Result 类。
     * 但类型实参由每个 Request 类自己声明，漏写只会退化成裸类型（javac 仅给警告），
     * 因此这里用构建期断言钉死：</p>
     * <ul>
     *   <li>Request 必须声明具体类型实参（裸 {@code extends CommandRequest} 视为失败）；</li>
     *   <li>声明的 Res 必须与路由推导出的结果类型一致。</li>
     * </ul>
     */
    @Test
    public void everyRouteRequestDeclaresMatchingResultType() {
        CommandMetadataScanner.ensureRegistered(); // 建立路由表

        List<String> offenders = new ArrayList<>();
        for (CommandRouter.RouteConfig config : CommandRouter.getInstance().getPathRegistry().values()) {
            Class<?> request = config.requestType();
            // 抽象 Request 不能作为路由请求：它无法实例化，运行时只会得到 null 请求。
            // 无参路由请改用具体的占位类型 NoArgRequest。
            if (Modifier.isAbstract(request.getModifiers())) {
                offenders.add(request.getSimpleName() + " 是抽象 Request，不能作为路由请求；"
                        + "无参路由请用 NoArgRequest 占位");
                continue;
            }
            Class<? extends CommandResult> expected = config.resultType();
            Class<?> declared = resolveRequestResultType(request);
            if (declared == null) {
                offenders.add(request.getSimpleName() + " 是裸 CommandRequest，应声明为 CommandRequest<"
                        + (expected != null ? expected.getSimpleName() : "?") + ">");
            } else if (declared != expected) {
                offenders.add(request.getSimpleName() + " 声明的是 " + declared.getSimpleName()
                        + "，但路由推导为 " + (expected != null ? expected.getSimpleName() : "null"));
            }
        }
        assertTrue("以下 Request 未在类型签名里声明正确的 Res:\n" + String.join("\n", offenders),
                offenders.isEmpty());
    }

    /** 沿泛型继承链解出 Request 声明的 {@code CommandRequest<Res>} 中的 Res（裸类型返回 null）。 */
    private static Class<?> resolveRequestResultType(Class<?> requestClass) {
        Map<TypeVariable<?>, Type> bindings = new HashMap<>();
        Class<?> current = requestClass;
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
            if (raw == CommandRequest.class) {
                Type resolved = local.get(vars[0]);
                return resolved instanceof Class<?> c ? c : null;
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

    // ==================== 类路径扫描 ====================

    private static List<Class<?>> scanRequestClasses() {
        return scanConcreteSubclasses(CommandRequest.class, "com.justnothing.testmodule.command");
    }

    /**
     * 扫描指定包（含子包）下的所有具体子类。
     *
     * <p>包名必须显式传入：基类自身往往不在"子类所在的包"里
     * （例如 CommandRequest 在 .framework.model，而所有 Request 散落在 .command.*）。</p>
     */
    private static List<Class<?>> scanConcreteSubclasses(Class<?> baseType, String packageName) {
        List<Class<?>> cached = scannedCache.get(baseType);
        if (cached != null) {
            return cached;
        }

        ClassLoader classLoader = CmdParamDeclarationTest.class.getClassLoader();
        Set<String> classNames = new LinkedHashSet<>();

        // 从 baseType 自身的 code source 出发：AGP 单元测试可能把主 classes 以目录或
        // jar 的形式放进 classpath（实测 getResources 对 jar 形态拿不到包目录），两种都要支持。
        String codeSource = "(未知)";
        try {
            URL location = baseType.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                throw new IllegalStateException("无法定位 " + baseType.getSimpleName() + " 的 code source");
            }
            codeSource = location.toString();
            Path path = Paths.get(location.toURI());
            if (Files.isDirectory(path)) {
                collectFromDirectory(path, packageName, classNames);
            } else {
                collectFromJar(path, packageName, classNames);
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫描 " + packageName + " 失败", e);
        }

        List<Class<?>> result = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> c = Class.forName(className, false, classLoader);
                if (c == baseType
                        || !baseType.isAssignableFrom(c)
                        || c.isInterface()
                        || Modifier.isAbstract(c.getModifiers())) {
                    continue;
                }
                result.add(c);
            } catch (Throwable t) {
                failures.add(className + " → " + t);
            }
        }

        diagnosticsByType.put(baseType, "codeSource=" + codeSource + ", candidates=" + classNames.size()
                + ", loaded=" + result.size() + ", failures=" + failures.size()
                + (failures.isEmpty() ? "" : ", firstFailure=" + failures.get(0)));
        loadFailuresByType.put(baseType, failures);
        scannedCache.put(baseType, result);
        return result;
    }

    private static void collectFromDirectory(Path classesRoot, String packageName, Set<String> out) throws IOException {
        Path base = classesRoot.resolve(packageName.replace('.', '/'));
        if (!Files.isDirectory(base)) return;
        try (Stream<Path> paths = Files.walk(base)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        String relative = base.relativize(p).toString().replace(File.separatorChar, '/');
                        if (relative.contains("$")) return; // 跳过内部类/匿名类
                        out.add(packageName + "." + toClassName(relative));
                    });
        }
    }

    private static void collectFromJar(Path jarPath, String packageName, Set<String> out) {
        String prefix = packageName.replace('.', '/') + "/";
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(prefix) || !name.endsWith(".class")) continue;
                if (name.contains("$")) continue; // 跳过内部类/匿名类
                out.add(toClassName(name));
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + jarPath + " 失败", e);
        }
    }

    /** 把 class 文件路径（如 {@code a/b/C.class}）转成类名（{@code a.b.C}），不含包前缀。 */
    private static String toClassName(String classFilePath) {
        String withoutSuffix = classFilePath.substring(0, classFilePath.length() - ".class".length());
        return withoutSuffix.replace('/', '.');
    }

    private static boolean isNumeric(Class<?> type) {
        Class<?> boxed = type;
        if (type == int.class) boxed = Integer.class;
        else if (type == long.class) boxed = Long.class;
        else if (type == double.class) boxed = Double.class;
        else if (type == float.class) boxed = Float.class;
        else if (type == short.class) boxed = Short.class;
        else if (type == byte.class) boxed = Byte.class;
        return Number.class.isAssignableFrom(boxed);
    }
}
