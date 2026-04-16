package com.justnothing.testmodule.constants;

import android.annotation.SuppressLint;


public class FileDirectory {
    public static final String APPLICATION_VERSION = "0.4.6";
    @SuppressLint("SdCardPath") // hook端如果用Environment会出现未定义行为
    public static final String SDCARD = "/sdcard";
    public static final String EXTERNAL_PATH = "/data/user_de/0/com.justnothing.testmodule/JustNothing";
    public static final String SDCARD_PATH = SDCARD + "/JustNothing";
    public static final String METHODS_DATA_DIR = "/data/local/tmp/methods";
    public static final String PORT_FILE = METHODS_DATA_DIR + "/methods_port";
    public static String DATA_PATH = EXTERNAL_PATH;
    public static final String EXPORT_DIR_NAME = "TestModuleExports";
    public static final String SCRIPTS_DIR_NAME = "scripts";
    public static final String SCRIPTS_FILE_NAME = "scripts.json";
    public static final String CONTENTS_DIR_NAME = "contents";
    public static final String CONTENT_FILE_PREFIX = "content_";
    public static final String MODULE_STATUS_FILE_NAME = "module_status.json";
    public static final String PERFORMANCE_DATA_FILE_NAME = "performance_data.json";
    public static final String MODULE_LOG_FILE_NAME = "module_log.txt";
    public static final String CLIENT_HOOK_CONFIG_FILE_NAME = "client_hook_config.json";
    public static final String SERVER_HOOK_LIST_CONFIG_NAME = "server_hook_config.json";
    public static final String EXECUTE_SESSIONS_DIR_NAME = "execute_sessions";
    public static final String PORT_UPDATE_SESSIONS_DIR_NAME = "port_update_sessions";
    public static final String PERFORMANCE_DATA_FILE_NAME_CLIENT = "java_client_perf_stats.prop";
    public static final String SESSION_PREFIX = "session_";
    public static final String INPUT_FILE_NAME = "input.txt";
    public static final String OUTPUT_FILE_NAME = "output.txt";
    public static final String RESULT_FILE_NAME = "result.txt";

    public static final String TEMP_FALLBACK_RECORD_FILE_DIR = SDCARD_PATH + "/fallback_dir";
    public static final String SCRIPT_SUFFIX = ".java";
}
