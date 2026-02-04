package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.utils.functions.Logger;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

// import cn.ncw.logger.log.NCWLoggerFactory;

public class ScriptLogger {

    public static final String TAG = "ScriptLogger";
    // private static NCWLoggerFactory ncwLogger = new NCWLoggerFactory(TAG);

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
            // ncwLogger.debug(str, TAG);
            System.out.println("[DEBUG] " + str);
        }

        @Override
        public void debug(Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.debug("出现错误", TAG, th);
            System.out.println("[DEBUG] 出现错误");
            th.printStackTrace();
        }

        @Override
        public void debug(String str, Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.debug(str, TAG, th);
            System.out.println("[DEBUG] " + str);
            th.printStackTrace();
        }

        @Override
        public void info(String str) {
            if (!enabled)
                return;
            // ncwLogger.info(str, TAG);
            System.out.println("[INFO] " + str);
        }

        @Override
        public void info(Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.info("出现错误", TAG, th);
            System.out.println("[INFO] 出现错误");
            th.printStackTrace();
        }

        @Override
        public void info(String str, Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.info(str, TAG, th);
            System.out.println("[INFO] " + str);
            th.printStackTrace();
        }

        @Override
        public void warn(String str) {
            if (!enabled)
                return;
            // ncwLogger.warn(str, TAG);
            System.out.println("[WARN] " + str);
        }

        @Override
        public void warn(Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.warn("出现错误", TAG, th);
            System.out.println("[WARN] 出现错误");
            th.printStackTrace();
        }

        @Override
        public void warn(String str, Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.warn(str, TAG, th);
            System.out.println("[WARN] " + str);
            th.printStackTrace();
        }

        @Override
        public void error(String str) {
            if (!enabled)
                return;
            // ncwLogger.error(str, TAG);
            System.err.println("[ERROR] " + str);
        }

        @Override
        public void error(Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.error("出现错误", TAG, th);
            System.err.println("[ERROR] 出现错误");
            th.printStackTrace();
        }

        @Override
        public void error(String str, Throwable th) {
            if (!enabled)
                return;
            // ncwLogger.error(str, TAG, th);
            System.err.println("[ERROR] " + str);
            th.printStackTrace();
        }
    }

    static final Logger logger = isStandaloneMode() ? new StandaloneLogger() : new InterpreterLogger();

}
