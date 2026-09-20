package com.justnothing.testmodule.command.functions.didyouknow.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.didyouknow.DidYouKnowTexts;
import com.justnothing.testmodule.command.functions.didyouknow.response.DidYouKnowResult;

public class DidYouKnowRequest extends CommandRequest<DidYouKnowResult> {

    @CmdParam(
        name = "--id",
        aliases = {"-i"},
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_ID_DESC,
        serializedName = "tipIndex"
    )
    private Integer tipIndex;

    @CmdParam(
        name = "--list",
        aliases = {"-l"},
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_LIST_DESC,
        serializedName = "showList"
    )
    private boolean showList = false;

    @CmdParam(
        name = "--count",
        aliases = {"-c"},
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_COUNT_DESC,
        serializedName = "showCount"
    )
    private boolean showCount = false;

    @CmdParam(
        name = "--search",
        aliases = {"-s"},
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_SEARCH_DESC,
        serializedName = "searchKeyword"
    )
    private String searchKeyword;

    @CmdParam(
        name = "--special",
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_SPECIAL_DESC,
        serializedName = "showSpecial"
    )
    private boolean showSpecial = false;

    @CmdParam(
        name = "--special-list",
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_SPECIAL_LIST_DESC,
        serializedName = "showSpecialList"
    )
    private boolean showSpecialList = false;

    @CmdParam(
        name = "--help",
        aliases = {"-h"},
        description = DidYouKnowTexts.PARAM_DID_YOU_KNOW_HELP_DESC,
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
