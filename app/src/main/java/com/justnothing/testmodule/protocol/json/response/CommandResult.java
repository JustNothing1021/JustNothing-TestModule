package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONObject;

public class CommandResult {
    
    private String requestId;
    private boolean success;
    private ErrorInfo error;
    
    public CommandResult() {
        this.success = true;
    }
    
    public CommandResult(String requestId) {
        this.requestId = requestId;
        this.success = true;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public ErrorInfo getError() {
        return error;
    }
    
    public void setError(ErrorInfo error) {
        this.error = error;
        this.success = false;
    }
    
    public String getResultType() {
        return getClass().getSimpleName().replace("Result", "");
    }
    
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", requestId);
        obj.put("success", success);
        obj.put("resultType", getResultType());
        
        if (error != null) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("code", error.getCode());
            errorObj.put("message", error.getMessage());
            obj.put("error", errorObj);
        }
        
        return obj;
    }
    
    public void fromJson(JSONObject obj) throws org.json.JSONException {
        requestId = obj.optString("requestId");
        success = obj.optBoolean("success", true);
        
        if (obj.has("error")) {
            JSONObject errorObj = obj.getJSONObject("error");
            error = new ErrorInfo(
                errorObj.optString("code"),
                errorObj.optString("message")
            );
        }
    }
    
    public static class ErrorInfo {
        private String code;
        private String message;
        
        public ErrorInfo() {
        }
        
        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
