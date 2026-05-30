package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("WatchAdd")
public class WatchAddRequest extends CommandRequest {

    @CmdParam(
        name = "targetType",
        description = "监控类型 (field/method)",
        required = true,
        position = 1,
        allowedValues = {"field", "method"}
    )
    private String targetType;

    @CmdParam(
        name = "className",
        description = "类名",
        required = true,
        position = 2
    )
    private String className;

    @CmdParam(
        name = "memberName",
        description = "成员名（字段名或方法名）",
        required = true,
        position = 3
    )
    private String memberName;

    @CmdParam(
        name = "signature",
        description = "方法签名（仅method类型有效）",
        required = false,
        aliases = {"sig"}
    )
    private String signature;

    @CmdParam(
        name = "interval",
        description = "检查间隔(ms)，默认1000",
        required = false,
        defaultValue = "1000",
        position = 4
    )
    private Long interval;

    public WatchAddRequest() {
        super();
        this.interval = 1000L;
    }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public Long getInterval() { return interval; }
    public void setInterval(Long interval) { this.interval = interval; }
}
