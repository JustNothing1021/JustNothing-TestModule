package com.justnothing.testmodule.command.functions.network;

import com.justnothing.testmodule.command.base.CommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkRequest extends CommandRequest {

    private String subCommand;
    private String paramValue;
    private String requestId_filter;
    private String mockPattern;
    private String mockResponse;
    private Integer mockStatus;
    private String methodFilter;
    private String statusFilter;
    private String hostFilter;
    private String exportFile;

    public NetworkRequest() { super(); }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }

    public String getRequestId_filter() { return requestId_filter; }
    public void setRequestId_filter(String requestId_filter) { this.requestId_filter = requestId_filter; }

    public String getMockPattern() { return mockPattern; }
    public void setMockPattern(String mockPattern) { this.mockPattern = mockPattern; }

    public String getMockResponse() { return mockResponse; }
    public void setMockResponse(String mockResponse) { this.mockResponse = mockResponse; }

    public Integer getMockStatus() { return mockStatus; }
    public void setMockStatus(Integer mockStatus) { this.mockStatus = mockStatus; }

    public String getMethodFilter() { return methodFilter; }
    public void setMethodFilter(String methodFilter) { this.methodFilter = methodFilter; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public String getHostFilter() { return hostFilter; }
    public void setHostFilter(String hostFilter) { this.hostFilter = hostFilter; }

    public String getExportFile() { return exportFile; }
    public void setExportFile(String exportFile) { this.exportFile = exportFile; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (subCommand != null) obj.put("subCommand", subCommand);
        if (paramValue != null) obj.put("paramValue", paramValue);
        if (requestId_filter != null) obj.put("requestId", requestId_filter);
        if (mockPattern != null) obj.put("mockPattern", mockPattern);
        if (mockResponse != null) obj.put("mockResponse", mockResponse);
        if (mockStatus != null) obj.put("mockStatus", mockStatus);
        if (methodFilter != null) obj.put("methodFilter", methodFilter);
        if (statusFilter != null) obj.put("statusFilter", statusFilter);
        if (hostFilter != null) obj.put("hostFilter", hostFilter);
        if (exportFile != null) obj.put("exportFile", exportFile);
        return obj;
    }

    @Override
    public NetworkRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSubCommand(obj.optString("subCommand", "status"));
        setParamValue(obj.optString("paramValue", null));
        setRequestId_filter(obj.optString("requestId", null));
        setMockPattern(obj.optString("mockPattern", null));
        setMockResponse(obj.optString("mockResponse", null));
        setMockStatus(obj.has("mockStatus") ? obj.getInt("mockStatus") : null);
        setMethodFilter(obj.optString("methodFilter", null));
        setStatusFilter(obj.optString("statusFilter", null));
        setHostFilter(obj.optString("hostFilter", null));
        setExportFile(obj.optString("exportFile", null));
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        if (args.length > 0) subCommand = args[0];
        if (args.length > 1) paramValue = args[1];
        if (args.length > 2) {
            switch (subCommand) {
                case "info" -> requestId_filter = args[2];
                case "filter" -> hostFilter = args[2];
                case "export" -> exportFile = args[2];
                default -> {}
            }
        }
        for (int i = 1; i < args.length; i++) {
            if ("--method".equals(args[i]) && i + 1 < args.length) methodFilter = args[++i];
            else if ("--status".equals(args[i]) && i + 1 < args.length) statusFilter = args[++i];
        }
        return this;
    }
}
