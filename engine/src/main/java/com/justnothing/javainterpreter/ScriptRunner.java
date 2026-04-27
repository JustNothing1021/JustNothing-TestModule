package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.IClassFinder;
import com.justnothing.javainterpreter.api.IOutputHandler;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.evaluator.ASTEvaluator;
import com.justnothing.javainterpreter.evaluator.ExecutionContext;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.javainterpreter.lexer.Lexer;
import com.justnothing.javainterpreter.parser.ParseContext;
import com.justnothing.javainterpreter.parser.Parser;
import com.justnothing.javainterpreter.preprocessor.Preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptRunner {
    
    private ExecutionContext context;
    private ParseContext parseContext;
    private Preprocessor preprocessor;
    private final ClassLoader classLoader;
    private boolean enablePreprocessor = true;

    public ScriptRunner() {
        this(Thread.currentThread().getContextClassLoader());
    }
    
    public ScriptRunner(ClassLoader classLoader) {
        this.context = new ExecutionContext(classLoader);
        this.parseContext = new ParseContext(classLoader);
        this.preprocessor = new Preprocessor();
        this.classLoader = classLoader;
        this.context.getBuiltins().setContextClassLoader(classLoader);
    }
    
    public ScriptRunner(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.context = new ExecutionContext(classLoader, outputHandler, errorHandler);
        this.parseContext = new ParseContext(classLoader);
        this.preprocessor = new Preprocessor();
        this.classLoader = classLoader;
        this.context.getBuiltins().setContextClassLoader(classLoader);
    }
    
    public ExecutionContext getExecutionContext() {
        return context;
    }
    
    public void setExecutionContext(ExecutionContext context) {
        this.context = context;
        this.context.getBuiltins().setContextClassLoader(context.getClassLoader());
    }
    
    public ParseContext getParseContext() {
        return parseContext;
    }
    
    public void setParseContext(ParseContext parseContext) {
        this.parseContext = parseContext;
    }


    public Object executeWithResult(String code) {
        return executeWithResult(code, "<stdin>");
    }

    public Object executeWithResult(String code, String sourceFileName) {
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            String processedCode = preprocess(code);
            Lexer lexer = new Lexer(processedCode, sourceFileName);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader, sourceFileName);
            BlockNode ast = parser.parse();
            if (context.isPrintAST()) {
                context.getOutputBuffer().println("AST:");
                context.getOutputBuffer().println(ast.formatString());
            }
            return ASTEvaluator.evaluate(ast, context);
        } catch (ParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw simplifyException(e);
        }
    }
    
    public Object executeWithResult(String code, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        context.setOutputBuffer(outputHandler);
        context.setWarnMsgBuffer(errorHandler);
        return executeWithResult(code);
    }

    public void execute(String code) {
        execute(code, true, "<stdin>");
    }

    public void execute(String code, boolean clearOutput, String sourceFileName) {
        if (clearOutput) {
            context.clearOutput();
            context.clearWarnMessages();
        }

        try {
            String processedCode = preprocess(code);
            Lexer lexer = new Lexer(processedCode, sourceFileName);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader, sourceFileName);
            BlockNode ast = parser.parse();
            ASTEvaluator.evaluate(ast, context);
        } catch (ParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw simplifyException(e);
        }
    }
    
    public void execute(String code, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        context.setOutputBuffer(outputHandler);
        context.setWarnMsgBuffer(errorHandler);
        execute(code);
    }
    
    public Map<String, Object> getAllVariablesAsObject() {
        Map<String, Object> result = new HashMap<>();
        Map<String, ExecutionContext.Variable> variables = context.getAllVariables();
        for (Map.Entry<String, ExecutionContext.Variable> entry : variables.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value);
        }
        return result;
    }
    
    public void clearVariables() {
        context.clearVariables();
    }

    public void clearImports() {
        parseContext.clearImports();
    }

    public List<ASTNode> tryParse(String code) {
        return tryParse(code, "<stdin>");
    }

    public List<ASTNode> tryParse(String code, String sourceFileName) {
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            Lexer lexer = new Lexer(code, sourceFileName);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader, sourceFileName);
            BlockNode ast = parser.parse();
            List<ASTNode> nodes = new ArrayList<>();
            if (ast.getStatements() != null) {
                nodes.addAll(ast.getStatements());
            }
            return nodes;
        } catch (ParseException e) {
            context.printlnWarn("Parse error: " + e.getMessage());
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        }
    }
    
    public String getOutput() {
        return context.getOutput();
    }
    
    public String getWarnMessages() {
        return context.getWarnMessages();
    }
    
    public void addImport(String importStmt) {
        context.addImport(importStmt);
        parseContext.addImport(importStmt);
    }
    
    public void setClassFinder(IClassFinder classFinder) {
        context.setClassFinder(classFinder);
        parseContext.setClassFinder(classFinder);
    }
    
    public IClassFinder getClassFinder() {
        return context.getClassFinder();
    }
    
    public void setVariable(String name, Object value) {
        context.setVariable(name, value, value != null ? value.getClass() : Object.class);
    }
    
    public Object getVariable(String name) {
        return context.getVariable(name).value;
    }
    
    public boolean hasVariable(String name) {
        return context.hasVariable(name);
    }
    
    public void deleteVariable(String name) {
        context.deleteVariable(name);
    }
    
    private RuntimeException simplifyException(Exception e) {
        Throwable rootCause = getRootCause(e);
        String message = e.getMessage();
        
        if (rootCause != e && rootCause != null) {
            message = message + " -> " + rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
        }
        
        String scriptCallStack = "";
        if (e instanceof EvaluationException) {
            scriptCallStack = ((EvaluationException) e).formatScriptCallStack();
        } else if (rootCause instanceof EvaluationException) {
            scriptCallStack = ((EvaluationException) rootCause).formatScriptCallStack();
        }
        
        if (!scriptCallStack.isEmpty()) {
            message = message + "\n\n" + scriptCallStack;
        }
        
        RuntimeException simplified = new RuntimeException(message, rootCause != null ? rootCause : e);
        simplified.setStackTrace(rootCause != null ? rootCause.getStackTrace() : e.getStackTrace());
        return simplified;
    }
    
    private Throwable getRootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    private String preprocess(String code) {
        if (!enablePreprocessor || preprocessor == null) {
            return code;
        }
        return preprocessor.process(code);
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
}
