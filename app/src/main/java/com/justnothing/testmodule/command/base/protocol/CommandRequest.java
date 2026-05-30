package com.justnothing.testmodule.command.base.protocol;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public abstract class CommandRequest {

    private static final Logger logger = Logger.getLoggerForName("CommandRequest");

    @Expose @SerializedName("requestId")
    private String requestId;

    @Expose @SerializedName("commandType")
    private String commandType;

    // 操作符追踪列表：记录实际使用了哪些操作符（如 ["get", "set"]）
    private final List<String> receivedOperators = new ArrayList<>();

    public CommandRequest() {
        this.requestId = UUID.randomUUID().toString();
        this.commandType = extractTypeKey();
    }

    public CommandRequest(String requestId) {
        this.requestId = requestId;
        this.commandType = extractTypeKey();
    }

    private String extractTypeKey() {
        SerializeKeyName keyName = this.getClass().getAnnotation(SerializeKeyName.class);
        if (keyName != null) return keyName.value();
        throw new IllegalStateException("Request类 " + this.getClass().getSimpleName() + " 缺少@SerializeKeyName注解!");
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    /**
     * 添加已接收的操作符（由 CmdParamProcessor.handleOperator() 调用）
     */
    public void addReceivedOperator(String operatorName) {
        if (operatorName != null && !operatorName.isEmpty() && !receivedOperators.contains(operatorName)) {
            receivedOperators.add(operatorName);
        }
    }

    /**
     * 获取所有已使用的操作符（只读视图）
     */
    public List<String> getReceivedOperators() {
        return Collections.unmodifiableList(receivedOperators);
    }

    /**
     * 检查是否使用了指定操作符
     */
    public boolean hasOperator(String operatorName) {
        return receivedOperators.contains(operatorName);
    }

    /**
     * 序列化为 JSONObject（兼容旧接口）
     * <p>
     * 内部实现：Gson → String → 安全转换为 JSONObject
     * <p>
     * 注意：在 Android 测试环境中 org.json 可能有 bug，推荐使用 toJsonString()
     */
    public JSONObject toJson() throws JSONException {
        try {
            String jsonStr = GsonFactory.getInstance().toJson(this);
            
            if (jsonStr.trim().isEmpty()) {
                logger.warn("Gson returned empty JSON for " + this.getClass().getSimpleName());
                return createMinimalJson();
            }
            
            try {
                return new JSONObject(jsonStr);
            } catch (JSONException e) {
                logger.warn("Failed to parse Gson output with org.json, trying manual conversion");
                return createJsonObjectManually(jsonStr);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize " + this.getClass().getSimpleName(), e);
            return createMinimalJson();
        }
    }

    /**
     * 手动将 JSON 字符串解析为 JSONObject（避免 org.json 解析器的 bug）
     */
    private JSONObject createJsonObjectManually(String jsonStr) throws JSONException {
        JSONObject result = new JSONObject();
        
        if (jsonStr == null || !jsonStr.trim().startsWith("{")) {
            return result;
        }
        
        String content = jsonStr.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1).trim();
        }
        
        int depth = 0;
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean inString = false;
        boolean escape = false;
        boolean inKey = true;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (escape) {
                if (inKey) currentKey.append(c);
                else currentValue.append(c);
                escape = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escape = true;
                if (inKey) currentKey.append(c);
                else currentValue.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                if (inKey) currentKey.append(c);
                else currentValue.append(c);
                continue;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    depth++;
                    if (!inKey) currentValue.append(c);
                } else if (c == '}' || c == ']') {
                    depth--;
                    if (!inKey) currentValue.append(c);
                } else if (c == ':' && depth == 0 && inKey) {
                    inKey = false;
                    continue;
                } else if (c == ',' && depth == 0 && !inKey) {
                    String key = extractStringValue(currentKey.toString());
                    String value = currentValue.toString().trim();
                    try {
                        putValue(result, key, value);
                    } catch (Exception ex) {
                        logger.warn("Failed to put value for key: " + key);
                    }
                    
                    currentKey = new StringBuilder();
                    currentValue = new StringBuilder();
                    inKey = true;
                    continue;
                } else {
                    if (!inKey) currentValue.append(c);
                }
            } else {
                if (inKey) currentKey.append(c);
                else currentValue.append(c);
            }
        }
        
        if (!inKey && currentKey.length() > 0) {
            String key = extractStringValue(currentKey.toString());
            String value = currentValue.toString().trim();
            try {
                putValue(result, key, value);
            } catch (Exception ex) {
                logger.warn("Failed to put value for last key: " + key);
            }
        }
        
        return result;
    }

    private String extractStringValue(String quoted) {
        if (quoted != null && quoted.startsWith("\"") && quoted.endsWith("\"") && quoted.length() >= 2) {
            return quoted.substring(1, quoted.length() - 1);
        }
        return quoted;
    }

    private void putValue(JSONObject obj, String key, String valueStr) throws JSONException {
        if (valueStr == null || valueStr.isEmpty()) {
            return;
        }

        switch (valueStr) {
            case "true" -> obj.put(key, true);
            case "false" -> obj.put(key, false);
            case "null" -> obj.put(key, JSONObject.NULL);
            default -> {
                try {
                    if (valueStr.contains(".") || valueStr.toLowerCase().contains("e") ||
                            (valueStr.startsWith("\"") && valueStr.endsWith("\""))) {
                        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                            obj.put(key, valueStr.substring(1, valueStr.length() - 1));
                        } else if (valueStr.startsWith("{") || valueStr.startsWith("[")) {
                            obj.put(key, new JSONObject(valueStr));
                        } else {
                            obj.put(key, Double.parseDouble(valueStr));
                        }
                    } else {
                        long longVal = Long.parseLong(valueStr);
                        if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                            obj.put(key, (int) longVal);
                        } else {
                            obj.put(key, longVal);
                        }
                    }
                } catch (NumberFormatException e) {
                    if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                        obj.put(key, valueStr.substring(1, valueStr.length() - 1));
                    } else {
                        obj.put(key, valueStr);
                    }
                }
            }
        }
    }

    /**
     * 从 JSONObject 反序列化（兼容旧接口）
     * 内部实现：安全提取 JSON 字符串 → Gson 反序列化
     */
    @SuppressWarnings("unchecked")
    public <T extends CommandRequest> T fromJson(JSONObject obj) throws JSONException {
        if (obj == null) {
            throw new JSONException("Cannot deserialize from null JSONObject");
        }
        
        try {
            String jsonStr = safeExtractJsonString(obj);
            
            if (jsonStr != null && !jsonStr.trim().isEmpty() && !jsonStr.equals("{}")) {
                T gsonResult = (T) GsonFactory.getInstance().fromJson(jsonStr, this.getClass());
                
                if (gsonResult != null) {
                    copyFieldsFrom(gsonResult);
                }
            } else {
                restoreFromJsonObjectManually(obj);
            }
            
            return (T) this;
            
        } catch (Exception e) {
            logger.error("Failed to deserialize " + this.getClass().getSimpleName(), e);
            throw new JSONException("Deserialization failed: " + e.getMessage());
        }
    }

    /**
     * 从 Gson 反序列化的结果复制字段到 this
     */
    private void copyFieldsFrom(CommandRequest source) {
        try {
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(source);

                java.lang.reflect.Field targetField = findField(field.getName());
                if (targetField != null) {
                    targetField.setAccessible(true);
                    targetField.set(this, value);
                }
            }

            Class<?> superClass = source.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                java.lang.reflect.Field[] superFields = superClass.getDeclaredFields();
                for (java.lang.reflect.Field field : superFields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(source);

                    java.lang.reflect.Field targetField = findField(field.getName());
                    if (targetField != null) {
                        targetField.setAccessible(true);
                        targetField.set(this, value);
                    }
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
            logger.error("Failed to copy fields from Gson result", e);
        }
    }

    /**
     * 手动从 JSONObject 恢复字段值（最后的 fallback）
     */
    private CommandRequest restoreFromJsonObjectManually(JSONObject obj) {
        try {
            java.util.Iterator<String> keys = obj.keys();
            if (!keys.hasNext()) {
                logger.warn("JSONObject has no keys, cannot restore fields");
                return this;
            }
            
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = obj.opt(key);
                
                try {
                    setFieldValueByName(key, value);
                } catch (Exception e) {
                    logger.debug("Failed to set field '" + key + "': " + e.getMessage());
                }
            }
            
            return this;
        } catch (Exception e) {
            logger.error("Manual field restoration failed", e);
            return this;
        }
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldValueByName(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(fieldName);
            if (field == null) {
                logger.debug("Field not found: " + fieldName);
                return;
            }
            
            field.setAccessible(true);
            
            if (value == null || value == JSONObject.NULL) {
                field.set(this, null);
                return;
            }
            
            Class<?> fieldType = field.getType();
            
            if (fieldType == boolean.class || fieldType == Boolean.class) {
                field.set(this, value instanceof Boolean ? value : Boolean.parseBoolean(value.toString()));
            } else if (fieldType == int.class || fieldType == Integer.class) {
                field.set(this, value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString()));
            } else if (fieldType == long.class || fieldType == Long.class) {
                field.set(this, value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString()));
            } else if (fieldType == double.class || fieldType == Double.class) {
                field.set(this, value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString()));
            } else if (fieldType == float.class || fieldType == Float.class) {
                field.set(this, value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString()));
            } else if (fieldType == String.class) {
                field.set(this, value.toString());
            } else {
                field.set(this, value);
            }
        } catch (Exception e) {
            logger.debug("Error setting field '" + fieldName + "': " + e.getMessage());
        }
    }

    /**
     * 查找字段（包括父类）
     */
    private java.lang.reflect.Field findField(String fieldName) {
        Class<?> currentClass = this.getClass();
        
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return null;
    }

    /**
     * 安全地从 JSONObject 提取 JSON 字符串
     * 解决 Android 测试环境 org.json.toString() 返回 null 的问题
     */
    private String safeExtractJsonString(JSONObject obj) {
        if (obj == null) return null;
        
        try {
            String str = obj.toString();
            
            if (!str.trim().isEmpty() && !str.equals("{}")) {
                return str;
            }
        } catch (Exception e) {
            // toString failed, try manual extraction
        }
        
        try {
            Iterator<String> keys = obj.keys();
            
            if (keys.hasNext()) {
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = obj.opt(key);
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("\"").append(escapeJsonString(key)).append("\":");
                    sb.append(convertToJsonString(value));
                }
                sb.append("}");
                return sb.toString();
            }
        } catch (Exception e) {
            // Manual extraction failed
        }
        
        return null;
    }

    private String convertToJsonString(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJsonString((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof JSONObject) {
            return safeExtractJsonString((JSONObject) value);
        } else {
            return "\"" + escapeJsonString(value.toString()) + "\"";
        }
    }

    private String escapeJsonString(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private JSONObject createMinimalJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("requestId", getRequestId() != null ? getRequestId() : "unknown");
        json.put("commandType", getCommandType() != null ? getCommandType() : "unknown");
        return json;
    }

    public CommandRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(getClass(), args);
    }

    /**
     * 纯 Gson 序列化（推荐使用）
     * 返回 JSON 字符串，完全绕过 org.json
     * 适用于所有环境（包括 Android 测试）
     */
    public String toJsonString() {
        return GsonFactory.getInstance().toJson(this);
    }

    /**
     * 纯 Gson 反序列化（推荐使用）
     * 从 JSON 字符串反序列化，完全绕过 org.json
     * 适用于所有环境（包括 Android 测试）
     */
    @SuppressWarnings("unchecked")
    public <T extends CommandRequest> T fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            logger.warn("Cannot deserialize from null/empty string");
            return (T) this;
        }
        
        try {
            T result = (T) GsonFactory.getInstance().fromJson(jsonStr, this.getClass());
            
            if (result != null) {
                copyFieldsFrom(result);
                return (T) this;
            } else {
                logger.warn("Gson returned null for " + this.getClass().getSimpleName());
                return (T) this;
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize from string", e);
            return (T) this;
        }
    }
}
