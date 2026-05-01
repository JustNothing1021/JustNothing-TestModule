package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class FieldInfoResult extends ClassCommandResult {

    private String className;
    private String fieldName;
    private String operation;  // "list", "get", "set", "info"
    private FieldInfo fieldInfo;
    private List<FieldInfo> fieldList;
    private Object fieldValue;
    private String valueToSet;
    private boolean success;
    private int totalCount;

    public FieldInfoResult() {
        super();
        this.fieldList = new ArrayList<>();
    }

    public FieldInfoResult(String requestId) {
        super(requestId);
        this.fieldList = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public void setFieldInfo(FieldInfo fieldInfo) {
        this.fieldInfo = fieldInfo;
    }

    public List<FieldInfo> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<FieldInfo> fieldList) {
        this.fieldList = fieldList;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(Object fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getValueToSet() {
        return valueToSet;
    }

    public void setValueToSet(String valueToSet) {
        this.valueToSet = valueToSet;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("operation", operation);
        obj.put("success", success);

        if (fieldName != null) {
            obj.put("fieldName", fieldName);
        }

        if (fieldInfo != null) {
            obj.put("fieldInfo", fieldInfo.toJson());
        }

        if (!fieldList.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (FieldInfo info : fieldList) {
                arr.put(info.toJson());
            }
            obj.put("fields", arr);
            obj.put("totalCount", totalCount);
        }

        if (fieldValue != null) {
            obj.put("fieldValue", fieldValue.toString());
            obj.put("fieldValueType", fieldValue.getClass().getName());
        } else if ("get".equals(operation)) {
            obj.put("fieldValue", JSONObject.NULL);
        }

        if (valueToSet != null) {
            obj.put("valueToSet", valueToSet);
        }

        return obj;
    }
}
