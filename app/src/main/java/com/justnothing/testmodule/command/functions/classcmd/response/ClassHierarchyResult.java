package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("ClassHierarchy")
public class ClassHierarchyResult extends ClassCommandResult {

    private List<HierarchyClassInfo> classChain = new ArrayList<>();

    private List<List<String>> interfacesPerLevel = new ArrayList<>();

    public ClassHierarchyResult() {
        super();
    }

    public ClassHierarchyResult(String requestId) {
        super(requestId);
    }

    public List<HierarchyClassInfo> getClassChain() { return classChain; }
    public void setClassChain(List<HierarchyClassInfo> classChain) { this.classChain = classChain; }
    public List<List<String>> getInterfacesPerLevel() { return interfacesPerLevel; }
    public void setInterfacesPerLevel(List<List<String>> interfacesPerLevel) { this.interfacesPerLevel = interfacesPerLevel; }

    public static class HierarchyClassInfo {
        private String name;

        private boolean isInterface;

        private boolean isAnnotation;

        private boolean isEnum;

        private boolean isAbstract;

        private boolean isFinal;

        public HierarchyClassInfo() {}

        public HierarchyClassInfo(String name, boolean isInterface, boolean isAnnotation,
                                  boolean isEnum, boolean isAbstract, boolean isFinal) {
            this.name = name;
            this.isInterface = isInterface;
            this.isAnnotation = isAnnotation;
            this.isEnum = isEnum;
            this.isAbstract = isAbstract;
            this.isFinal = isFinal;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isInterface() { return isInterface; }
        public void setInterface(boolean anInterface) { isInterface = anInterface; }
        public boolean isAnnotation() { return isAnnotation; }
        public void setAnnotation(boolean annotation) { isAnnotation = annotation; }
        public boolean isEnum() { return isEnum; }
        public void setEnum(boolean anEnum) { isEnum = anEnum; }
        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean anAbstract) { isAbstract = anAbstract; }
        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean aFinal) { isFinal = aFinal; }
    }
}
