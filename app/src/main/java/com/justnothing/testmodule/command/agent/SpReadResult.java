package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.HashMap;
import java.util.Map;

@SerializeKeyName("SpReadResult")
@AutoSerializable
public class SpReadResult extends CommandResult {

    @Expose @SerializedName("spName")
    private String spName;

    @Expose @SerializedName("entries")
    private Map<String, Object> entries;

    public SpReadResult() {
        super();
        this.entries = new HashMap<>();
    }

    public String getSpName() { return spName; }
    public void setSpName(String spName) { this.spName = spName; }

    public Map<String, Object> getEntries() { return entries; }
    public void setEntries(Map<String, Object> entries) { this.entries = entries; }
    public void putEntry(String key, Object value) { this.entries.put(key, value); }
}
