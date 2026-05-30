package com.justnothing.testmodule.command.functions.alias.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;

import java.util.List;

@SerializeKeyName("Alias")
public class AliasResult extends CommandResult {

    @Expose @SerializedName("aliases")
    private List<AliasInfo> aliases;

    public AliasResult() {
        super();
    }

    public AliasResult(String requestId) {
        super(requestId);
    }

    public List<AliasInfo> getAliases() {
        return aliases;
    }

    public void setAliases(List<AliasInfo> aliases) {
        this.aliases = aliases;
    }
}
