package com.justnothing.javainterpreter.security;

import java.util.concurrent.Callable;

public final class SandboxGuard {

    private static final ThreadLocal<SandboxContext> currentContext = new ThreadLocal<>();

    private SandboxGuard() {}

    public static SandboxContext enterSandbox(IPermissionChecker checker) {
        SandboxContext ctx = new SandboxContext(checker);
        currentContext.set(ctx);
        return ctx;
    }

    public static void exitSandbox() {
        currentContext.remove();
    }

    public static boolean isInSandbox() {
        SandboxContext ctx = currentContext.get();
        return ctx != null && ctx.isActive();
    }

    public static IPermissionChecker getCurrentChecker() {
        SandboxContext ctx = currentContext.get();
        return ctx != null ? ctx.getChecker() : null;
    }

    public static void checkMethodCall(String owner, String name, String descriptor) {
        SandboxContext ctx = currentContext.get();
        if (ctx == null || !ctx.isActive()) return;
        String dotOwner = owner.replace('/', '.');
        ctx.getChecker().checkMethodAccess(dotOwner, name, descriptor);
    }

    public static void checkFieldAccess(String owner, String name, String descriptor) {
        SandboxContext ctx = currentContext.get();
        if (ctx == null || !ctx.isActive()) return;
        String dotOwner = owner.replace('/', '.');
        ctx.getChecker().checkFieldAccess(dotOwner, name);
    }

    public static void checkNewInstance(String owner) {
        SandboxContext ctx = currentContext.get();
        if (ctx == null || !ctx.isActive()) return;
        String dotOwner = owner.replace('/', '.');
        ctx.getChecker().checkNewInstance(dotOwner);
    }

    public static void checkClassAccess(String owner) {
        SandboxContext ctx = currentContext.get();
        if (ctx == null || !ctx.isActive()) return;
        String dotOwner = owner.replace('/', '.');
        ctx.getChecker().checkClassAccess(dotOwner);
    }

    public static <T> T executeWithPermission(Callable<T> action, IPermissionChecker checker) throws Exception {
        try (SandboxContext ignored = enterSandbox(checker)) {
            return action.call();
        }
    }

    public static void executeWithPermission(Runnable action, IPermissionChecker checker) {
        try (SandboxContext ignored = enterSandbox(checker)) {
            action.run();
        }
    }

    public static <T> T execute(Callable<T> action) throws Exception {
        return executeWithPermission(action, BasicPermissionChecker.permissive());
    }

    public static void execute(Runnable action) {
        executeWithPermission(action, BasicPermissionChecker.permissive());
    }

    public static <T> T executeRestricted(Callable<T> action) throws Exception {
        return executeWithPermission(action, BasicPermissionChecker.createSandbox());
    }

    public static void executeRestricted(Runnable action) {
        executeWithPermission(action, BasicPermissionChecker.createSandbox());
    }

    public static <T> T executeMinimal(Callable<T> action) throws Exception {
        return executeWithPermission(action, BasicPermissionChecker.createMinimal());
    }

    public static void executeMinimal(Runnable action) {
        executeWithPermission(action, BasicPermissionChecker.createMinimal());
    }

    public static <T> T executeExpressionOnly(Callable<T> action) throws Exception {
        return executeWithPermission(action, BasicPermissionChecker.createExpressionOnly());
    }

    public static void executeExpressionOnly(Runnable action) {
        executeWithPermission(action, BasicPermissionChecker.createExpressionOnly());
    }

    public static final class SandboxContext implements AutoCloseable {
        private final IPermissionChecker checker;
        private volatile boolean active = true;

        SandboxContext(IPermissionChecker checker) {
            this.checker = checker;
        }

        public IPermissionChecker getChecker() {
            return checker;
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            active = false;
        }

        @Override
        public void close() {
            exitSandbox();
        }
    }
}
