package com.justnothing.testmodule.command.functions.agent.handlers;

import android.content.Context;

import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.response.DbTablesResult;

import org.json.JSONObject;

import java.io.File;

public class DbTablesHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_tables";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        DbTablesResult result = new DbTablesResult();

        String dbName = params.getString("dbName");
        result.setDbName(dbName);

        File dbFile = new File(context.getApplicationInfo().dataDir, "databases/" + dbName);

        try (android.database.sqlite.SQLiteDatabase db =
                     android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.getPath(), null,
                             android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
            android.database.Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null);
            while (cursor.moveToNext()) {
                result.addTable(cursor.getString(0));
            }
            cursor.close();

            result.setSuccess(true);
            return result;
        }
    }
}
