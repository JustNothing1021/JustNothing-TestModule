package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:hierarchy")
public class ClassHierarchyRequest extends ClassCommandRequest {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    public ClassHierarchyRequest() {
        super();
    }

    public ClassHierarchyRequest(String className) {
        super();
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
