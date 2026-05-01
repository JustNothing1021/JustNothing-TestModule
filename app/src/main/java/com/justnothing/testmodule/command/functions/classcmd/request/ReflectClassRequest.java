package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReflectClassRequest extends ClassCommandRequest {

    private String className;
    private String operationType;
    private String memberName;
    private String valueToSet;
    private boolean accessSuper;
    private boolean accessInterfaces;
    private boolean rawOutput;

    public ReflectClassRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getValueToSet() { return valueToSet; }
    public void setValueToSet(String valueToSet) { this.valueToSet = valueToSet; }
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }
    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }
    public boolean isRawOutput() { return rawOutput; }
    public void setRawOutput(boolean rawOutput) { this.rawOutput = rawOutput; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("operationType", operationType);
        obj.put("memberName", memberName);
        if (valueToSet != null) obj.put("valueToSet", valueToSet);
        obj.put("accessSuper", accessSuper);
        obj.put("accessInterfaces", accessInterfaces);
        obj.put("rawOutput", rawOutput);
        return obj;
    }

    @Override
    public ReflectClassRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setOperationType(obj.optString("operationType"));
        setMemberName(obj.optString("memberName"));
        setValueToSet(obj.optString("valueToSet", null));
        setAccessSuper(obj.optBoolean("accessSuper", false));
        setAccessInterfaces(obj.optBoolean("accessInterfaces", false));
        setRawOutput(obj.optBoolean("rawOutput", false));
        return this;
    }

    @Override
    public ReflectClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 3) {
            throw new IllegalCommandLineArgumentException(
                "参数不足: class reflect <class> <type> <name> [options]");
        }

        className = args[0];
        operationType = args[1];
        memberName = args[2];

        List<String> params = new ArrayList<>();
        boolean valueSet = false;

        for (int i = 3; i < args.length; i++) {
            switch (args[i]) {
                case "-s", "--super" -> accessSuper = true;
                case "-i", "--interfaces" -> accessInterfaces = true;
                case "-r", "--raw" -> rawOutput = true;
                case "-v", "--value" -> {
                    if (i + 1 < args.length) {
                        valueToSet = args[++i];
                        valueSet = true;
                    } else {
                        throw new IllegalCommandLineArgumentException(
                            "提供给-v的参数不足: class reflect ... --value <value>");
                    }
                }
                case "-p", "--params" -> {
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        params.add(args[++i]);
                    }
                }
                default -> {
                    if (!args[i].startsWith("-") && !valueSet) {
                        valueToSet = args[i];
                        valueSet = true;
                    }
                }
            }
        }

        return this;
    }
}
