package com.justnothing.testmodule.command.functions.hook;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class HookAddResult extends CommandResult {

    @Expose @SerializedName("hookId")
    private String hookId;
    @Expose @SerializedName("successAction")
    private boolean successAction;
    @Expose @SerializedName("message")
    private String message;
    @Expose @SerializedName("detail")
    private List<HookDetailInfo> detail;

    public HookAddResult() {
        super();
        this.detail = new ArrayList<>();
    }

    public HookAddResult(String requestId) {
        super(requestId);
        this.detail = new ArrayList<>();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    public boolean isSuccessAction() { return successAction; }
    public void setSuccessAction(boolean successAction) { this.successAction = successAction; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<HookDetailInfo> getDetail() { return detail; }
    public void setDetail(List<HookDetailInfo> detail) { this.detail = detail; }

    public static class HookDetailInfo {
        @Expose @SerializedName("key")
        private String key;
        @Expose @SerializedName("value")
        private String value;

        public HookDetailInfo() {}

        public HookDetailInfo(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
