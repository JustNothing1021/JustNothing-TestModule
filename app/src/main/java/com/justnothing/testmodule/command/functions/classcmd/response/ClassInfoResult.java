package com.justnothing.testmodule.command.functions.classcmd.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;

@SerializeKeyName("ClassInfo")
public class ClassInfoResult extends ClassCommandResult {

    @Expose @SerializedName("classInfo")
    private ClassInfo classInfo;

    public ClassInfoResult() {
        super();
    }

    public ClassInfoResult(String requestId) {
        super(requestId);
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }
}
