package com.justnothing.testmodule.command.framework.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;


public class CommandResult {

    private static final Logger logger = Logger.getLoggerForName("CommandResult");

    @Expose @SerializedName("requestId")
    private String requestId;

    @Expose @SerializedName("success")
    private boolean success;

    @Expose @SerializedName("message")
    private String message;

    @Expose
    @SerializedName("error")
    private ErrorInfo error;

    @Expose @SerializedName("data")
    private Object data;

    public CommandResult() { this.success = true; }
    public CommandResult(String requestId) { this.requestId = requestId; this.success = true; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; this.success = false; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    /**
     * 从 Gson 反序列化的结果复制字段到 this
     */
    private void copyFieldsFrom(CommandResult source) {
        try {
            CommandFieldCopier.copy(source, this);
        } catch (ReflectiveOperationException e) {
            logger.error("Failed to copy fields from Gson result", e);
        }
    }

    public static class ErrorInfo {

        @Expose @SerializedName("code")
        private String code;

        @Expose @SerializedName("message")
        private String message;

        @Expose(serialize = false, deserialize = false)
        private String stacktrace;

        public ErrorInfo() {}

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
            setStacktrace(t);
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getStacktrace() { return stacktrace; }
        public void setStacktrace(String stacktrace) { this.stacktrace = stacktrace; }

        public void setStacktrace(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            this.stacktrace = sw.toString();
        }
    }

    public String toJsonString() {
        return GsonFactory.getInstance().toJson(this);
    }


    @SuppressWarnings("unchecked")
    public <T extends CommandResult> T fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            logger.warn("Cannot deserialize from null/empty string");
            return (T) this;
        }

        try {
            T result = (T) GsonFactory.getInstance().fromJson(jsonStr, this.getClass());

            if (result != null) {
                copyFieldsFrom(result);
            } else {
                logger.warn("Gson returned null for " + this.getClass().getSimpleName());
            }
            return (T) this;
        } catch (Exception e) {
            logger.error("Failed to deserialize from string", e);
            return (T) this;
        }
    }
}
