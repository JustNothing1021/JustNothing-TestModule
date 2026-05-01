package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

public class SpListHandler extends AgentCommandHandler<JSONArray> {

    @Override
    public String getCommandType() {
        return "sp_list";
    }

    @Override
    public JSONArray handle(JSONObject params, Context context) throws Exception {
        File spDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");

        if (!spDir.exists() || !spDir.isDirectory()) {
            return new JSONArray();
        }

        String[] files = spDir.list((dir, name) -> name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            return new JSONArray();
        }

        Arrays.sort(files);

        JSONArray result = new JSONArray();
        for (String file : files) {
            String spName = file.substring(0, file.length() - 4);
            File spFile = new File(spDir, file);
            long lastModified = spFile.lastModified();

            JSONObject info = new JSONObject();
            info.put("name", spName);
            info.put("file", file);
            info.put("sizeBytes", spFile.length());
            info.put("lastModified", lastModified);

            try {
                SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
                info.put("keyCount", sp.getAll().size());
            } catch (Exception ignored) {
                info.put("keyCount", -1);
            }

            result.put(info);
        }
        return result;
    }
}
