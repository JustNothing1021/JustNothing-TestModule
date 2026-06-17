package com.justnothing.engine;

import com.justnothing.engine.api.DefaultOutputHandler;
import com.justnothing.engine.api.IClassFinder;
import com.justnothing.engine.api.IOutputHandler;
import com.justnothing.engine.ast.ASTNode;
import com.justnothing.engine.ast.nodes.ClassDeclarationNode;
import com.justnothing.engine.codegen.DynamicClassGenerator;
import com.justnothing.engine.builtins.BuiltinRegistry;
import com.justnothing.engine.builtins.Builtins;
import com.justnothing.engine.eval.CustomClassExecutor;
import com.justnothing.engine.parser.OperatorRegistry;
import com.justnothing.engine.eval.EvalContext;
import com.justnothing.engine.eval.EvalException;
import com.justnothing.engine.eval.Evaluator;
import com.justnothing.engine.eval.Value;
import com.justnothing.engine.lexer.Lexer;
import com.justnothing.engine.parser.CythavaParseException;
import com.justnothing.engine.parser.ParseContext;
import com.justnothing.engine.parser.Parser;
import com.justnothing.engine.preprocessor.Preprocessor;
import com.justnothing.engine.security.IPermissionChecker;
import com.justnothing.engine.security.SandboxConfig;
import com.justnothing.engine.security.SecurityGate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptRunner {

    private Preprocessor preprocessor;
    private final ParseContext parseContext;
    private final EvalContext evalContext;
    private final DynamicClassGenerator codegen;
    private final ClassLoader classLoader;
    private IOutputHandler outputHandler;
    private IOutputHandler errorHandler;
    private boolean enablePreprocessor = true;

    public ScriptRunner() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ScriptRunner(ClassLoader classLoader) {
        this(classLoader, null, null);
    }

    public ScriptRunner(IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this(Thread.currentThread().getContextClassLoader(), outputHandler, errorHandler);
    }

    public ScriptRunner(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.outputHandler = outputHandler != null ? outputHandler : new DefaultOutputHandler(System.out, System.in);
        this.errorHandler = errorHandler != null ? errorHandler : new DefaultOutputHandler(System.err, System.in);

        // ★ 创建共享的 BuiltinRegistry，解析器和运行时共用同一个实例
        BuiltinRegistry registry = new BuiltinRegistry();
        // ★ 创建共享的 OperatorRegistry，解析期注册 + 运行期查询共用
        OperatorRegistry operatorRegistry = new OperatorRegistry();
        operatorRegistry.registerAllBuiltins();  // 预注册所有内置运算符
        this.parseContext = new ParseContext(this.classLoader);
        this.parseContext.setBuiltinRegistry(registry);
        this.parseContext.setOperatorRegistry(operatorRegistry);
        this.evalContext = new EvalContext(registry, this.outputHandler);
        this.codegen = new DynamicClassGenerator(this.classLoader);
        this.codegen.setDelegateToExecutor(true);
        this.codegen.setClassDeclarations(parseContext.getClassDeclarations());
        this.preprocessor = new Preprocessor();
        this.parseContext.setClassLoader(codegen.getLoader());
        this.parseContext.setCodeGenerator(codegen);
    }

    // ==================== Builtin 管理 ====================

    /**
     * 动态添加 builtin 函数。同时注册到解析器符号表和运行时执行器。
     * <p>Hook 模块等外部代码应使用此方法，确保解析期不会报 "Cannot find symbol"。
     */
    public void addBuiltin(String name, Builtins.BuiltinFunction function) {
        Builtins builtins = evalContext.getBuiltins();
        if (builtins != null) {
            builtins.registerFunction(name, function);
        }
    }

    /** 获取共享的 BuiltinRegistry（供高级用法直接操作注册表）。 */
    public BuiltinRegistry getBuiltinRegistry() {
        Builtins builtins = evalContext.getBuiltins();
        return builtins != null ? builtins.getRegistry() : null;
    }

    /** 获取共享的 OperatorRegistry（供动态注册运算符重载）。 */
    public OperatorRegistry getOperatorRegistry() {
        return parseContext.getOperatorRegistry();
    }

    /** 设置严格模式（默认 true）。设为 false 时解析器对运算符类型不匹配等静默放行。 */
    public void setStrictMode(boolean strict) {
        parseContext.setStrictMode(strict);
    }

    /** 获取当前严格模式状态。 */
    public boolean isStrictMode() {
        return parseContext.isStrictMode();
    }

    // ==================== Execution ====================

    public Object executeWithResult(String code) {
        return executeWithResult(code, "<stdin>");
    }

    public Object executeWithResult(String code, String sourceFileName) {
        try {
            String processedCode = preprocess(code);
            Lexer lexer = new Lexer(processedCode, sourceFileName);
            Parser parser = new Parser(lexer.tokenize(), parseContext, sourceFileName);
            List<ASTNode> nodes = parser.parse();

            for (ASTNode node : nodes) {
                if (node instanceof ClassDeclarationNode classDecl) {
                    parseContext.declareClass(classDecl);
                    try {
                        codegen.generate(classDecl);
                    } catch (Exception e) {
                        // skip
                    }
                }
            }

            CustomClassExecutor.setContext(evalContext, parseContext);
            Evaluator evaluator = new Evaluator(evalContext, parseContext);
            List<Value> results = evaluator.evaluateAll(nodes);
            CustomClassExecutor.clearContext();

            if (results.isEmpty()) return null;
            Value last = results.get(results.size() - 1);
            return last instanceof Value.VoidValue ? null : last.asJavaObject();
        } catch (CythavaParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (EvalException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void execute(String code) {
        executeWithResult(code);
    }

    public void execute(String code, String sourceFileName) {
        executeWithResult(code, sourceFileName);
    }

    /**
     * 执行代码（兼容旧版 API，支持每次调用指定输出/错误处理器）。
     * <p>临时替换 outputHandler/errorHandler，执行后恢复。
     */
    public void execute(String code, IOutputHandler out, IOutputHandler err) {
        executeWithResult(code, out, err);
    }

    /**
     * 执行代码并返回结果（兼容旧版 API，支持每次调用指定输出/错误处理器）。
     * <p>临时替换 outputHandler/errorHandler，执行后恢复。
     */
    public Object executeWithResult(String code, IOutputHandler out, IOutputHandler err) {
        IOutputHandler oldOut = this.outputHandler;
        IOutputHandler oldErr = this.errorHandler;
        try {
            this.outputHandler = out != null ? out : oldOut;
            this.errorHandler = err != null ? err : oldErr;
            return executeWithResult(code);
        } finally {
            this.outputHandler = oldOut;
            this.errorHandler = oldErr;
        }
    }

    // ==================== Parsing ====================

    public List<ASTNode> tryParse(String code) {
        return tryParse(code, "<stdin>");
    }

    public List<ASTNode> tryParse(String code, String sourceFileName) {
        try {
            String processedCode = preprocess(code);
            Lexer lexer = new Lexer(processedCode, sourceFileName);
            Parser parser = new Parser(lexer.tokenize(), parseContext, sourceFileName);
            List<ASTNode> nodes = parser.parse();
            return nodes != null ? nodes : new ArrayList<>();
        } catch (CythavaParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        }
    }

    // ==================== Imports ====================

    public void addImport(String importStmt) {
        parseContext.addImport(importStmt);
    }

    public void clearImports() {
        parseContext.clearImports();
    }

    // ==================== Variables ====================

    public void setVariable(String name, Object value) {
        evalContext.setVariable(name, Value.of(value));
    }

    public Object getVariable(String name) {
        if (evalContext.hasVariable(name)) {
            Value v = evalContext.getVariable(name);
            return v != null ? v.asJavaObject() : null;
        }
        return null;
    }

    public boolean hasVariable(String name) {
        return evalContext.hasVariable(name);
    }

    public void deleteVariable(String name) {
        evalContext.getVariables().remove(name);
    }

    public void clearVariables() {
        evalContext.getVariables().clear();
    }

    public Map<String, Object> getAllVariablesAsObject() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Value> entry : evalContext.getVariables().entrySet()) {
            result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().asJavaObject() : null);
        }
        return result;
    }

    // ==================== Class finder ====================

    public void setClassFinder(IClassFinder classFinder) {
        parseContext.setClassFinder(classFinder);
    }

    public IClassFinder getClassFinder() {
        return parseContext.getClassFinder();
    }

    // ==================== Accessors ====================

    public ParseContext getParseContext() {
        return parseContext;
    }

    public EvalContext getEvalContext() {
        return evalContext;
    }

    public DynamicClassGenerator getCodeGenerator() {
        return codegen;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public IOutputHandler getOutputHandler() {
        return outputHandler;
    }

    public void setOutputHandler(IOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    public IOutputHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(IOutputHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    // ==================== Preprocessor ====================

    private String preprocess(String code) {
        if (!enablePreprocessor || preprocessor == null) {
            return code;
        }
        String result = preprocessor.process(code);
        // 仅当用户通过 #pragma typeCheck 显式修改过时才同步到 ParseContext
        // （避免用 Preprocessor 默认值 true 覆盖 setStrictMode(false) 的设置）
        if (preprocessor != null && preprocessor.wasTypeCheckExplicitlySet()) {
            parseContext.setStrictMode(preprocessor.isTypeCheckEnabled());
        }
        return result;
    }

    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    public void setPreprocessor(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public boolean isPreprocessorEnabled() {
        return enablePreprocessor;
    }

    public void setEnablePreprocessor(boolean enable) {
        this.enablePreprocessor = enable;
    }

    public void defineMacro(String name, String value) {
        if (preprocessor == null) {
            preprocessor = new Preprocessor();
        }
        preprocessor.define(name, value);
    }

    public void defineMacro(String name) {
        if (preprocessor == null) {
            preprocessor = new Preprocessor();
        }
        preprocessor.define(name);
    }

    public void undefineMacro(String name) {
        if (preprocessor != null) {
            preprocessor.undefine(name);
        }
    }

    public boolean isMacroDefined(String name) {
        return preprocessor != null && preprocessor.isDefined(name);
    }

    // ==================== Security / Sandbox 兼容层 ====================

    /**
     * 应用沙箱配置，设置解释器级权限检查器。
     *
     * <p>兼容旧版 API：旧版通过 ExecutionContext.setPermissionChecker() 设置，
     * 新版通过 EvalContext.setSecurityGate(SecurityGate) 实现。
     *
     * @param config 沙箱配置（null 表示清除安全限制）
     */
    public void applySandboxConfig(SandboxConfig config) {
        if (config == null) {
            evalContext.setSecurityGate(null);
            return;
        }
        IPermissionChecker checker = config.getPermissionChecker();
        if (checker != null) {
            evalContext.setSecurityGate(new SecurityGate(checker));
        } else {
            evalContext.setSecurityGate(null);
        }
    }

    /**
     * 直接设置权限检查器。
     * <p>兼容旧版 {@code ExecutionContext.setPermissionChecker(IPermissionChecker)}。
     */
    public void setPermissionChecker(IPermissionChecker checker) {
        if (checker != null) {
            evalContext.setSecurityGate(new SecurityGate(checker));
        } else {
            evalContext.setSecurityGate(null);
        }
    }

    /** 获取当前的权限检查器。 */
    public IPermissionChecker getPermissionChecker() {
        SecurityGate gate = evalContext.getSecurityGate();
        return gate != null ? gate.getChecker() : null;
    }

    /**
     * 获取执行上下文（兼容旧版 getExecutionContext）。
     * <p>注意：返回的是 EvalContext 而非旧的 ExecutionContext。
     */
    public Object getExecutionContext() {
        return evalContext;
    }

    /** 是否启用 AST 打印模式。 */
    private boolean printASTMode = false;

    public boolean isPrintAST() {
        return printASTMode;
    }

    public void setPrintAST(boolean printAST) {
        this.printASTMode = printASTMode;
    }
}
