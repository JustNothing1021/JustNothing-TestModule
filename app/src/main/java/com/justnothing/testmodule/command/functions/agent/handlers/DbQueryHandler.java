package com.justnothing.testmodule.command.functions.agent.handlers;

import android.content.Context;

import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.agent.response.DbQueryResult;

import org.json.JSONObject;

import java.io.File;

public class DbQueryHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "db_query";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        DbQueryResult result = new DbQueryResult();

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
