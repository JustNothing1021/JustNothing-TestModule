package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("ClassGraph")
public class ClassGraphResult extends ClassCommandResult {

    private String className;

    private List<HierarchyLevel> hierarchy = new ArrayList<>();

    private List<String> subclasses = new ArrayList<>();

    private List<String> implementedInterfaces = new ArrayList<>();

    public ClassGraphResult() {
        super();
    }

    public ClassGraphResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public List<HierarchyLevel> getHierarchy() { return hierarchy; }
    public void setHierarchy(List<HierarchyLevel> hierarchy) { this.hierarchy = hierarchy; }
    public List<String> getSubclasses() { return subclasses; }
    public void setSubclasses(List<String> subclasses) { this.subclasses = subclasses; }
    public List<String> getImplementedInterfaces() { return implementedInterfaces; }
    public void setImplementedInterfaces(List<String> implementedInterfaces) { this.implementedInterfaces = implementedInterfaces; }

    public static class HierarchyLevel {
        public String className;

        public int depth;

        public List<String> interfaces = new ArrayList<>();

        public HierarchyLevel() {}

        public HierarchyLevel(String className, int depth, List<String> interfaces) {
            this.className = className;
            this.depth = depth;
            this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        }
    }
}
