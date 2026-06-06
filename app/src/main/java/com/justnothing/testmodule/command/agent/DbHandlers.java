package com.justnothing.testmodule.command.agent;

import android.content.Context;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.constants.AgentResultTypes;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

class DbListHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_list";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        DbListResult result = new DbListResult();
        result.setResultType(AgentResultTypes.DB_LIST);

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

class DbQueryHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_query";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        DbQueryResult result = new DbQueryResult();
        result.setResultType(AgentResultTypes.DB_QUERY);

        String dbName = params.getString("dbName");
        String sql = params.getString("sql");
        int limit = params.optInt("limit", 1000);

        result.setDbName(dbName);

        File dbFile = new File(context.getApplicationInfo().dataDir, "databases/" + dbName);
        if (!dbFile.exists()) {
            throw new IllegalArgumentException("数据库不存在: " + dbFile.getPath());
        }

        try (android.database.sqlite.SQLiteDatabase db =
                     android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.getPath(), null,
                             android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
            android.database.Cursor cursor = db.rawQuery(sql, null);

            String[] columns = cursor.getColumnNames();
            for (String col : columns) {
                result.getColumns().add(col);
            }

            int rowCount = 0;
            while (cursor.moveToNext() && rowCount < limit) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                for (int i = 0; i < columns.length; i++) {
                    row.put(columns[i], getCursorValue(cursor, i));
                }
                result.addRow(row);
                rowCount++;
            }

            cursor.close();
            result.setSuccess(true);
            return result;
        }
    }

    private Object getCursorValue(android.database.Cursor c, int idx) {
        switch (c.getType(idx)) {
            case android.database.Cursor.FIELD_TYPE_NULL:
                return null;
            case android.database.Cursor.FIELD_TYPE_INTEGER:
                return c.getLong(idx);
            case android.database.Cursor.FIELD_TYPE_FLOAT:
                return c.getDouble(idx);
            case android.database.Cursor.FIELD_TYPE_BLOB:
                return c.getBlob(idx);
            default:
                return c.getString(idx);
        }
    }
}

class DbTablesHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_tables";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        DbTablesResult result = new DbTablesResult();
        result.setResultType(AgentResultTypes.DB_TABLES);

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
