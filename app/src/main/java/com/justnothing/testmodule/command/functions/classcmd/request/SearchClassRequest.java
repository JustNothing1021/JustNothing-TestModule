package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("SearchClass")
@SubCommand("search")
@AutoSerializable
public class SearchClassRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "搜索类型", required = true, description = "class/method/field/annotation")
    private String searchType;

    @PositionalParam(order = 2, name = "搜索模式", required = true)
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
