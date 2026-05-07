package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;

@AutoSerializable
public class ContextFieldInfo {

    @ResultField(name = "category")
    private String category;

    @ResultField(name = "label")
    private String label;

    @ResultField(name = "value")
    private String value;

    public ContextFieldInfo() {
    }

    public ContextFieldInfo(String category, String label, String value) {
        this.category = category;
        this.label = label;
        this.value = value;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
