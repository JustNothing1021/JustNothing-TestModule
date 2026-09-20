package com.justnothing.testmodule.command.functions.agent.handlers;

import android.content.Context;

import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.response.DbListResult;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

public class DbListHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_list";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) {
        DbListResult result = new DbListResult();

        File dbDir = new File(context.getApplicationInfo().dataDir, "databases");

        if (!dbDir.exists() || !dbDir.isDirectory()) {
            result.setSuccess(true);
            return result;
        }

        String[] files = dbDir.list();
        if (files == null || files.length == 0) {
            result.setSuccess(true);
            return result;
        }

        Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);

        for (String file : files) {
            File dbFile = new File(dbDir, file);

            DbListResult.DbFileInfo info = new DbListResult.DbFileInfo();
            info.setName(file);
            info.setSizeBytes(dbFile.length());
            info.setLastModified(dbFile.lastModified());

            boolean isDb = file.endsWith(".db") || file.endsWith(".sqlite") ||
                    file.endsWith(".sqlite3") || file.endsWith(".wal");
            info.setDatabase(isDb);

            result.addDbFile(info);
        }

        result.setSuccess(true);
        return result;
    }
}

