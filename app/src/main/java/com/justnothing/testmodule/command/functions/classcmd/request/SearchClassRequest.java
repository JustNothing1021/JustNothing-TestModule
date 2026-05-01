package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchClassRequest extends ClassCommandRequest {

    private String searchType;
    private String pattern;

    public SearchClassRequest() {
        super();
    }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("searchType", searchType);
        obj.put("pattern", pattern);
        return obj;
    }

    @Override
    public SearchClassRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setSearchType(obj.optString("searchType"));
        setPattern(obj.optString("pattern"));
        return this;
    }

    @Override
    public SearchClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 2) {
            throw new IllegalCommandLineArgumentException("参数不足: class search <subcmd> <pattern>");
        }

        searchType = args[0];
        pattern = args[1];
        return this;
    }
}
