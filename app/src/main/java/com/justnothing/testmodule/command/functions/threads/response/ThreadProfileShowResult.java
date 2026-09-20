package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ThreadProfileShowResult extends ThreadCommandResult {

    @Expose @SerializedName("isProfiling")
    private boolean isProfiling;
    @Expose @SerializedName("profileData")
    private String profileData;

    public ThreadProfileShowResult() {
        super();
    }

    public boolean isProfiling() { return isProfiling; }
    public void setProfiling(boolean profiling) { isProfiling = profiling; }

    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }
}
