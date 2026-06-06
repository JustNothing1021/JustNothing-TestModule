package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.content.SharedPreferences;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.constants.AgentResultTypes;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

public class SpListHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "sp_list";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        SpListResult result = new SpListResult();
        result.setResultType(AgentResultTypes.SP_LIST);

        File spDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");

        if (!spDir.exists() || !spDir.isDirectory()) {
            result.setSuccess(true);
            return result;
        }

        String[] files = spDir.list((dir, name) -> name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            result.setSuccess(true);
            return result;
        }

        Arrays.sort(files);

        for (String file : files) {
            String spName = file.substring(0, file.length() - 4);
            File spFile = new File(spDir, file);

            SpListResult.SpFileInfo info = new SpListResult.SpFileInfo();
            info.setName(spName);
            info.setFile(file);
            info.setSizeBytes(spFile.length());
            info.setLastModified(spFile.lastModified());

            try {
                SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
                info.setKeyCount(sp.getAll().size());
            } catch (Exception ignored) {
                info.setKeyCount(-1);
            }

            result.addSpFile(info);
        }

        result.setSuccess(true);
        return result;
    }
}
