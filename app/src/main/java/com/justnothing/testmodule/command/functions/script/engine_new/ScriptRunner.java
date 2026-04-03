package com.justnothing.testmodule.command.functions.script.engine_new;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.EvaluationException;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.ParseException;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script.engine_new.parser.ParseContext;
import com.justnothing.testmodule.command.functions.script.engine_new.parser.Parser;
import com.justnothing.testmodule.command.output.IOutputHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptRunner {
    
    private ExecutionContext context;
    private ParseContext parseContext;
    private final ClassLoader classLoader;
    
    public ScriptRunner(ClassLoader classLoader) {
        this.context = new ExecutionContext(classLoader);
        this.parseContext = new ParseContext(classLoader);
        this.classLoader = classLoader;
        this.context.getBuiltins().setContextClassLoader(classLoader);
    }
    
    public ScriptRunner(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.context = new ExecutionContext(classLoader, outputHandler, errorHandler);
        this.parseContext = new ParseContext(classLoader);
        this.classLoader = classLoader;
        this.context.getBuiltins().setContextClassLoader(classLoader);
    }
    
    public ExecutionContext getContext() {
        return context;
    }
    
    public ExecutionContext setContext(ExecutionContext context) {
        this.context = context;
        this.context.getBuiltins().setContextClassLoader(context.getClassLoader());
        return context;
    }
    
    public ParseContext getParseContext() {
        return parseContext;
    }
    
    public void setParseContext(ParseContext parseContext) {
        this.parseContext = parseContext;
    }
    
    public Object executeWithResult(String code) {
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            Lexer lexer = new Lexer(code);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader);
            BlockNode ast = parser.parse();
            if (context.isPrintAST()) {
                context.getOutputBuffer().println("AST:");
                context.getOutputBuffer().println(ast.formatString());
            }
            return ASTEvaluator.evaluate(ast, context);
        } catch (ParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (EvaluationException e) {
            throw simplifyException(e);
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
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            Lexer lexer = new Lexer(code);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader);
            BlockNode ast = parser.parse();
            ASTEvaluator.evaluate(ast, context);
        } catch (ParseException e) {
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (EvaluationException e) {
            throw simplifyException(e);
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
    
    public List<ASTNode> tryParse(String code) {
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            Lexer lexer = new Lexer(code);
            Parser parser = new Parser(lexer.tokenize(), parseContext, classLoader);
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
        
        RuntimeException simplified = new RuntimeException(message);
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
}
