package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.functions.script.ScriptModels.*;
import com.justnothing.testmodule.command.functions.script.ASTNodes.*;
import com.justnothing.testmodule.command.output.IOutputHandler;
import static com.justnothing.testmodule.command.functions.script.ScriptLogger.*;
import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ScriptRunner {
        private ExecutionContext context;

        public ScriptRunner(ClassLoader classLoader) {
            this.context = new ExecutionContext(classLoader);
        }

        public ExecutionContext getContext() {
            return context;
        }

        public ExecutionContext setContext(ExecutionContext context) {
            this.context = context;
            return context;
        }

        public Object executeWithResult(String code) {
            context.clearOutput();
            context.clearWarnMessages();
            ScriptParser parser = new ScriptParser(code, context);
            ASTNode node;
            Object lastResult = null;

            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    try {
                        lastResult = node.evaluate(context);
                    } catch (Exception e) {
                        if (context.continueOnErrors) {
                            System.err.println("Error: " + e.getMessage());
                            System.err.println(getStackTraceString(e));
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                if (!context.continueOnErrors) {
                    System.err.println(getStackTraceString(e));
                }
            }
            return lastResult;
        }

        public Object executeWithResult(String code,
                IOutputHandler builtInOutStream,
                IOutputHandler builtInErrStream) {
            context.clearOutput();
            context.clearWarnMessages();
            context.setBuiltInOutputBuffer(builtInOutStream);
            context.setBuiltInErrorBuffer(builtInErrStream);
            ScriptParser parser = new ScriptParser(code, context);
            ASTNode node;
            Object lastResult = null;

            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    try {
                        lastResult = node.evaluate(context);
                    } catch (Exception e) {
                        if (context.continueOnErrors) {
                            System.err.println("Error: " + e.getMessage());
                            System.err.println(getStackTraceString(e));
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                if (!context.continueOnErrors) {
                    System.err.println(getStackTraceString(e));
                }
            }
            return lastResult;
        }

        public void execute(String code) {
            context.clearOutput();
            context.clearWarnMessages();
            ScriptParser parser = new ScriptParser(code, context);
            ASTNode node;
            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    try {
                        node.evaluate(context);
                    } catch (Exception e) {
                        if (context.continueOnErrors) {
                            System.err.println("Error: " + e.getMessage());
                            System.err.println(getStackTraceString(e));
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                if (!context.continueOnErrors) {
                    System.err.println(getStackTraceString(e));
                }
            }
        }

        public void execute(String code,
                IOutputHandler builtInOutStream,
                IOutputHandler builtInErrStream) {
            context.setBuiltInOutputBuffer(builtInOutStream);
            context.setBuiltInErrorBuffer(builtInErrStream);
            // context.clearVariables();
            context.clearOutput();
            context.clearWarnMessages();
            ScriptParser parser = new ScriptParser(code, context);
            ASTNode node;
            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    try {
                        node.evaluate(context);
                    } catch (Exception e) {
                        if (context.continueOnErrors) {
                            System.err.println("Error: " + e.getMessage());
                            System.err.println(getStackTraceString(e));
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                if (!context.continueOnErrors) {
                    System.err.println(getStackTraceString(e));
                }
            }
        }

        public Map<String, Object> getAllVariablesAsObject() {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Variable> entry : context.getAllVariables().entrySet()) {
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
            ScriptParser parser = new ScriptParser(code, context);
            List<ASTNode> nodes = new ArrayList<>();
            ASTNode node;

            try {
                while ((node = parser.parseCompletedStatement()) != null) {
                    nodes.add(node);
                }
            } catch (Exception e) {
                logger.warn("解析代码时出错: " + e.getMessage());
                return nodes;
            }
            return nodes;
        }

    }