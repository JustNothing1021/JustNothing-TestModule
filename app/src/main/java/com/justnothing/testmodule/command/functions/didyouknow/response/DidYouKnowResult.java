package com.justnothing.testmodule.command.functions.didyouknow.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("DidYouKnow")
public class DidYouKnowResult extends CommandResult {

    @Expose @SerializedName("tipContent")
    private String tipContent;

    @Expose @SerializedName("tipAuthor")
    private String tipAuthor;

    @Expose @SerializedName("isSpecial")
    private boolean isSpecial;

    @Expose @SerializedName("isCliExclusive")
    private boolean isCliExclusive;

    @Expose @SerializedName("tipIndex")
    private Integer tipIndex;

    @Expose @SerializedName("totalCount")
    private Integer totalCount;

    @Expose @SerializedName("appTipCount")
    private Integer appTipCount;

    @Expose @SerializedName("cliTipCount")
    private Integer cliTipCount;

    @Expose @SerializedName("searchResults")
    private int searchResultCount;

    @Expose @SerializedName("hasSpecialToday")
    private boolean hasSpecialToday;

    public DidYouKnowResult() {
        super();
    }

    // ---- getters & setters ----

    public String getTipContent() { return tipContent; }
    public void setTipContent(String tipContent) { this.tipContent = tipContent; }

    public String getTipAuthor() { return tipAuthor; }
    public void setTipAuthor(String tipAuthor) { this.tipAuthor = tipAuthor; }

    public boolean isSpecial() { return isSpecial; }
    public void setSpecial(boolean special) { isSpecial = special; }

    public boolean isCliExclusive() { return isCliExclusive; }
    public void setCliExclusive(boolean cliExclusive) { isCliExclusive = cliExclusive; }

    public Integer getTipIndex() { return tipIndex; }
    public void setTipIndex(Integer tipIndex) { this.tipIndex = tipIndex; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getAppTipCount() { return appTipCount; }
    public void setAppTipCount(Integer appTipCount) { this.appTipCount = appTipCount; }

    public Integer getCliTipCount() { return cliTipCount; }
    public void setCliTipCount(Integer cliTipCount) { this.cliTipCount = cliTipCount; }

    public int getSearchResultCount() { return searchResultCount; }
    public void setSearchResultCount(int searchResultCount) { this.searchResultCount = searchResultCount; }

    public boolean isHasSpecialToday() { return hasSpecialToday; }
    public void setHasSpecialToday(boolean hasSpecialToday) { this.hasSpecialToday = hasSpecialToday; }
}
