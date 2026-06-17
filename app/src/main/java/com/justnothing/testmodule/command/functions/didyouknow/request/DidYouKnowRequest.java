package com.justnothing.testmodule.command.functions.didyouknow.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("did-you-know")
public class DidYouKnowRequest extends CommandRequest {

    @CmdParam(
        name = "--id",
        aliases = {"-i"},
        description = "显示指定序号的提示",
        serializedName = "tipIndex"
    )
    private Integer tipIndex;

    @CmdParam(
        name = "--list",
        aliases = {"-l"},
        description = "列出所有提示（带编号）",
        serializedName = "showList"
    )
    private boolean showList = false;

    @CmdParam(
        name = "--count",
        aliases = {"-c"},
        description = "显示提示统计信息",
        serializedName = "showCount"
    )
    private boolean showCount = false;

    @CmdParam(
        name = "--search",
        aliases = {"-s"},
        description = "搜索包含关键词的提示",
        serializedName = "searchKeyword"
    )
    private String searchKeyword;

    @CmdParam(
        name = "--special",
        description = "显示今日特殊提示（生日/节日等）",
        serializedName = "showSpecial"
    )
    private boolean showSpecial = false;

    @CmdParam(
        name = "--special-list",
        description = "列出所有特殊提示（节日/生日等）",
        serializedName = "showSpecialList"
    )
    private boolean showSpecialList = false;

    @CmdParam(
        name = "--help",
        aliases = {"-h"},
        description = "显示帮助信息",
        serializedName = "showHelp"
    )
    private boolean showHelp = false;

    public DidYouKnowRequest() {
        super();
    }

    public Integer getTipIndex() { return tipIndex; }
    public void setTipIndex(Integer tipIndex) { this.tipIndex = tipIndex; }

    public boolean isShowList() { return showList; }
    public void setShowList(boolean showList) { this.showList = showList; }

    public boolean isShowCount() { return showCount; }
    public void setShowCount(boolean showCount) { this.showCount = showCount; }

    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    public boolean isShowSpecial() { return showSpecial; }
    public void setShowSpecial(boolean showSpecial) { this.showSpecial = showSpecial; }

    public boolean isShowSpecialList() { return showSpecialList; }
    public void setShowSpecialList(boolean showSpecialList) { this.showSpecialList = showSpecialList; }

    public boolean isShowHelp() { return showHelp; }
    public void setShowHelp(boolean showHelp) { this.showHelp = showHelp; }
}
