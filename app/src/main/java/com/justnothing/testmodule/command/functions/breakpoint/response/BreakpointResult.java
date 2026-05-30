package com.justnothing.testmodule.command.functions.breakpoint.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class BreakpointResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("breakpoints")
    private List<BreakpointInfo> breakpoints;
    @Expose @SerializedName("totalBreakpoints")
    private Integer totalBreakpoints;
    @Expose @SerializedName("activeBreakpoints")
    private Integer activeBreakpoints;

    public BreakpointResult() { super(); this.breakpoints = new ArrayList<>(); }
    public BreakpointResult(String requestId) { super(requestId); this.breakpoints = new ArrayList<>(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public List<BreakpointInfo> getBreakpoints() { return breakpoints; }
    public void setBreakpoints(List<BreakpointInfo> breakpoints) { this.breakpoints = breakpoints; }
    public void addBreakpoint(BreakpointInfo bp) { this.breakpoints.add(bp); }

    public Integer getTotalBreakpoints() { return totalBreakpoints; }
    public void setTotalBreakpoints(Integer totalBreakpoints) { this.totalBreakpoints = totalBreakpoints; }

    public Integer getActiveBreakpoints() { return activeBreakpoints; }
    public void setActiveBreakpoints(Integer activeBreakpoints) { this.activeBreakpoints = activeBreakpoints; }

    public static class BreakpointInfo {
        @Expose @SerializedName("id")
        private String id;
        @Expose @SerializedName("className")
        private String className;
        @Expose @SerializedName("methodName")
        private String methodName;
        @Expose @SerializedName("lineNumber")
        private Integer lineNumber;
        @Expose @SerializedName("enabled")
        private Boolean enabled;
        @Expose @SerializedName("hitCount")
        private Integer hitCount;

        public BreakpointInfo() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public Integer getLineNumber() { return lineNumber; }
        public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Integer getHitCount() { return hitCount; }
        public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    }
}
