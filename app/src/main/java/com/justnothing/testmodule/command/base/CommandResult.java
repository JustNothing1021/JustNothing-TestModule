package com.justnothing.testmodule.command.base;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CommandResult {

    private String requestId;
    private boolean success;
    private String message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", requestId);
        obj.put("success", success);
        obj.put("resultType", getResultType());

        if (message != null) {
            obj.put("message", message);
        }

        if (error != null) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("code", error.getCode());
            errorObj.put("message", error.getMessage());
            obj.put("error", errorObj);
        }

        return obj;
    }

    public void fromJson(JSONObject obj) throws JSONException {
        requestId = obj.optString("requestId", "unknown");
        success = obj.optBoolean("success", true);
        message = obj.optString("message", null);

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
        private String stacktrace;


        public ErrorInfo() {
        }

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(String code, String message, String stacktrace) {
            this.code = code;
            this.message = message;
            this.stacktrace = stacktrace;
        }

        public ErrorInfo(String code, String message, Throwable t) {
            this.code = code;
            this.message = message;
            this.setStacktrace(t);
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

        public String getStacktrace() {
            return stacktrace;
        }

        public void setStacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
        }

        public void setStacktrace(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            stacktrace = sw.toString();
        }
    }

}
