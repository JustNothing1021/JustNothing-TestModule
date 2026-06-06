package com.justnothing.testmodule.command.functions.exportcontext;

public class ContextFieldInfo {

    private String category;

    private String label;

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
