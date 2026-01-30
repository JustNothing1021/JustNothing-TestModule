package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.utils.functions.Logger;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

public class ScriptLogger {

    public static final class InterpreterLogger extends Logger {
        @Override
        public String getTag() {
            return "TestInterpreter";
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
            System.out.println("[DEBUG] " + str);
        }

        @Override
        public void debug(Throwable th) {
            if (!enabled)
                return;
            System.out.println("[DEBUG] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void debug(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println("[DEBUG] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void info(String str) {
            if (!enabled)
                return;
            System.out.println("[INFO] " + str);
        }

        @Override
        public void info(Throwable th) {
            if (!enabled)
                return;
            System.out.println("[INFO] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void info(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println("[INFO] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void warn(String str) {
            if (!enabled)
                return;
            System.out.println("[WARN] " + str);
        }

        @Override
        public void warn(Throwable th) {
            if (!enabled)
                return;
            System.out.println("[WARN] " + th.getMessage());
            th.printStackTrace(System.out);
        }

        @Override
        public void warn(String str, Throwable th) {
            if (!enabled)
                return;
            System.out.println("[WARN] " + str);
            th.printStackTrace(System.out);
        }

        @Override
        public void error(String str) {
            if (!enabled)
                return;
            System.err.println("[ERROR] " + str);
        }

        @Override
        public void error(Throwable th) {
            if (!enabled)
                return;
            System.err.println("[ERROR] " + th.getMessage());
            th.printStackTrace(System.err);
        }

        @Override
        public void error(String str, Throwable th) {
            if (!enabled)
                return;
            System.err.println("[ERROR] " + str);
            th.printStackTrace(System.err);
        }
    }

    static final Logger logger = isStandaloneMode() ? new StandaloneLogger() : new InterpreterLogger();


}
