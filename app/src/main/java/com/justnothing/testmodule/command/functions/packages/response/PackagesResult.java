package com.justnothing.testmodule.command.functions.packages.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.util.List;

public class PackagesResult extends CommandResult {

    @Expose @SerializedName("packages")
    private List<String> packages;

    public PackagesResult() {
        super();
    }

    public PackagesResult(String requestId) {
        super(requestId);
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }
}
