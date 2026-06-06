package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("network:mock")
public class NetworkMockRequest extends CommandRequest {

    @CmdParam(name = "subCommand", required = false, description = "子命令 (add/header/remove/list/clear)")
    private String subCommand;

    @CmdParam(name = "pattern", required = false, description = "匹配模式")
    private String pattern;

    @CmdParam(name = "response", required = false, description = "响应内容")
    private String response;

    @CmdParam(name = "statusCode", required = false, description = "状态码")
    private Integer statusCode;

    @CmdParam(name = "headerName", required = false, description = "头部名称")
    private String headerName;

    @CmdParam(name = "headerValue", required = false, description = "头部值")
    private String headerValue;

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }

    public String getHeaderValue() { return headerValue; }
    public void setHeaderValue(String headerValue) { this.headerValue = headerValue; }
}
