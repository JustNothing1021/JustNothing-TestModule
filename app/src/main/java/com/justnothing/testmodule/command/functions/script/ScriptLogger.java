package com.justnothing.testmodule.command.functions.script;

//import cn.ncw.logger.log.NCWLoggerFactory;
import com.justnothing.testmodule.utils.functions.Logger;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;

import cn.ncw.logger.log.NCWLoggerFactory;

public class ScriptLogger {

    public static final String TAG = "ScriptLogger";
    private static NCWLoggerFactory ncwLogger = new NCWLoggerFactory(TAG);

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
            ncwLogger.debug(str, TAG);
        }

        @Override
        public void debug(Throwable th) {
            if (!enabled)
                return;
            ncwLogger.debug("出现错误", TAG, th);
        }

        @Override
        public void debug(String str, Throwable th) {
            if (!enabled)
                return;
            ncwLogger.debug(str, TAG, th);
        }

        @Override
        public void info(String str) {
            if (!enabled)
                return;
            ncwLogger.info(str, TAG);
        }

        @Override
        public void info(Throwable th) {
            if (!enabled)
                return;
            ncwLogger.info("出现错误", TAG, th);
        }

        @Override
        public void info(String str, Throwable th) {
            if (!enabled)
                return;
            ncwLogger.info(str, TAG, th);
        }

        @Override
        public void warn(String str) {
            if (!enabled)
                return;
            ncwLogger.warn(str, TAG);
        }

        @Override
        public void warn(Throwable th) {
            if (!enabled)
                return;
            ncwLogger.warn("出现错误", TAG, th);
        }

        @Override
        public void warn(String str, Throwable th) {
            if (!enabled)
                return;
            ncwLogger.warn(str, TAG, th);
        }

        @Override
        public void error(String str) {
            if (!enabled)
                return;
            ncwLogger.error(str, TAG);
        }

        @Override
        public void error(Throwable th) {
            if (!enabled)
                return;
            ncwLogger.error("出现错误", TAG, th);
        }

        @Override
        public void error(String str, Throwable th) {
            if (!enabled)
                return;
            ncwLogger.error(str, TAG, th);
        }
    }

    static final Logger logger = isStandaloneMode() ? new StandaloneLogger() : new InterpreterLogger();

}
