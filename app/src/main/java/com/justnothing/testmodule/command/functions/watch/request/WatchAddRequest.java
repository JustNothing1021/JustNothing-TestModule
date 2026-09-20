package com.justnothing.testmodule.command.functions.watch.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.watch.response.WatchAddResult;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

public class WatchAddRequest extends CommandRequest<WatchAddResult> {

    @CmdParam(
        name = "targetType",
        description = WatchTexts.PARAM_WATCH_ADD_TARGETTYPE_DESC,
        required = true,
        position = 1,
        allowedValues = {"field", "method"}
    )
    private String targetType;

    @CmdParam(
        name = "className",
        description = WatchTexts.PARAM_WATCH_ADD_CLASSNAME_DESC,
        required = true,
        position = 2
    )
    private String className;

    @CmdParam(
        name = "memberName",
        description = WatchTexts.PARAM_WATCH_ADD_MEMBERNAME_DESC,
        required = true,
        position = 3
    )
    private String memberName;

    @CmdParam(
        name = "signature",
        description = WatchTexts.PARAM_WATCH_ADD_SIGNATURE_DESC,
        required = false,
        aliases = {"sig"}
    )
    private String signature;

    @CmdParam(
        name = "interval",
        description = WatchTexts.PARAM_WATCH_ADD_INTERVAL_DESC,
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
