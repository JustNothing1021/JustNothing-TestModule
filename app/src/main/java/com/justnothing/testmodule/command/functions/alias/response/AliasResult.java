package com.justnothing.testmodule.command.functions.alias.response;

import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AliasResult extends CommandResult {

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (aliases != null) {
            JSONArray arr = new JSONArray();
            for (AliasInfo alias : aliases) {
                arr.put(alias.toJson());
            }
            obj.put("aliases", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        if (obj.has("aliases")) {
            JSONArray arr = obj.getJSONArray("aliases");
            aliases = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                aliases.add(AliasInfo.fromJson(arr.getJSONObject(i)));
            }
        }
    }
}
