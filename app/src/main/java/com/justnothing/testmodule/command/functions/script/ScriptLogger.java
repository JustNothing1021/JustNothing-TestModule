package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.utils.functions.Logger;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;


public class ScriptLogger {

    public static final String TAG = "ScriptLogger";

    private static final String RESET = "\u001B[0m";
    private static final String GRAY = "\u001B[90m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";




    public static final class InterpreterLogger extends Logger {
        @Override
        public String getTag() {
            return TAG;
        }
    }


    public static final class StandaloneLogger extends Logger {
        public static boolean enabled = true;

        @Override
        public String getTag() {
            return "TestInterpreter";
        }

        @Override
        public void debug(String str) {
            if (!enabled)
                return;
            System.out.println(GRAY + "[" + TAG + "] " + str + RESET);
        }

        @Override
        public void debug(Throwable th) {
            if (!enabled)
                return;
            System.out.println(GRAY + "[" + TAG + "] 出现错误" + RESET);
            th.printStackTrace();
        }

        @Override
        public void debug(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println(GRAY + "[" + TAG + "] " + str + RESET);
            th.printStackTrace();
        }

        @Override
        public void info(String str) {
            if (!enabled)
                return;
            System.out.println(GREEN + "[" + TAG + "] " + str + RESET);
        }

        @Override
        public void info(Throwable th) {
            if (!enabled)
                return;
            System.out.println(GREEN + "[" + TAG + "] 出现错误" + RESET);
            th.printStackTrace();
        }

        @Override
        public void info(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println(GREEN + "[" + TAG + "] " + str + RESET);
            th.printStackTrace();
        }

        @Override
        public void warn(String str) {
            if (!enabled)
                return;
            System.out.println(YELLOW + "[" + TAG + "] " + str + RESET);
        }

        @Override
        public void warn(Throwable th) {
            if (!enabled)
                return;
            System.out.println(YELLOW + "[" + TAG + "] 出现错误" + RESET);
            th.printStackTrace();
        }

        @Override
        public void warn(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println(YELLOW + "[" + TAG + "] " + str + RESET);
            th.printStackTrace();
        }

        @Override
        public void error(String str) {
            if (!enabled)
                return;
            System.err.println(RED + "[" + TAG + "] " + str + RESET);
        }

        @Override
        public void error(Throwable th) {
            if (!enabled)
                return;
            System.err.println(RED + "[" + TAG + "] 出现错误" + RESET);
            th.printStackTrace();
        }

        @Override
        public void error(String str, Throwable th) {
            if (!enabled)
                return;
            System.err.println(RED + "[" + TAG + "] " + str + RESET);
            th.printStackTrace();
        }
    }

    static final Logger logger = isStandaloneMode() ? new StandaloneLogger() : new InterpreterLogger();
}
