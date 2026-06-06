package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:graph")
public class ClassGraphRequest extends ClassCommandRequest {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--no-subclasses",
        description = "隐藏子类",
        aliases = {"--hide-subclasses"},
        serializedName = "showSubclasses",
        isNegated = true
    )
    private boolean showSubclasses = true;

    @CmdParam(
        name = "--no-interfaces",
        description = "隐藏接口",
        aliases = {"--hide-interfaces"},
        serializedName = "showInterfaces"
    )
    private boolean showInterfaces = true;

    @CmdParam(
        name = "--compact",
        description = "紧凑模式",
        serializedName = "compactMode"
    )
    private boolean compactMode = false;

    @CmdParam(
        name = "--depth",
        description = "最大深度 (支持: --depth=10 或 --depth 10)",
        serializedName = "maxDepth"
    )
    private int maxDepth = 10;

    public ClassGraphRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isShowSubclasses() { return showSubclasses; }
    public void setShowSubclasses(boolean showSubclasses) { this.showSubclasses = showSubclasses; }
    public boolean isShowInterfaces() { return showInterfaces; }
    public void setShowInterfaces(boolean showInterfaces) { this.showInterfaces = showInterfaces; }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public boolean isCompactMode() { return compactMode; }
    public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }
    @Override
    public ClassGraphRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(ClassGraphRequest.class, args);
    }
}
