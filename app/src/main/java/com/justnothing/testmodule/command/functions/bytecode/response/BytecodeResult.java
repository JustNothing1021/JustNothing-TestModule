package com.justnothing.testmodule.command.functions.bytecode.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

public class BytecodeResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("className")
    private String className;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("bytecodeSize")
    private Long bytecodeSize;
    @Expose @SerializedName("methodCount")
    private Integer methodCount;
    @Expose @SerializedName("fieldCount")
    private Integer fieldCount;
    @Expose @SerializedName("interfaceCount")
    private Integer interfaceCount;
    @Expose @SerializedName("superClass")
    private String superClass;
    @Expose @SerializedName("modifiers")
    private Integer modifiers;
    @Expose @SerializedName("versionMajor")
    private String versionMajor;
    @Expose @SerializedName("versionMinor")
    private String versionMinor;
    @Expose @SerializedName("constantPoolCount")
    private Integer constantPoolCount;
    @Expose @SerializedName("magicValid")
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
}
