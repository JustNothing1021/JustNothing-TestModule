package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.utils.logging.Logger;


public class ParamParser {

    private static final Logger logger = Logger.getLoggerForName("ParamParser.v4.0");

    /**
     * 统一解析入口 (向后兼容)
     * 
     * 自动委托给 CmdParamProcessor，支持:
     * - @CmdParam (新注解)
     * - @PositionalParam / @KeywordParam / @FlagParam (旧注解)
     * 
     * @param requestClass 目标 Request 类
     * @param args 命令行参数
     * @return 解析完成后的请求实例
     * @throws IllegalCommandLineArgumentException 参数错误
     */
    public static <T extends CommandRequest> T parse(Class<T> requestClass, String[] args) throws IllegalCommandLineArgumentException {
        logger.debug("ParamParser.parse() 委托给 CmdParamProcessor");
        
        try {
            T request = requestClass.getDeclaredConstructor().newInstance();
            CmdParamProcessor.parseCommandLineArgs(request, args);
            return request;
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String detail = cause != null 
                ? cause.getClass().getSimpleName() + ": " + cause.getMessage() 
                : e.getMessage();
            throw new IllegalCommandLineArgumentException(
                "参数解析失败: " + e.getClass().getSimpleName() + " → " + detail
            );
        }
    }
}