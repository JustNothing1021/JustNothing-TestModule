package com.justnothing.testmodule.command.base.protocol;

import com.justnothing.testmodule.command.utils.AutoSerializer;

public abstract class AutoSerializableBase {
    public String autoToJson() {
        return AutoSerializer.toJson(this);
    }
    @SuppressWarnings("unchecked")
    public <T extends AutoSerializableBase> T autoFromJson(String json) {
        return (T) AutoSerializer.fromJson(json, this.getClass());
    }
}
