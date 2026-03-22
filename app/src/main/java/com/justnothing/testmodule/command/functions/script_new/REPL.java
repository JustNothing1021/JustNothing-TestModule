package com.justnothing.testmodule.command.functions.script_new;

import java.util.Scanner;

import com.justnothing.testmodule.command.functions.script_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script_new.exception.ParseException;
import com.justnothing.testmodule.command.functions.script_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script_new.parser.Parser;

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
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("> ");
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
                System.out.println("  help  - 显示帮助信息");
                System.out.println("  exit  - 退出REPL");
                System.out.println("  quit  - 退出REPL");
                System.out.println();
                continue;
            }
            
            try {
                Lexer lexer = new Lexer(input);
                Parser parser = new Parser(lexer.tokenize());
                BlockNode ast = parser.parse();
                
                System.out.println("AST:");
                System.out.println(ast.formatString());
            } catch (ParseException e) {
                System.out.println("错误: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("内部错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        scanner.close();
    }
}
