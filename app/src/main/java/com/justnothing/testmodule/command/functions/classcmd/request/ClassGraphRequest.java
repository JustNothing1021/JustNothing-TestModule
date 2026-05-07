package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("ClassGraph")
@SubCommand("graph")
@AutoSerializable
public class ClassGraphRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名", required = true)
    private String className;

    @FlagParam(names = {"--no-subclasses", "--hide-subclasses"}, 
              negated = true,  // ★ 遇到→设为false!
              description = "隐藏子类")
    private boolean showSubclasses = true;

    @FlagParam(names = {"--no-interfaces", "--hide-interfaces"}, 
              negated = true,  // ★ 遇到→设为false!
              description = "隐藏接口")
    private boolean showInterfaces = true;

    @FlagParam(names = {"--compact"}, description = "紧凑模式")
    private boolean compactMode = false;

    @KeywordParam(name = "depth", description = "最大深度 (支持: --depth=10 或 --depth 10)")
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
