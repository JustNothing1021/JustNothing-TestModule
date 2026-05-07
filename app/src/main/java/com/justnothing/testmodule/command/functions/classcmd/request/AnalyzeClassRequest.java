package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("AnalyzeClass")
@SubCommand("analyze")
@AutoSerializable
public class AnalyzeClassRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名", required = true)
    private String className;

    @FlagParam(names = {"-f", "--fields"}, description = "显示字段", defaultValue = true)
    private boolean showFields = true;

    @FlagParam(names = {"-m", "--methods"}, description = "显示方法", defaultValue = true)
    private boolean showMethods = true;

    @FlagParam(names = {"-c", "--constructors"}, description = "显示构造函数", defaultValue = true)
    private boolean showConstructors = true;

    @FlagParam(names = {"-i", "--interfaces"}, description = "显示实现的接口", defaultValue = true)
    private boolean showInterfaces = true;

    @FlagParam(names = {"-s", "--super"}, description = "显示父类信息", defaultValue = true)
    private boolean showSuper = true;

    @FlagParam(names = {"--modifiers"}, description = "显示修饰符信息", defaultValue = true)
    private boolean showModifiers = true;

    @FlagParam(names = {"-a", "--all"}, description = "显示所有信息 (默认)", defaultValue = true)
    private boolean showAll = true;

    @FlagParam(names = {"-v", "--verbose"}, description = "显示详细信息")
    private boolean isVerbose = false;

    @FlagParam(names = {"--hierarchy"}, description = "显示继承层次", defaultValue = true)
    private boolean showHierarchy = true;

    @FlagParam(names = {"--stats"}, description = "显示统计信息", defaultValue = true)
    private boolean showStats = true;

    @FlagParam(names = {"--raw"}, description = "原始输出格式")
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
