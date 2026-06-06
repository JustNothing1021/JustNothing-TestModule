package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("SpListResult")
public class SpListResult extends CommandResult {

    @Expose @SerializedName("spFiles")
    private List<SpFileInfo> spFiles;

    public SpListResult() {
        super();
        this.spFiles = new ArrayList<>();
    }

    public List<SpFileInfo> getSpFiles() { return spFiles; }
    public void setSpFiles(List<SpFileInfo> spFiles) { this.spFiles = spFiles; }
    public void addSpFile(SpFileInfo info) { this.spFiles.add(info); }

    public static class SpFileInfo {
        @Expose @SerializedName("name")
        private String name;
        @Expose @SerializedName("file")
        private String file;
        @Expose @SerializedName("sizeBytes")
        private long sizeBytes;
        @Expose @SerializedName("lastModified")
        private long lastModified;
        @Expose @SerializedName("keyCount")
        private int keyCount;

        public SpFileInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public int getKeyCount() { return keyCount; }
        public void setKeyCount(int keyCount) { this.keyCount = keyCount; }
    }
}
