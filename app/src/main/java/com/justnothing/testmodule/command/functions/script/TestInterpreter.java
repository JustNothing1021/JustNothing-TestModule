package com.justnothing.testmodule.command.functions.script;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

public class TestInterpreter {

    public static void main(String[] args) {

        ScriptRunner runner = new ScriptRunner(Thread.currentThread().getContextClassLoader());

        while (true) {
            String code;

            try {
                System.out.print(">>> ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                code = reader.readLine();
                if (code == null || code.trim().equals("exit")) {
                    break;
                }
                Object result = runner.executeWithResult(code);
                if (result != null) {
                    System.out.println(result);
                }
            } catch (Exception e) {
                System.err.println("执行出错: " + e.getMessage());
                System.err.println(getStackTraceString(e));
            }
        }
    }
}