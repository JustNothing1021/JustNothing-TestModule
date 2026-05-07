package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("ClassHierarchy")
@AutoSerializable
public class ClassHierarchyResult extends ClassCommandResult {

    @ResultField(name = "classChain", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<HierarchyClassInfo> classChain = new ArrayList<>();

    @ResultField(name = "interfacesPerLevel", defaultValue = ValueSupplier.EmptyListSupplier.class)
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

    @AutoSerializable
    public static class HierarchyClassInfo {
        @ResultField(name = "name")
        private String name;

        @ResultField(name = "isInterface", defaultValue = ValueSupplier.FalseSupplier.class)
        private boolean isInterface;

        @ResultField(name = "isAnnotation", defaultValue = ValueSupplier.FalseSupplier.class)
        private boolean isAnnotation;

        @ResultField(name = "isEnum", defaultValue = ValueSupplier.FalseSupplier.class)
        private boolean isEnum;

        @ResultField(name = "isAbstract", defaultValue = ValueSupplier.FalseSupplier.class)
        private boolean isAbstract;

        @ResultField(name = "isFinal", defaultValue = ValueSupplier.FalseSupplier.class)
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
