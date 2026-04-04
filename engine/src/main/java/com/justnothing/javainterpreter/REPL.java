package com.justnothing.javainterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.lang.reflect.Array;

import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.evaluator.ASTEvaluator;
import com.justnothing.javainterpreter.evaluator.ExecutionContext;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.lexer.Lexer;
import com.justnothing.javainterpreter.parser.ParseContext;
import com.justnothing.javainterpreter.parser.Parser;

public class REPL {
    
    public static void main(String[] args) {
        System.out.println("=== Script Parser REPL ===");
        System.out.println("输入代码查看AST，输入 'exit' 或 'quit' 退出");
        System.out.println("支持的功能:");
        System.out.println("  - 变量声明: int x = 10;");
        System.out.println("  - auto关键字: auto str = java.lang.System.out;");
        System.out.println("  - Lambda表达式: x -> x + 1;");
        System.out.println("  - 控制流: if, for, while");
        System.out.println("  - 方法调用: System.out.println(1);");
        System.out.println("  - :multi 进入多行输入模式");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        ExecutionContext context = new ExecutionContext(REPL.class.getClassLoader());
        ParseContext parseContext = new ParseContext();
        parseContext.setClassLoader(REPL.class.getClassLoader());
        List<String> pendingLines = new ArrayList<>();
        
        while (true) {
            String prompt = pendingLines.isEmpty() ? "> " : "... ";
            System.out.print(prompt);
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("再见！");
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                System.out.println("支持的命令:");
                System.out.println("  help   - 显示帮助信息");
                System.out.println("  exit   - 退出REPL");
                System.out.println("  quit   - 退出REPL");
                System.out.println("  :multi - 进入多行输入模式");
                System.out.println("  :eval  - 执行多行输入的代码");
                System.out.println("  :clear - 清空多行输入缓冲区");
                System.out.println("  :show  - 显示当前多行输入缓冲区内容");
                System.out.println();
                continue;
            }
            
            if (input.equalsIgnoreCase(":multi")) {
                System.out.println("进入多行输入模式，输入 :eval 执行，输入 :clear 清空，输入 :exit 退出多行模式");
                List<String> lines = new ArrayList<>();
                
                while (true) {
                    System.out.print("... ");
                    String line;
                    try {
                        line = scanner.nextLine().trim();
                    } catch (Exception e) {
                        break;
                    }
                    
                    if (line.equalsIgnoreCase(":exit")) {
                        System.out.println("退出多行输入模式");
                        break;
                    }
                    
                    if (line.equalsIgnoreCase(":eval")) {
                        if (lines.isEmpty()) {
                            System.out.println("没有输入代码");
                            continue;
                        }
                        
                        String code = String.join("\n", lines);
                        executeCode(code, context, parseContext);
                        lines.clear();
                        continue;
                    }
                    
                    if (line.equalsIgnoreCase(":clear")) {
                        lines.clear();
                        System.out.println("已清空输入缓冲区");
                        continue;
                    }
                    
                    if (line.equalsIgnoreCase(":show")) {
                        if (lines.isEmpty()) {
                            System.out.println("(缓冲区为空)");
                        } else {
                            for (int i = 0; i < lines.size(); i++) {
                                System.out.println((i + 1) + ": " + lines.get(i));
                            }
                        }
                        continue;
                    }
                    
                    lines.add(line);
                }
                continue;
            }
            
            pendingLines.add(input);
            String code = String.join("\n", pendingLines);
            
            if (!isCodeComplete(code)) {
                continue;
            }
            
            executeCode(code, context, parseContext);
            pendingLines.clear();
        }
        
        scanner.close();
    }
    
    private static boolean isCodeComplete(String code) {
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inFString = false;
        boolean escape = false;
        
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\' && (inString || inChar || inFString)) {
                escape = true;
                continue;
            }
            
            if (c == '"' && !inChar) {
                if (i > 0 && code.charAt(i - 1) == 'f' && !inString && !inFString) {
                    inFString = !inFString;
                } else if (inFString) {
                    inFString = false;
                } else {
                    inString = !inString;
                }
                continue;
            }
            
            if (c == '\'' && !inString && !inFString) {
                inChar = !inChar;
                continue;
            }
            
            if (inString || inChar || inFString) {
                continue;
            }
            
            switch (c) {
                case '{': braceCount++; break;
                case '}': braceCount--; break;
                case '(': parenCount++; break;
                case ')': parenCount--; break;
                case '[': bracketCount++; break;
                case ']': bracketCount--; break;
            }
        }
        
        return braceCount <= 0 && parenCount <= 0 && bracketCount <= 0;
    }
    
    private static void executeCode(String input, ExecutionContext context, ParseContext parseContext) {
        try {
            Lexer lexer = new Lexer(input);
            Parser parser = new Parser(lexer.tokenize(), parseContext);
            BlockNode ast = parser.parse();
            
            System.out.println("AST:");
            System.out.println(ast.formatString());
            
            Object result = ASTEvaluator.evaluate(ast, context);
            
            System.out.println("Result: " + formatValue(result));
        } catch (ParseException e) {
            System.out.println("错误: " + e.getMessage());
        } catch (EvaluationException e) {
            System.out.println("错误: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("内部错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(Array.get(value, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
}
