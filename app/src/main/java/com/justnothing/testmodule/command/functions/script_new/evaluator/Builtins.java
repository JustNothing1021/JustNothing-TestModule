package com.justnothing.testmodule.command.functions.script_new.evaluator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Builtins {
    
    @FunctionalInterface
    public interface BuiltinFunction {
        Object call(List<Object> args);
    }
    
    private final Map<String, BuiltinFunction> functions;
    
    public Builtins() {
        this.functions = new HashMap<>();
        registerAll();
    }
    
    private void registerAll() {
        registerOutputFunctions();
        registerCollectionFunctions();
        registerReflectionFunctions();
        registerMathFunctions();
        registerTimeFunctions();
        registerUtilityFunctions();
    }
    
    private void registerOutputFunctions() {
        functions.put("println", args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(formatValue(args.get(i)));
            }
            System.out.println(sb.toString());
            return null;
        });
        
        functions.put("print", args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(formatValue(args.get(i)));
            }
            System.out.print(sb.toString());
            return null;
        });
        
        functions.put("printf", args -> {
            if (args.isEmpty()) {
                throw new RuntimeException("printf() requires at least one argument");
            }
            String format = args.get(0).toString();
            Object[] params = args.subList(1, args.size()).toArray();
            System.out.printf(format, params);
            return null;
        });
    }
    
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(Array.get(value, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return value.toString();
    }
    
    private void registerCollectionFunctions() {
        functions.put("range", args -> {
            if (args.size() == 1) {
                int end = ((Number) args.get(0)).intValue();
                return createRange(0, end, 1);
            } else if (args.size() == 2) {
                int start = ((Number) args.get(0)).intValue();
                int end = ((Number) args.get(1)).intValue();
                return createRange(start, end, 1);
            } else if (args.size() == 3) {
                int start = ((Number) args.get(0)).intValue();
                int end = ((Number) args.get(1)).intValue();
                int step = ((Number) args.get(2)).intValue();
                return createRange(start, end, step);
            }
            throw new RuntimeException("range() takes 1-3 arguments");
        });
        
        functions.put("keys", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("keys() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (!(obj instanceof Map)) {
                throw new RuntimeException("keys() argument must be a Map");
            }
            return new ArrayList<>(((Map<?, ?>) obj).keySet());
        });
        
        functions.put("values", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("values() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (!(obj instanceof Map)) {
                throw new RuntimeException("values() argument must be a Map");
            }
            return new ArrayList<>(((Map<?, ?>) obj).values());
        });
        
        functions.put("entries", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("entries() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (!(obj instanceof Map)) {
                throw new RuntimeException("entries() argument must be a Map");
            }
            return new ArrayList<>(((Map<?, ?>) obj).entrySet());
        });
        
        functions.put("size", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("size() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj instanceof Collection) {
                return ((Collection<?>) obj).size();
            } else if (obj instanceof Map) {
                return ((Map<?, ?>) obj).size();
            } else if (obj != null && obj.getClass().isArray()) {
                return java.lang.reflect.Array.getLength(obj);
            }
            throw new RuntimeException("size() argument must be a Collection, Map, or Array");
        });
        
        functions.put("contains", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("contains() requires exactly 2 arguments");
            }
            Object collection = args.get(0);
            Object element = args.get(1);
            if (collection instanceof Collection) {
                return ((Collection<?>) collection).contains(element);
            } else if (collection instanceof Map) {
                return ((Map<?, ?>) collection).containsKey(element);
            } else if (collection != null && collection.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(collection);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(collection, i);
                    if (item == null && element == null) return true;
                    if (item != null && item.equals(element)) return true;
                }
                return false;
            }
            throw new RuntimeException("contains() first argument must be a Collection, Map, or Array");
        });
        
        functions.put("split", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("split() requires exactly 2 arguments");
            }
            String str = args.get(0).toString();
            String delimiter = args.get(1).toString();
            return Arrays.asList(str.split(delimiter));
        });
        
        functions.put("join", args -> {
            if (args.size() < 1 || args.size() > 2) {
                throw new RuntimeException("join() requires 1 or 2 arguments");
            }
            Object collection = args.get(0);
            String delimiter = args.size() > 1 ? args.get(1).toString() : "";
            if (collection instanceof Collection) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Object item : (Collection<?>) collection) {
                    if (!first) sb.append(delimiter);
                    sb.append(item != null ? item.toString() : "null");
                    first = false;
                }
                return sb.toString();
            } else if (collection != null && collection.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(collection);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if (i > 0) sb.append(delimiter);
                    Object item = java.lang.reflect.Array.get(collection, i);
                    sb.append(item != null ? item.toString() : "null");
                }
                return sb.toString();
            }
            throw new RuntimeException("join() first argument must be a Collection or Array");
        });
        
        functions.put("filter", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("filter() requires exactly 2 arguments: collection and predicate");
            }
            Object collection = args.get(0);
            Object predicate = args.get(1);
            
            List<Object> result = new ArrayList<>();
            
            if (collection instanceof Collection) {
                for (Object item : (Collection<?>) collection) {
                    if (callPredicate(predicate, item)) {
                        result.add(item);
                    }
                }
            } else if (collection != null && collection.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(collection);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(collection, i);
                    if (callPredicate(predicate, item)) {
                        result.add(item);
                    }
                }
            } else {
                throw new RuntimeException("filter() first argument must be a Collection or Array");
            }
            
            return result.toArray();
        });
        
        functions.put("map", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("map() requires exactly 2 arguments: collection and mapper");
            }
            Object collection = args.get(0);
            Object mapper = args.get(1);
            
            List<Object> result = new ArrayList<>();
            
            if (collection instanceof Collection) {
                for (Object item : (Collection<?>) collection) {
                    result.add(callFunction(mapper, item));
                }
            } else if (collection != null && collection.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(collection);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(collection, i);
                    result.add(callFunction(mapper, item));
                }
            } else {
                throw new RuntimeException("map() first argument must be a Collection or Array");
            }
            
            return result.toArray();
        });
        
        functions.put("reduce", args -> {
            if (args.size() < 2 || args.size() > 3) {
                throw new RuntimeException("reduce() requires 2 or 3 arguments: collection, reducer, and optional initial value");
            }
            Object collection = args.get(0);
            Object reducer = args.get(1);
            Object accumulator = args.size() > 2 ? args.get(2) : null;
            
            if (collection instanceof Collection) {
                boolean first = (accumulator == null);
                for (Object item : (Collection<?>) collection) {
                    if (first) {
                        accumulator = item;
                        first = false;
                    } else {
                        accumulator = callFunction(reducer, accumulator, item);
                    }
                }
            } else if (collection != null && collection.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(collection);
                boolean first = (accumulator == null);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(collection, i);
                    if (first) {
                        accumulator = item;
                        first = false;
                    } else {
                        accumulator = callFunction(reducer, accumulator, item);
                    }
                }
            } else {
                throw new RuntimeException("reduce() first argument must be a Collection or Array");
            }
            
            return accumulator;
        });
    }
    
    private boolean callPredicate(Object predicate, Object item) {
        if (predicate instanceof Lambda) {
            Object result = ((Lambda) predicate).invoke(new Object[]{item});
            return result instanceof Boolean ? (Boolean) result : result != null;
        } else if (predicate instanceof java.lang.reflect.Method) {
            try {
                java.lang.reflect.Method method = (java.lang.reflect.Method) predicate;
                Object result = method.invoke(null, item);
                return result instanceof Boolean ? (Boolean) result : result != null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to call predicate: " + e.getMessage());
            }
        }
        throw new RuntimeException("Predicate must be a Lambda or Method");
    }
    
    private Object callFunction(Object func, Object... args) {
        if (func instanceof Lambda) {
            return ((Lambda) func).invoke(args);
        } else if (func instanceof java.lang.reflect.Method) {
            try {
                java.lang.reflect.Method method = (java.lang.reflect.Method) func;
                return method.invoke(null, args);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call function: " + e.getMessage());
            }
        }
        throw new RuntimeException("Function must be a Lambda or Method");
    }
    
    private void registerReflectionFunctions() {
        functions.put("analyze", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("analyze() requires exactly 1 argument");
            }
            Object target = args.get(0);
            StringBuilder result = new StringBuilder();
            
            if (target == null) {
                result.append("Target object is null\n");
                System.out.println(result.toString());
                return null;
            }
            
            Class<?> clazz = target.getClass();
            result.append("=== Object Analysis ===\n");
            result.append("String: ").append(target).append("\n");
            result.append("Class Name: ").append(clazz.getName()).append("\n");
            result.append("Simple Name: ").append(clazz.getSimpleName()).append("\n");
            result.append("Package: ").append(clazz.getPackage() != null ? clazz.getPackage().getName() : "None").append("\n");
            result.append("Is Array: ").append(clazz.isArray()).append("\n");
            result.append("Is Interface: ").append(clazz.isInterface()).append("\n");
            result.append("Is Primitive: ").append(clazz.isPrimitive()).append("\n\n");
            
            result.append("=== Fields ===\n");
            Field[] fields = clazz.getDeclaredFields();
            if (fields.length == 0) {
                result.append("No fields\n");
            } else {
                for (Field field : fields) {
                    result.append("  ").append(field.toString());
                    try {
                        field.setAccessible(true);
                        Object value = field.get(target);
                        result.append(" = ").append(value != null ? value.toString() : "null");
                    } catch (Exception e) {
                        result.append(" = [Cannot access: ").append(e.getMessage()).append("]");
                    }
                    result.append("\n");
                }
            }
            result.append("Total Fields: ").append(fields.length).append("\n\n");
            
            result.append("=== Methods ===\n");
            Method[] methods = clazz.getDeclaredMethods();
            if (methods.length == 0) {
                result.append("No methods\n");
            } else {
                for (Method method : methods) {
                    result.append("  ").append(method.toString()).append("\n");
                }
            }
            result.append("Total Methods: ").append(methods.length).append("\n");
            
            System.out.println(result.toString());
            return null;
        });
        
        functions.put("typename", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("typename() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj == null) {
                return "null";
            }
            return obj.getClass().getName();
        });
        
        functions.put("isInstanceOf", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("isInstanceOf() requires exactly 2 arguments");
            }
            Object obj = args.get(0);
            Object className = args.get(1);
            if (obj == null) {
                return false;
            }
            try {
                Class<?> targetClass;
                if (className instanceof Class) {
                    targetClass = (Class<?>) className;
                } else {
                    targetClass = Class.forName(className.toString());
                }
                return targetClass.isInstance(obj);
            } catch (Exception e) {
                throw new RuntimeException("Failed to find class: " + className, e);
            }
        });
        
        functions.put("cast", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("cast() requires exactly 2 arguments");
            }
            Object obj = args.get(0);
            Object className = args.get(1);
            if (obj == null) {
                return null;
            }
            try {
                Class<?> targetClass;
                if (className instanceof Class) {
                    targetClass = (Class<?>) className;
                } else {
                    targetClass = Class.forName(className.toString());
                }
                if (!targetClass.isInstance(obj)) {
                    throw new RuntimeException("Cannot cast " + obj.getClass().getName() + " to " + targetClass.getName());
                }
                return targetClass.cast(obj);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to find class: " + className, e);
            }
        });
        
        functions.put("getField", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("getField() requires exactly 2 arguments");
            }
            Object obj = args.get(0);
            String fieldName = args.get(1).toString();
            if (obj == null) {
                throw new RuntimeException("getField() first argument cannot be null");
            }
            try {
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get field: " + fieldName, e);
            }
        });
        
        functions.put("setField", args -> {
            if (args.size() != 3) {
                throw new RuntimeException("setField() requires exactly 3 arguments");
            }
            Object obj = args.get(0);
            String fieldName = args.get(1).toString();
            Object value = args.get(2);
            if (obj == null) {
                throw new RuntimeException("setField() first argument cannot be null");
            }
            try {
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to set field: " + fieldName, e);
            }
        });
        
        functions.put("invokeMethod", args -> {
            if (args.size() < 2) {
                throw new RuntimeException("invokeMethod() requires at least 2 arguments");
            }
            Object obj = args.get(0);
            String methodName = args.get(1).toString();
            if (obj == null) {
                throw new RuntimeException("invokeMethod() first argument cannot be null");
            }
            List<Object> methodArgs = args.subList(2, args.size());
            try {
                Method[] methods = obj.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        method.setAccessible(true);
                        return method.invoke(obj, methodArgs.toArray());
                    }
                }
                throw new RuntimeException("Method not found: " + methodName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke method: " + methodName, e);
            }
        });
    }
    
    private void registerMathFunctions() {
        functions.put("random", args -> Math.random());
        
        functions.put("randint", args -> {
            if (args.size() != 2) {
                throw new RuntimeException("randint() requires exactly 2 arguments");
            }
            int min = ((Number) args.get(0)).intValue();
            int max = ((Number) args.get(1)).intValue();
            return min + (int) (Math.random() * (max - min + 1));
        });
        
        functions.put("abs", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("abs() requires exactly 1 argument");
            }
            return Math.abs(((Number) args.get(0)).doubleValue());
        });
        
        functions.put("min", args -> {
            if (args.isEmpty()) {
                throw new RuntimeException("min() requires at least 1 argument");
            }
            double min = Double.POSITIVE_INFINITY;
            for (Object arg : args) {
                min = Math.min(min, ((Number) arg).doubleValue());
            }
            return min;
        });
        
        functions.put("max", args -> {
            if (args.isEmpty()) {
                throw new RuntimeException("max() requires at least 1 argument");
            }
            double max = Double.NEGATIVE_INFINITY;
            for (Object arg : args) {
                max = Math.max(max, ((Number) arg).doubleValue());
            }
            return max;
        });
        
        functions.put("clamp", args -> {
            if (args.size() != 3) {
                throw new RuntimeException("clamp() requires exactly 3 arguments");
            }
            double val = ((Number) args.get(0)).doubleValue();
            double minVal = ((Number) args.get(1)).doubleValue();
            double maxVal = ((Number) args.get(2)).doubleValue();
            return Math.max(minVal, Math.min(maxVal, val));
        });
    }
    
    private void registerTimeFunctions() {
        functions.put("currentTimeMillis", args -> System.currentTimeMillis());
        
        functions.put("nanoTime", args -> System.nanoTime());
        
        functions.put("sleep", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("sleep() requires exactly 1 argument");
            }
            long milliseconds = ((Number) args.get(0)).longValue();
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
    }
    
    private void registerUtilityFunctions() {
        functions.put("typeof", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("typeof() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj == null) {
                return "null";
            }
            return obj.getClass().getSimpleName();
        });
        
        functions.put("toStr", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("toStr() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            return obj != null ? obj.toString() : "null";
        });
        
        functions.put("toInt", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("toInt() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            }
            return Integer.parseInt(obj.toString());
        });
        
        functions.put("toDouble", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("toDouble() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            }
            return Double.parseDouble(obj.toString());
        });
        
        functions.put("toBool", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("toBool() requires exactly 1 argument");
            }
            Object obj = args.get(0);
            if (obj instanceof Boolean) {
                return obj;
            }
            return Boolean.parseBoolean(obj.toString());
        });
    }
    
    private List<Integer> createRange(int start, int end, int step) {
        List<Integer> list = new ArrayList<>();
        if (step > 0) {
            for (int i = start; i < end; i += step) {
                list.add(i);
            }
        } else if (step < 0) {
            for (int i = start; i > end; i += step) {
                list.add(i);
            }
        }
        return list;
    }
    
    public BuiltinFunction getFunction(String name) {
        return functions.get(name);
    }
    
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
    
    public Map<String, BuiltinFunction> getAllFunctions() {
        return Collections.unmodifiableMap(functions);
    }
}
