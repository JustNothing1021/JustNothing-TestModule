package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:analyze")
public class AnalyzeClassRequest extends ClassCommandRequest {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--fields",
        description = "显示字段",
        aliases = {"-f"},
        serializedName = "showFields"
    )
    private boolean showFields = true;

    @CmdParam(
        name = "--methods",
        description = "显示方法",
        aliases = {"-m"},
        serializedName = "showMethods"
    )
    private boolean showMethods = true;

    @CmdParam(
        name = "--constructors",
        description = "显示构造函数",
        aliases = {"-c"},
        serializedName = "showConstructors"
    )
    private boolean showConstructors = true;

    @CmdParam(
        name = "--interfaces",
        description = "显示实现的接口",
        aliases = {"-i"},
        serializedName = "showInterfaces"
    )
    private boolean showInterfaces = true;

    @CmdParam(
        name = "--super",
        description = "显示父类信息",
        aliases = {"-s"},
        serializedName = "showSuper"
    )
    private boolean showSuper = true;

    @CmdParam(
        name = "--modifiers",
        description = "显示修饰符信息",
        serializedName = "showModifiers"
    )
    private boolean showModifiers = true;

    @CmdParam(
        name = "--all",
        description = "显示所有信息 (默认)",
        aliases = {"-a"},
        serializedName = "showAll"
    )
    private boolean showAll = true;

    @CmdParam(
        name = "--verbose",
        description = "显示详细信息",
        aliases = {"-v"},
        serializedName = "verbose"
    )
    private boolean isVerbose = false;

    @CmdParam(
        name = "--hierarchy",
        description = "显示继承层次",
        serializedName = "showHierarchy"
    )
    private boolean showHierarchy = true;

    @CmdParam(
        name = "--stats",
        description = "显示统计信息",
        serializedName = "showStats"
    )
    private boolean showStats = true;

    @CmdParam(
        name = "--raw",
        description = "原始输出格式",
        serializedName = "rawOutput"
    )
    private boolean rawOutput = false;

    public AnalyzeClassRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isShowFields() { return showFields; }
    public void setShowFields(boolean showFields) { this.showFields = showFields; }
    public boolean isShowMethods() { return showMethods; }
    public void setShowMethods(boolean showMethods) { this.showMethods = showMethods; }
    public boolean isShowConstructors() { return showConstructors; }
    public void setShowConstructors(boolean showConstructors) { this.showConstructors = showConstructors; }
    public boolean isShowInterfaces() { return showInterfaces; }
    public void setShowInterfaces(boolean showInterfaces) { this.showInterfaces = showInterfaces; }
    public boolean isShowSuper() { return showSuper; }
    public void setShowSuper(boolean showSuper) { this.showSuper = showSuper; }
    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { this.showModifiers = showModifiers; }
    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }
    public boolean isVerbose() { return isVerbose; }
    public void setVerbose(boolean verbose) { isVerbose = verbose; }
    public boolean isShowHierarchy() { return showHierarchy; }
    public void setShowHierarchy(boolean showHierarchy) { this.showHierarchy = showHierarchy; }
    public boolean isShowStats() { return showStats; }
    public void setShowStats(boolean showStats) { this.showStats = showStats; }
    public boolean isRawOutput() { return rawOutput; }
    public void setRawOutput(boolean rawOutput) { this.rawOutput = rawOutput; }
    @Override
    public AnalyzeClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(AnalyzeClassRequest.class, args);
    }
}
