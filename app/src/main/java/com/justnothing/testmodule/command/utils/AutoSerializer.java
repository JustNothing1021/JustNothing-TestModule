package com.justnothing.testmodule.command.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.NoDefaultSupplier;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSerializer {

    private static final Logger logger = Logger.getLoggerForName("AutoSerializer");

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    private static final ConcurrentHashMap<String, Class<? extends CommandRequest>>
        requestRegistry = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Class<? extends CommandResult>>
        resultRegistry = new ConcurrentHashMap<>();

    public static void registerRequest(Class<? extends CommandRequest> requestClass) {
        if (requestClass == null || requestClass == CommandRequest.class) return;

        String typeKey = extractTypeKey(requestClass);

        if (typeKey != null && !typeKey.isEmpty()) {
            requestRegistry.put(typeKey, requestClass);
            logger.debug("注册请求类型: " + typeKey + " → " + requestClass.getSimpleName());
        } else {
            logger.warn("无法提取 Request 类型标识符: " + requestClass.getSimpleName());
        }
    }

    public static void registerResult(Class<? extends CommandResult> resultClass) {
        if (resultClass == null || resultClass == CommandResult.class) return;

        String typeKey = extractTypeKey(resultClass);

        if (typeKey != null && !typeKey.isEmpty()) {
            resultRegistry.put(typeKey, resultClass);
            logger.debug("注册响应类型: " + typeKey + " → " + resultClass.getSimpleName());
        } else {
            logger.warn("无法提取 Result 类型标识符: " + resultClass.getSimpleName());
        }
    }

    private static String extractTypeKey(Class<?> clazz) {
        SerializeKeyName keyName = clazz.getAnnotation(SerializeKeyName.class);
        if (keyName != null) {
            return keyName.value();
        }

        throw new IllegalStateException(
            "类 " + clazz.getSimpleName() +
            " 缺少 @SerializeKeyName 注解！注册到 AutoSerializer 的类必须声明 @SerializeKeyName"
        );
    }

    public static CommandRequest parseRequest(String json) {
        if (json == null || json.isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(json);
            String commandType = obj.optString("commandType");

            if (commandType.isEmpty()) {
                logger.warn("JSON 中缺少 commandType 字段");
                return null;
            }

            Class<? extends CommandRequest> clazz = requestRegistry.get(commandType);

            if (clazz != null) {
                CommandRequest request = fromJson(json, clazz);
                logger.debug("解析请求成功: " + commandType + " → " + clazz.getSimpleName());
                return request;
            }

            logger.warn("未注册的请求类型: " + commandType +
                        ", 已注册类型: " + requestRegistry.keySet());
            return null;

        } catch (Exception e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }

    public static CommandResult parseResponse(String json) {
        if (json == null || json.isEmpty()) {
            return createErrorResponse("空的 JSON 输入");
        }

        try {
            JSONObject obj = new JSONObject(json);
            String resultType = obj.optString("resultType", "");

            if (!resultType.isEmpty()) {
                Class<? extends CommandResult> clazz = resultRegistry.get(resultType);

                if (clazz != null) {
                    CommandResult result = fromJson(json, clazz);
                    logger.debug("解析响应成功: " + resultType + " → " + clazz.getSimpleName());
                    return result;
                }

                logger.warn("未注册的响应类型: " + resultType +
                            ", 已注册类型: " + resultRegistry.keySet());

                CommandResult result = tryInferAndDeserialize(json, resultType);
                if (result != null) return result;
            } else {
                logger.warn("JSON 中缺少 resultType 字段");
            }

            logger.info("降级到基类 CommandResult");
            return fromJson(json, CommandResult.class);

        } catch (Exception e) {
            logger.error("解析响应失败", e);
            return createErrorResponse(e.getMessage());
        }
    }

    private static final String[] RESULT_PACKAGE_PREFIXES = {
        "com.justnothing.testmodule.command.functions.alias.response",
        "com.justnothing.testmodule.command.functions.classcmd.response",
        "com.justnothing.testmodule.command.functions.bytecode.response",
        "com.justnothing.testmodule.command.functions.breakpoint.response",
        "com.justnothing.testmodule.command.functions.bsh.response",
        "com.justnothing.testmodule.command.functions.exportcontext"
    };

    @SuppressWarnings("unchecked")
    private static CommandResult tryInferAndDeserialize(String json, String resultType) {
        String[] candidates = {
            resultType + "Result",
            Character.toUpperCase(resultType.charAt(0)) + resultType.substring(1) + "Result"
        };

        for (String candidate : candidates) {
            for (String pkg : RESULT_PACKAGE_PREFIXES) {
                try {
                    String fullClassName = pkg + "." + candidate;
                    Class<?> clazz = Class.forName(fullClassName);

                    if (clazz.isAnnotationPresent(AutoSerializable.class) &&
                        CommandResult.class.isAssignableFrom(clazz)) {
                        registerResult((Class<? extends CommandResult>) clazz);

                        CommandResult result = fromJson(json, (Class<? extends CommandResult>) clazz);
                        logger.info("延迟注册成功: " + resultType + " → " + clazz.getSimpleName());
                        return result;
                    }
                } catch (ClassNotFoundException ignored) {
                } catch (Exception e) {
                    logger.debug("尝试加载 " + candidate + " 失败: " + e.getMessage());
                }
            }
        }

        return null;
    }

    private static CommandResult createErrorResponse(String message) {
        try {
            CommandResult errorResult = CommandResult.class.newInstance();
            errorResult.setSuccess(false);
            errorResult.setMessage(message);
            errorResult.setError(new CommandResult.ErrorInfo("PARSE_ERROR", message));
            return errorResult;
        } catch (Exception e) {
            throw new RuntimeException("创建错误响应失败", e);
        }
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }

        Class<?> clazz = obj.getClass();
        
        if (!isAutoSerializable(clazz)) {
            logger.debug("Class " + clazz.getSimpleName() + " is not @AutoSerializable, using default Gson");
            return gson.toJson(obj);
        }

        try {
            Map<String, Object> dataMap = extractFields(obj, clazz);
            return gson.toJson(dataMap);
        } catch (Exception e) {
            logger.error("Failed to auto-serialize: " + clazz.getSimpleName(), e);
            throw new RuntimeException("Auto-serialization failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + clazz.getSimpleName(), e);
            }
        }

        if (!isAutoSerializable(clazz)) {
            logger.debug("Class " + clazz.getSimpleName() + " is not @AutoSerializable, using default Gson");
            return gson.fromJson(json, clazz);
        }

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            
            Map<String, Object> dataMap = gson.fromJson(json, Map.class);
            populateFields(instance, dataMap, clazz);
            
            return instance;
        } catch (Exception e) {
            logger.error("Failed to auto-deserialize: " + clazz.getSimpleName(), e);
            throw new RuntimeException("Auto-deserialization failed: " + e.getMessage(), e);
        }
    }

    private static boolean isAutoSerializable(Class<?> clazz) {
        return clazz.isAnnotationPresent(AutoSerializable.class);
    }

    private static Map<String, Object> extractFields(Object obj, Class<?> clazz) throws IllegalAccessException {
        Map<String, Object> result = new LinkedHashMap<>();
        
        AutoSerializable annotation = clazz.getAnnotation(AutoSerializable.class);
        assert annotation != null;
        Set<String> excludeFields = new HashSet<>(Arrays.asList(annotation.excludeFields()));
        boolean includeSuperFields = annotation.includeSuperFields();

        List<Field> allFields = collectFields(clazz, includeSuperFields);

        for (Field field : allFields) {
            field.setAccessible(true);

            if (excludeFields.contains(field.getName())) {
                continue;
            }

            ResultField resultField = field.getAnnotation(ResultField.class);
            String key = (resultField != null) ? resultField.name() : field.getName();

            Object value = field.get(obj);
            
            if (value != null) {
                if (isAutoSerializable(value.getClass())) {
                    value = extractFields(value, value.getClass());
                } else if (value instanceof Collection) {
                    value = serializeCollection((Collection<?>) value);
                } else if (value instanceof Map) {
                    value = serializeMap((Map<?, ?>) value);
                }
                result.put(key, value);
            }
        }

        return result;
    }

    private static void populateFields(Object target, Map<String, Object> dataMap, Class<?> clazz) throws IllegalAccessException {
        AutoSerializable annotation = clazz.getAnnotation(AutoSerializable.class);
        assert annotation != null;
        Set<String> excludeFields = new HashSet<>(Arrays.asList(annotation.excludeFields()));
        boolean includeSuperFields = annotation.includeSuperFields();

        List<Field> allFields = collectFields(clazz, includeSuperFields);

        for (Field field : allFields) {
            field.setAccessible(true);

            if (excludeFields.contains(field.getName())) {
                continue;
            }

            ResultField resultField = field.getAnnotation(ResultField.class);
            String key = (resultField != null) ? resultField.name() : field.getName();

            Object value = dataMap.get(key);

            if (value == null && !dataMap.containsKey(key)) {
                if (resultField != null && resultField.defaultValue() != NoDefaultSupplier.class) {
                    try {
                        ValueSupplier supplier = resultField.defaultValue().getDeclaredConstructor().newInstance();
                        value = supplier.get();
                    } catch (Exception e) {
                        logger.warn("无法实例化默认值供应商: " + resultField.defaultValue().getSimpleName(), e);
                    }
                }
                if (value == null) {
                    continue;
                }
            }
            
            if (value != null) {
                Class<?> fieldType = field.getType();

                if (isAutoSerializable(fieldType)) {
                    value = fromJson(gson.toJson(value), fieldType);
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    Class<?> elementType = getCollectionElementType(field);
                    value = deserializeCollection(value, fieldType, elementType);
                } else if (Map.class.isAssignableFrom(fieldType)) {
                    value = deserializeMap((Map<?, ?>) value);
                } else {
                    value = convertValue(value, fieldType);
                }

                field.set(target, value);
            }
        }
    }

    private static Class<?> getCollectionElementType(Field field) {
        java.lang.reflect.Type genericType = field.getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] typeArgs = pType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return Object.class;
    }

    private static List<Field> collectFields(Class<?> clazz, boolean includeSuper) {
        List<Field> fields = new ArrayList<>();
        
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    !java.lang.reflect.Modifier.isTransient(field.getModifiers()) &&
                    !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    fields.add(field);
                }
            }
            
            if (!includeSuper) break;
            current = current.getSuperclass();
        }
        
        return fields;
    }

    private static Collection<?> serializeCollection(Collection<?> collection) {
        List<Object> resultList = new ArrayList<>();
        for (Object item : collection) {
            if (item != null && isAutoSerializable(item.getClass())) {
                try {
                    resultList.add(extractFields(item, item.getClass()));
                } catch (IllegalAccessException e) {
                    resultList.add(item);
                }
            } else {
                resultList.add(item);
            }
        }
        return resultList;
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> deserializeCollection(Object value, Class<T> collectionType, Class<?> elementType) {
        if (!(value instanceof Collection)) {
            logger.warn("Expected Collection but got " + (value != null ? value.getClass() : "null") + ", returning empty list");
            if (List.class.isAssignableFrom(collectionType)) {
                return new ArrayList<>();
            } else if (Set.class.isAssignableFrom(collectionType)) {
                return new HashSet<>();
            }
            return new ArrayList<>();
        }

        Collection<Object> sourceCollection = (Collection<Object>) value;
        Collection<T> result;

        try {
            if (List.class.isAssignableFrom(collectionType)) {
                result = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(collectionType)) {
                result = new HashSet<>();
            } else {
                result = new ArrayList<>();
            }

            for (Object item : sourceCollection) {
                if (item instanceof Map<?, ?> map) {
                    if (isAutoSerializable(elementType) && elementType != Object.class) {
                        item = fromJson(gson.toJson(map), elementType);
                    } else {
                        item = handleNestedObject(deserializeMap(map), elementType);
                    }
                }
                result.add((T) item);
            }

            return result;
        } catch (Exception e) {
            logger.warn("Failed to deserialize collection", e);
            return (Collection<T>) sourceCollection;
        }
    }

    private static Map<?, ?> serializeMap(Map<?, ?> map) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && isAutoSerializable(value.getClass())) {
                try {
                    value = extractFields(value, value.getClass());
                } catch (IllegalAccessException e) {
                    logger.warn("Failed to serialize nested object in map", e);
                }
            }
            resultMap.put(String.valueOf(entry.getKey()), value);
        }
        return resultMap;
    }

    private static Map<String, Object> deserializeMap(Map<?, ?> sourceMap) {

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object val = entry.getValue();
            result.put((String) entry.getKey(), val);
        }

        return result;
    }

    private static Object handleNestedObject(Map<String, Object> itemMap, Class<?> expectedType) {

        try {
            if (expectedType.isArray() || Collection.class.isAssignableFrom(expectedType)) {
                return itemMap;
            }
            
            Object nestedInstance = expectedType.getDeclaredConstructor().newInstance();
            populateFields(nestedInstance, itemMap, expectedType);
            return nestedInstance;
        } catch (Exception e) {
            logger.warn("Failed to deserialize nested object of type: " + expectedType.getSimpleName(), e);
            return itemMap;
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString());
        }
        
        if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(value.toString());
        }
        
        if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return Double.parseDouble(value.toString());
        }
        
        if (targetType == float.class || targetType == Float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            return Float.parseFloat(value.toString());
        }
        
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) return value;
            return Boolean.parseBoolean(value.toString());
        }
        
        if (targetType == String.class) {
            return value.toString();
        }

        return value;
    }

}
