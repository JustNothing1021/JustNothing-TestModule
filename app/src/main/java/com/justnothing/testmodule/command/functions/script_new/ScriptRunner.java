package com.justnothing.testmodule.command.functions.script_new;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;
import com.justnothing.testmodule.command.functions.script_new.exception.ParseException;
import com.justnothing.testmodule.command.functions.script_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script_new.parser.ParseContext;
import com.justnothing.testmodule.command.functions.script_new.parser.Parser;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptRunner {
    
    private ExecutionContext context;
    private ParseContext parseContext;
    
    public ScriptRunner(ClassLoader classLoader) {
        this.context = new ExecutionContext(classLoader);
        this.parseContext = new ParseContext(classLoader);
    }
    
    public ScriptRunner(ClassLoader classLoader, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        this.context = new ExecutionContext(classLoader, outputHandler, errorHandler);
        this.parseContext = new ParseContext(classLoader);
    }
    
    public ExecutionContext getContext() {
        return context;
    }
    
    public void setContext(ExecutionContext context) {
        this.context = context;
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
            Parser parser = new Parser(lexer.tokenize(), parseContext);
            BlockNode ast = parser.parse();
            return ASTEvaluator.evaluate(ast, context);
        } catch (ParseException e) {
            context.printlnWarn("Parse error: " + e.getMessage());
            throw new RuntimeException("Parse error: " + e.getMessage(), e);
        } catch (EvaluationException e) {
            context.printlnWarn("Evaluation error: " + e.getMessage());
            throw new RuntimeException("Evaluation error: " + e.getMessage(), e);
        } catch (Exception e) {
            context.printlnWarn("Error: " + e.getMessage());
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }
    
    public Object executeWithResult(String code, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        context.setOutputBuffer(outputHandler);
        context.setWarnMsgBuffer(errorHandler);
        return executeWithResult(code);
    }
    
    public void execute(String code) {
        executeWithResult(code);
    }
    
    public void execute(String code, IOutputHandler outputHandler, IOutputHandler errorHandler) {
        executeWithResult(code, outputHandler, errorHandler);
    }
    
    public Map<String, Object> getAllVariablesAsObject() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> variables = context.getScopeManager().getAllVariables();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    public void clearVariables() {
        context.getScopeManager().clearAll();
    }
    
    public List<ASTNode> tryParse(String code) {
        context.clearOutput();
        context.clearWarnMessages();
        
        try {
            Lexer lexer = new Lexer(code);
            Parser parser = new Parser(lexer.tokenize(), parseContext);
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
        context.getScopeManager().declareVariable(name, value);
    }
    
    public Object getVariable(String name) {
        return context.getScopeManager().getVariable(name).getValue();
    }
    
    public boolean hasVariable(String name) {
        return context.getScopeManager().hasVariable(name);
    }
    
    public void deleteVariable(String name) {
        context.getScopeManager().deleteVariable(name);
    }
}
