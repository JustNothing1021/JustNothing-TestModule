package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:info")
public class ClassInfoRequest extends ClassCommandRequest {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--verbose",
        description = "显示详细信息",
        aliases = {"-v"},
        required = false,
        serializedName = "verbose"
    )
    private boolean isVerbose = false;

    @CmdParam(
        name = "--interfaces",
        description = "显示实现的接口",
        aliases = {"-i"},
        required = false,
        serializedName = "showInterfaces"
    )
    private boolean showInterfaces = false;

    @CmdParam(
        name = "--constructors",
        description = "显示构造函数",
        aliases = {"-c"},
        required = false,
        serializedName = "showConstructors"
    )
    private boolean showConstructors = false;

    @CmdParam(
        name = "--super",
        description = "显示父类信息",
        aliases = {"-s"},
        required = false,
        serializedName = "showSuper"
    )
    private boolean showSuper = false;

    @CmdParam(
        name = "--modifiers",
        description = "显示修饰符信息",
        aliases = {"-m"},
        required = false,
        serializedName = "showModifiers"
    )
    private boolean showModifiers = false;

    @CmdParam(
        name = "--all",
        description = "显示所有信息 (默认)",
        aliases = {"-a"},
        required = false,
        serializedName = "showAll"
    )
    private boolean showAll = true;

    // 追踪哪些字段被用户显式设置了（用于区分默认值和用户意图）
    private boolean constructorsExplicitlySet = false;
    private boolean interfacesExplicitlySet = false;
    private boolean superExplicitlySet = false;
    private boolean modifiersExplicitlySet = false;

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
        this.interfacesExplicitlySet = true;
        if (showInterfaces) this.showAll = false;
    }

    public boolean isShowConstructors() { return showConstructors; }
    public void setShowConstructors(boolean showConstructors) {
        this.showConstructors = showConstructors;
        this.constructorsExplicitlySet = true;
        if (showConstructors) this.showAll = false;
    }

    public boolean isShowSuper() { return showSuper; }
    public void setShowSuper(boolean showSuper) {
        this.showSuper = showSuper;
        this.superExplicitlySet = true;
        if (showSuper) this.showAll = false;
    }

    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) {
        this.showModifiers = showModifiers;
        this.modifiersExplicitlySet = true;
        if (showModifiers) this.showAll = false;
    }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }

    /** 用户是否显式设置了 --constructors（用于区分默认值和用户意图） */
    public boolean wasConstructorsExplicitlySet() { return constructorsExplicitlySet; }
    /** 用户是否显式设置了 --interfaces */
    public boolean wasInterfacesExplicitlySet() { return interfacesExplicitlySet; }
    /** 用户是否显式设置了 --super */
    public boolean wasSuperExplicitlySet() { return superExplicitlySet; }
    /** 用户是否显式设置了 --modifiers */
    public boolean wasModifiersExplicitlySet() { return modifiersExplicitlySet; }

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
