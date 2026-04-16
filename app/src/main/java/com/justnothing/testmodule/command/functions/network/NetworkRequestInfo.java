package com.justnothing.testmodule.command.functions.network;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NetworkRequestInfo {

    private final int id;
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private String requestBody;
    private final long requestTime;
    private final String clientType;

    private int responseCode;
    private String responseMessage;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private long responseTime;
    private long duration;
    private Throwable error;
    private boolean completed;

    public NetworkRequestInfo(int id, String url, String method, String clientType) {
        this.id = id;
        this.url = url;
        this.method = method;
        this.clientType = clientType;
        this.headers = new HashMap<>();
        this.requestTime = System.currentTimeMillis();
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public String getClientType() {
        return clientType;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void addResponseHeader(String name, String value) {
        if (this.responseHeaders == null) {
            this.responseHeaders = new HashMap<>();
        }
        this.responseHeaders.put(name, value);
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
        this.duration = responseTime - requestTime;
    }

    public long getDuration() {
        return duration;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getHost() {
        if (url == null) return "";
        try {
            int start = url.indexOf("://");
            if (start == -1) return url;
            start += 3;
            int end = url.indexOf("/", start);
            if (end == -1) {
                int query = url.indexOf("?", start);
                return query == -1 ? url.substring(start) : url.substring(start, query);
            }
            return url.substring(start, end);
        } catch (Exception e) {
            return url;
        }
    }

    public String getPath() {
        if (url == null) return "";
        try {
            int start = url.indexOf("://");
            if (start == -1) return url;
            start += 3;
            int pathStart = url.indexOf("/", start);
            if (pathStart == -1) return "/";
            return url.substring(pathStart);
        } catch (Exception e) {
            return url;
        }
    }

    public String getSummary() {
        return String.format(Locale.getDefault(), "[%d] %s %s (%dms) %s",
                id, method, getHost(), duration,
                completed ? (error != null ? "ERROR" : responseCode + "") : "PENDING");
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Network Request #").append(id).append(" ===\n");
        sb.append("URL: ").append(url).append("\n");
        sb.append("Method: ").append(method).append("\n");
        sb.append("Client: ").append(clientType).append("\n");
        sb.append("Status: ").append(completed ? (error != null ? "ERROR" : responseCode + " " + responseMessage) : "PENDING").append("\n");
        sb.append("Duration: ").append(duration).append("ms\n");

        if (!headers.isEmpty()) {
            sb.append("\nRequest Headers:\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        if (requestBody != null && !requestBody.isEmpty()) {
            sb.append("\nRequest Body:\n");
            sb.append("  ").append(truncate(requestBody, 500)).append("\n");
        }

        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            sb.append("\nResponse Headers:\n");
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append("\nResponse Body:\n");
            sb.append("  ").append(truncate(responseBody, 500)).append("\n");
        }

        if (error != null) {
            sb.append("\nError: ").append(error.getMessage()).append("\n");
        }

        return sb.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "... (truncated)";
    }
}
