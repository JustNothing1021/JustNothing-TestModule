package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

class DbListHandler extends AgentCommandHandler<JSONArray> {

    @Override
    public String getCommandType() {
        return "db_list";
    }

    @Override
    public JSONArray handle(JSONObject params, Context context) throws Exception {
        File dbDir = new File(context.getApplicationInfo().dataDir, "databases");

        if (!dbDir.exists() || !dbDir.isDirectory()) {
            return new JSONArray();
        }

        String[] files = dbDir.list();
        if (files == null || files.length == 0) {
            return new JSONArray();
        }

        Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);

        JSONArray result = new JSONArray();
        for (String file : files) {
            File dbFile = new File(dbDir, file);
            JSONObject info = new JSONObject();
            info.put("name", file);
            info.put("sizeBytes", dbFile.length());
            info.put("lastModified", dbFile.lastModified());

            boolean isDb = file.endsWith(".db") || file.endsWith(".sqlite") ||
                    file.endsWith(".sqlite3") || file.endsWith(".wal");
            info.put("isDatabase", isDb);

            result.put(info);
        }
        return result;
    }
}

class DbQueryHandler extends AgentCommandHandler<JSONObject> {

    @Override
    public String getCommandType() {
        return "db_query";
    }

    @Override
    public JSONObject handle(JSONObject params, Context context) throws Exception {
        String dbName = params.getString("dbName");
        String sql = params.getString("sql");
        int limit = params.optInt("limit", 1000);

        File dbFile = new File(context.getApplicationInfo().dataDir, "databases/" + dbName);
        if (!dbFile.exists()) {
            throw new IllegalArgumentException("数据库不存在: " + dbFile.getPath());
        }

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery(sql, null);

            String[] columns = cursor.getColumnNames();

            JSONArray rows = new JSONArray();
            int rowCount = 0;

            while (cursor.moveToNext() && rowCount < limit) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < columns.length; i++) {
                    putCursorValue(row, columns[i], cursor, i);
                }
                rows.put(row);
                rowCount++;
            }

            cursor.close();

            JSONObject result = new JSONObject();
            result.put("columns", new JSONArray(Arrays.asList(columns)));
            result.put("rows", rows);
            result.put("rowCount", rowCount);
            return result;

        } finally {
            if (db != null) db.close();
        }
    }

    private void putCursorValue(JSONObject row, String col, Cursor c, int idx)
            throws org.json.JSONException {
        switch (c.getType(idx)) {
            case Cursor.FIELD_TYPE_NULL:
                row.put(col, JSONObject.NULL);
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                row.put(col, c.getLong(idx));
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                row.put(col, c.getDouble(idx));
                break;
            case Cursor.FIELD_TYPE_BLOB:
                byte[] blob = c.getBlob(idx);
                row.put(col, new JSONArray(Arrays.asList(blob)));
                break;
            default:
                row.put(col, c.getString(idx));
                break;
        }
    }
}

class DbTablesHandler extends AgentCommandHandler<JSONArray> {

    @Override
    public String getCommandType() {
        return "db_tables";
    }

    @Override
    public JSONArray handle(JSONObject params, Context context) throws Exception {
        String dbName = params.getString("dbName");

        File dbFile = new File(context.getApplicationInfo().dataDir, "databases/" + dbName);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY);

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null);
            JSONArray tables = new JSONArray();
            while (cursor.moveToNext()) {
                tables.put(cursor.getString(0));
            }
            cursor.close();
            return tables;
        } finally {
            db.close();
        }
    }
}
