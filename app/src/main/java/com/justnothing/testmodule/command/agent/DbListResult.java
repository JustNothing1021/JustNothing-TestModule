package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("DbListResult")
public class DbListResult extends CommandResult {

    @Expose @SerializedName("dbFiles")
    private List<DbFileInfo> dbFiles;

    public DbListResult() {
        super();
        this.dbFiles = new ArrayList<>();
    }

    public List<DbFileInfo> getDbFiles() { return dbFiles; }
    public void setDbFiles(List<DbFileInfo> dbFiles) { this.dbFiles = dbFiles; }
    public void addDbFile(DbFileInfo info) { this.dbFiles.add(info); }

    public static class DbFileInfo {
        @Expose @SerializedName("name")
        private String name;
        @Expose @SerializedName("sizeBytes")
        private long sizeBytes;
        @Expose @SerializedName("lastModified")
        private long lastModified;
        @Expose @SerializedName("isDatabase")
        private boolean isDatabase;

        public DbFileInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public boolean isDatabase() { return isDatabase; }
        public void setDatabase(boolean database) { isDatabase = database; }
    }
}
