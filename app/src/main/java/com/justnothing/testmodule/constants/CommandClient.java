package com.justnothing.testmodule.constants;

import com.justnothing.testmodule.utils.data.DataDirectoryManager;

public class CommandClient {
    public static final String PERFORMANCE_DATA_FILE =
            DataDirectoryManager.getMethodsCmdlineDataDirectory()
                    + "/" + FileDirectory.PERFORMANCE_DATA_FILE_NAME_CLIENT;
    public static final String UPDATE_PORT_TEMP_DIR =
            DataDirectoryManager.getMethodsCmdlineDataDirectory()
                +  "/" + FileDirectory.PORT_UPDATE_SESSIONS_DIR_NAME;
    public static final String CLIENT_VER = "v0.3.0";
}
