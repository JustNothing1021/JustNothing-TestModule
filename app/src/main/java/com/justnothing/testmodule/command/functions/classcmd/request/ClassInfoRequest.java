package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

@SerializeKeyName("ClassInfo")
@SubCommand("info")
@AutoSerializable
public class ClassInfoRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名")
    private String className;

    @FlagParam(names = {"-v", "--verbose"}, description = "显示详细信息")
    private boolean isVerbose = false;

    @FlagParam(names = {"-i", "--interfaces"}, description = "显示实现的接口")
    private boolean showInterfaces = false;

    @FlagParam(names = {"-c", "--constructors"}, description = "显示构造函数")
    private boolean showConstructors = false;

    @FlagParam(names = {"-s", "--super"}, description = "显示父类信息")
    private boolean showSuper = false;

    @FlagParam(names = {"-m", "--modifiers"}, description = "显示修饰符信息")
    private boolean showModifiers = false;

    @FlagParam(names = {"-a", "--all"}, description = "显示所有信息 (默认)")
    private boolean showAll = true;

    private boolean showMethods;
    private boolean showFields;

    public ClassInfoRequest() {
        super();
        this.showMethods = true;
        this.showFields = true;
    }

    public ClassInfoRequest(String className) {
        this();
        this.className = className;
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public boolean isShowInterfaces() { return showInterfaces; }
    public void setShowInterfaces(boolean showInterfaces) { 
        this.showInterfaces = showInterfaces; 
        if (showInterfaces) this.showAll = false;
    }

    public boolean isShowConstructors() { return showConstructors; }
    public void setShowConstructors(boolean showConstructors) { 
        this.showConstructors = showConstructors; 
        if (showConstructors) this.showAll = false;
    }

    public boolean isShowSuper() { return showSuper; }
    public void setShowSuper(boolean showSuper) { 
        this.showSuper = showSuper; 
        if (showSuper) this.showAll = false;
    }

    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { 
        this.showModifiers = showModifiers; 
        if (showModifiers) this.showAll = false;
    }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }

    public boolean isVerbose() { return isVerbose; }
    public void setVerbose(boolean verbose) { isVerbose = verbose; }

    public boolean isShowMethods() { return showMethods; }
    public void setShowMethods(boolean showMethods) { this.showMethods = showMethods; }

    public boolean isShowFields() { return showFields; }
    public void setShowFields(boolean showFields) { this.showFields = showFields; }

    @Override
    public ClassInfoRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(ClassInfoRequest.class, args);
    }
}
