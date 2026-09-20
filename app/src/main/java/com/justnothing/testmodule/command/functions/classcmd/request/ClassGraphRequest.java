package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassGraphResult;

public class ClassGraphRequest extends ClassCommandRequest<ClassGraphResult> {

    @CmdParam(
        name = "class",
        description = ClassTexts.PARAM_CLASS_GRAPH_CLASS_DESC,
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--no-subclasses",
        description = ClassTexts.PARAM_CLASS_GRAPH_NO_SUBCLASSES_DESC,
        aliases = {"--hide-subclasses"},
        serializedName = "showSubclasses",
        isNegated = true
    )
    private boolean showSubclasses = true;

    @CmdParam(
        name = "--no-interfaces",
        description = ClassTexts.PARAM_CLASS_GRAPH_NO_INTERFACES_DESC,
        aliases = {"--hide-interfaces"},
        serializedName = "showInterfaces"
    )
    private boolean showInterfaces = true;

    @CmdParam(
        name = "--compact",
        description = ClassTexts.PARAM_CLASS_GRAPH_COMPACT_DESC,
        serializedName = "compactMode"
    )
    private boolean compactMode = false;

    @CmdParam(
        name = "--depth",
        description = ClassTexts.PARAM_CLASS_GRAPH_DEPTH_DESC,
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
}
