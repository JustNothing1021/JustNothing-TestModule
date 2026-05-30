package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:search")
public class SearchClassRequest extends ClassCommandRequest {

    @CmdParam(
        name = "--type",
        description = "搜索类型",
        required = true,
        position = 1,
        serializedName = "searchType"
    )
    private String searchType;

    @CmdParam(
        name = "--pattern",
        description = "搜索模式",
        required = true,
        position = 2,
        serializedName = "pattern"
    )
    private String pattern;

    public SearchClassRequest() {
        super();
    }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    @Override
    public SearchClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(SearchClassRequest.class, args);
    }
}
