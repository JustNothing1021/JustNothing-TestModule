package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.response.AnalyzeReportResult;

public class AnalyzeClassRequest extends ClassCommandRequest<AnalyzeReportResult> {

    @CmdParam(
        name = "class",
        description = ClassTexts.PARAM_CLASS_ANALYZE_CLASS_DESC,
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--fields",
        description = ClassTexts.PARAM_CLASS_ANALYZE_FIELDS_DESC,
        aliases = {"-f"},
        serializedName = "showFields"
    )
    private boolean showFields = true;

    @CmdParam(
        name = "--methods",
        description = ClassTexts.PARAM_CLASS_ANALYZE_METHODS_DESC,
        aliases = {"-m"},
        serializedName = "showMethods"
    )
    private boolean showMethods = true;

    @CmdParam(
        name = "--constructors",
        description = ClassTexts.PARAM_CLASS_ANALYZE_CONSTRUCTORS_DESC,
        aliases = {"-c"},
        serializedName = "showConstructors"
    )
    private boolean showConstructors = true;

    @CmdParam(
        name = "--interfaces",
        description = ClassTexts.PARAM_CLASS_ANALYZE_INTERFACES_DESC,
        aliases = {"-i"},
        serializedName = "showInterfaces"
    )
    private boolean showInterfaces = true;

    @CmdParam(
        name = "--super",
        description = ClassTexts.PARAM_CLASS_ANALYZE_SUPER_DESC,
        aliases = {"-s"},
        serializedName = "showSuper"
    )
    private boolean showSuper = true;

    @CmdParam(
        name = "--modifiers",
        description = ClassTexts.PARAM_CLASS_ANALYZE_MODIFIERS_DESC,
        serializedName = "showModifiers"
    )
    private boolean showModifiers = true;

    @CmdParam(
        name = "--all",
        description = ClassTexts.PARAM_CLASS_ANALYZE_ALL_DESC,
        aliases = {"-a"},
        serializedName = "showAll"
    )
    private boolean showAll = true;

    @CmdParam(
        name = "--verbose",
        description = ClassTexts.PARAM_CLASS_ANALYZE_VERBOSE_DESC,
        aliases = {"-v"},
        serializedName = "verbose"
    )
    private boolean isVerbose = false;

    @CmdParam(
        name = "--hierarchy",
        description = ClassTexts.PARAM_CLASS_ANALYZE_HIERARCHY_DESC,
        serializedName = "showHierarchy"
    )
    private boolean showHierarchy = true;

    @CmdParam(
        name = "--stats",
        description = ClassTexts.PARAM_CLASS_ANALYZE_STATS_DESC,
        serializedName = "showStats"
    )
    private boolean showStats = true;

    @CmdParam(
        name = "--raw",
        description = ClassTexts.PARAM_CLASS_ANALYZE_RAW_DESC,
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
}
