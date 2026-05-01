package com.justnothing.testmodule.command.functions.bytecode.response;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class BytecodeResult extends CommandResult {

    private String subCommand;
    private String className;
    private String output;
    private Long bytecodeSize;
    private Integer methodCount;
    private Integer fieldCount;
    private Integer interfaceCount;
    private String superClass;
    private Integer modifiers;
    private String versionMajor;
    private String versionMinor;
    private Integer constantPoolCount;
    private Boolean magicValid;

    public BytecodeResult() {
        super();
    }

    public BytecodeResult(String requestId) {
        super(requestId);
    }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Long getBytecodeSize() { return bytecodeSize; }
    public void setBytecodeSize(Long bytecodeSize) { this.bytecodeSize = bytecodeSize; }

    public Integer getMethodCount() { return methodCount; }
    public void setMethodCount(Integer methodCount) { this.methodCount = methodCount; }

    public Integer getFieldCount() { return fieldCount; }
    public void setFieldCount(Integer fieldCount) { this.fieldCount = fieldCount; }

    public Integer getInterfaceCount() { return interfaceCount; }
    public void setInterfaceCount(Integer interfaceCount) { this.interfaceCount = interfaceCount; }

    public String getSuperClass() { return superClass; }
    public void setSuperClass(String superClass) { this.superClass = superClass; }

    public Integer getModifiers() { return modifiers; }
    public void setModifiers(Integer modifiers) { this.modifiers = modifiers; }

    public String getVersionMajor() { return versionMajor; }
    public void setVersionMajor(String versionMajor) { this.versionMajor = versionMajor; }

    public String getVersionMinor() { return versionMinor; }
    public void setVersionMinor(String versionMinor) { this.versionMinor = versionMinor; }

    public Integer getConstantPoolCount() { return constantPoolCount; }
    public void setConstantPoolCount(Integer constantPoolCount) { this.constantPoolCount = constantPoolCount; }

    public Boolean getMagicValid() { return magicValid; }
    public void setMagicValid(Boolean magicValid) { this.magicValid = magicValid; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (className != null) obj.put("className", className);
        if (output != null) obj.put("output", output);
        if (bytecodeSize != null) obj.put("bytecodeSize", bytecodeSize);
        if (methodCount != null) obj.put("methodCount", methodCount);
        if (fieldCount != null) obj.put("fieldCount", fieldCount);
        if (interfaceCount != null) obj.put("interfaceCount", interfaceCount);
        if (superClass != null) obj.put("superClass", superClass);
        if (modifiers != null) obj.put("modifiers", modifiers);
        if (versionMajor != null) obj.put("versionMajor", versionMajor);
        if (versionMinor != null) obj.put("versionMinor", versionMinor);
        if (constantPoolCount != null) obj.put("constantPoolCount", constantPoolCount);
        if (magicValid != null) obj.put("magicValid", magicValid);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        subCommand = obj.optString("subCommand", null);
        className = obj.optString("className", null);
        output = obj.optString("output", null);
        bytecodeSize = obj.has("bytecodeSize") ? obj.getLong("bytecodeSize") : null;
        methodCount = obj.has("methodCount") ? obj.getInt("methodCount") : null;
        fieldCount = obj.has("fieldCount") ? obj.getInt("fieldCount") : null;
        interfaceCount = obj.has("interfaceCount") ? obj.getInt("interfaceCount") : null;
        superClass = obj.optString("superClass", null);
        modifiers = obj.has("modifiers") ? obj.getInt("modifiers") : null;
        versionMajor = obj.optString("versionMajor", null);
        versionMinor = obj.optString("versionMinor", null);
        constantPoolCount = obj.has("constantPoolCount") ? obj.getInt("constantPoolCount") : null;
        magicValid = obj.has("magicValid") ? obj.getBoolean("magicValid") : null;
    }
}
