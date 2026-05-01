package com.justnothing.testmodule.command.agent;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InspectionClient {

    private static final Logger logger = Logger.getLoggerForName("InspectionClient");

    private static final String AGENT_WORK_DIR = "/data/local/tmp/methods/agent";

    public static AgentInfo ping(String packageName) throws AgentNotFoundException, AgentDeadException {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        JSONObject request;
        try {
            request = new JSONObject();
            request.put("command", "PING");
        } catch (JSONException e) {
            throw new AgentDeadException(packageName);
        }

        try {
            JSONObject response = sendRequest(packageName, request);
            int returnCode = response.optInt("returnCode", -1);
            if (returnCode != 0) {
                throw new AgentDeadException(packageName);
            }
            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                throw new AgentDeadException(packageName);
            }
            return new AgentInfo(
                    data.optString("packageName", packageName),
                    data.optLong("startTime", 0),
                    data.optString("version", "unknown"));
        } catch (AgentNotFoundException | AgentDeadException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentDeadException(packageName);
        }
    }

    public static boolean isAlive(String packageName) {
        try {
            ping(packageName);
            return true;
        } catch (AgentNotFoundException | AgentDeadException e) {
            return false;
        }
    }

    public static JSONObject execute(String packageName, String command,
                                     JSONObject params) throws Exception {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        JSONObject request = new JSONObject();
        request.put("command", command);
        if (params != null) request.put("params", params);

        JSONObject response = sendRequest(packageName, request);

        int returnCode = response.optInt("returnCode", 0);
        if (returnCode < 0) {
            JSONObject error = response.optJSONObject("error");
            String code = error != null ? error.optString("code", "UNKNOWN") : "ERROR_" + returnCode;
            String msg = error != null ? error.optString("message", "未知错误") : "Agent 命令执行失败";
            throw new AgentCommandFailedException(code, msg);
        }

        return response;
    }

    public static Map<String, Object> executeAndGetData(String packageName, String command,
                                                        JSONObject params) throws Exception {
        JSONObject resp = execute(packageName, command, params);
        Object data = resp.opt("data");
        if (data instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) data);
        }
        throw new AgentCommandFailedException("INVALID_RESPONSE",
                "期望 data 为 JSON Object, 实际: " + (data != null ? data.getClass().getSimpleName() : "null"));
    }

    public static List<Map<String, Object>> executeAndGetList(String packageName, String command,
                                                               JSONObject params) throws Exception {
        JSONObject resp = execute(packageName, command, params);
        Object data = resp.opt("data");
        if (data instanceof JSONArray arr) {
            List<Map<String, Object>> result = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item instanceof JSONObject) result.add(jsonObjectToMap((JSONObject) item));
            }
            return result;
        }
        throw new AgentCommandFailedException("INVALID_RESPONSE",
                "期望 data 为 JSON Array, 实际: " + (data != null ? data.getClass().getSimpleName() : "null"));
    }

    public static Set<String> getAvailableCommands(String packageName) throws Exception {
        JSONObject resp = execute(packageName, "_list_commands", null);
        Object data = resp.opt("data");
        if (data instanceof JSONArray arr) {
            Set<String> cmds = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                String cmd = arr.optString(i, null);
                if (cmd != null) cmds.add(cmd);
            }
            return cmds;
        }
        return Collections.emptySet();
    }

    private static boolean isAgentInfoExists(String packageName) {
        File infoFile = new File(AGENT_WORK_DIR,
                InspectionAgent.DESCRIPTOR_PREFIX + packageName + ".info");
        return infoFile.exists();
    }

    private static String getSocketName(String packageName) {
        return InspectionAgent.DESCRIPTOR_PREFIX + packageName;
    }

    private static JSONObject sendRequest(String packageName, JSONObject request)
            throws Exception {
        try (LocalSocket socket = new LocalSocket()) {

            socket.connect(new LocalSocketAddress(
                    "\0" + getSocketName(packageName),
                    LocalSocketAddress.Namespace.ABSTRACT));

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));

            writer.write(request.toString());
            writer.newLine();
            writer.flush();

            String responseJson = reader.readLine();
            if (responseJson == null || responseJson.isEmpty()) {
                throw new AgentDeadException(packageName);
            }

            return new JSONObject(responseJson);

        }
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object val = json.opt(key);
            if (val == null || val == JSONObject.NULL) {
                map.put(key, null);
            } else if (val instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) val));
            } else if (val instanceof org.json.JSONArray) {
                map.put(key, jsonArrayToList((org.json.JSONArray) val));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    private static List<Object> jsonArrayToList(org.json.JSONArray arr) throws JSONException {
        List<Object> list = new java.util.ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.get(i);
            if (item == null || item == JSONObject.NULL) {
                list.add(null);
            } else if (item instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) item));
            } else if (item instanceof org.json.JSONArray) {
                list.add(jsonArrayToList((org.json.JSONArray) item));
            } else {
                list.add(item);
            }
        }
        return list;
    }

    public record AgentInfo(String packageName, long startTime, String version) {
    }

    public static class AgentNotFoundException extends Exception {
        public AgentNotFoundException(String pkg) {
            super("目标应用 " + pkg + " 未注册 InspectionAgent。" +
                    "可能原因: 1) 应用未运行 2) 应用未被 Xposed 注入 Agent" +
                    " 3) 请确认应用已重启（Xposed Hook 需要应用重新启动才生效）");
        }
    }

    public static class AgentDeadException extends Exception {
        public AgentDeadException(String pkg) {
            super("目标应用 " + pkg + " 的 InspectionAgent 无响应（应用可能已崩溃）");
        }
    }

    public static class AgentCommandFailedException extends Exception {
        private final String errorCode;

        public AgentCommandFailedException(String code, String msg) {
            super(msg);
            this.errorCode = code;
        }

        public String getErrorCode() { return errorCode; }
    }
}
