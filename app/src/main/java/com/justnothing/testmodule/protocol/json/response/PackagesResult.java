package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PackagesResult extends CommandResult {

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (packages != null) {
            JSONArray arr = new JSONArray();
            for (String pkg : packages) {
                arr.put(pkg);
            }
            obj.put("packages", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        if (obj.has("packages")) {
            JSONArray arr = obj.getJSONArray("packages");
            packages = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                packages.add(arr.getString(i));
            }
        }
    }
}
