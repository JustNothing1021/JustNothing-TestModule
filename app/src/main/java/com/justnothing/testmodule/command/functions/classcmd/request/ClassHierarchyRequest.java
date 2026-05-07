package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("ClassHierarchy")
@SubCommand("hierarchy")
@AutoSerializable
public class ClassHierarchyRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名")
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
