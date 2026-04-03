package com.justnothing.testmodule.command.functions.script.engine_new.evaluator;

import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputCollector;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class Builtins {
    
    @FunctionalInterface
    public interface BuiltinFunction {
        Object call(List<Object> args);
    }
    
    private final Map<String, BuiltinFunction> functions;
    private ClassLoader contextClassLoader;
    private IOutputHandler outputHandler;
    private IOutputHandler errorHandler;
    private ExecutionContext executionContext;
    
    public Builtins() {
        this.functions = new HashMap<>();
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.outputHandler = new SystemOutputCollector(System.out, System.in);
        this.errorHandler = new SystemOutputCollector(System.err, System.in);
        registerAll();
    }
    
    public void setContextClassLoader(ClassLoader classLoader) {
        this.contextClassLoader = classLoader;
    }
    
    public void setOutputHandler(IOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }
    
    public void setErrorHandler(IOutputHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    public void setExecutionContext(ExecutionContext context) {
        this.executionContext = context;
    }
    
    public void registerFunction(String name, BuiltinFunction function) {
        functions.put(name, function);
    }
    
    private void registerAll() {
        registerOutputFunctions();
        registerCollectionFunctions();
        registerReflectionFunctions();
        registerMathFunctions();
        registerTimeFunctions();
        registerUtilityFunctions();
        registerLegacyFunctions();
    }
    
    private void registerOutputFunctions() {
        functions.put("println", args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(formatValue(args.get(i)));
            }
            outputHandler.println(sb.toString());
            return null;
        });
        
        functions.put("print", args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(formatValue(args.get(i)));
            }
            outputHandler.print(sb.toString());
            return null;
        });
        
        functions.put("printf", args -> {
            if (args.isEmpty()) {
                throw new RuntimeException("printf() requires at least one argument");
            }
            String format = args.get(0).toString();
            Object[] params = args.subList(1, args.size()).toArray();
            outputHandler.printf(format, params);
            return null;
        });
        
        functions.put("readLine", args -> {
            String prompt = args.isEmpty() ? "" : args.get(0).toString();
            return outputHandler.readLineFromClient(prompt);
        });
        
        functions.put("readPassword", args -> {
            String prompt = args.isEmpty() ? "" : args.get(0).toString();
            return outputHandler.readPasswordFromClient(prompt);
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
            if (args.isEmpty() || args.size() > 2) {
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
        } else if (predicate instanceof Method method) {
            try {
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
        } else if (func instanceof Method method) {
            try {
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
                outputHandler.println(result.toString());
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
            
            outputHandler.println(result.toString());
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
                    targetClass = ClassResolver.findClassOrFail(className.toString(), contextClassLoader);
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
                    targetClass = ClassResolver.findClassOrFail(className.toString(), contextClassLoader);
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
        
        functions.put("setPrintAST", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("setPrintAST() requires exactly 1 argument");
            }
            if (executionContext == null) {
                throw new RuntimeException("ExecutionContext not set");
            }
            Object value = args.get(0);
            boolean enabled = value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString());
            executionContext.setPrintAST(enabled);
            return enabled;
        });
        
        functions.put("isPrintAST", args -> {
            if (executionContext == null) {
                return false;
            }
            return executionContext.isPrintAST();
        });
        
        functions.put("setupInteractiveInput", args -> {
            InputStream interactiveInputStream = new InputStream() {
                private byte[] buffer = null; // 保留给不读取一整行的情况
                private int pos = 0;
                
                @Override
                public int read() {
                    if (buffer == null || pos >= buffer.length) {
                        String line = outputHandler.readLineFromClient("");
                        if (line == null) {
                            return -1;
                        }
                        buffer = (line + "\n").getBytes();
                        pos = 0;
                    }
                    return buffer[pos++] & 0xFF;
                }
                
                @Override
                public int read(byte[] b, int off, int len) {
                    if (b == null) {
                        throw new NullPointerException();
                    } else if (off < 0 || len < 0 || len > b.length - off) {
                        throw new IndexOutOfBoundsException();
                    } else if (len == 0) {
                        return 0;
                    }
                    
                    int c = read();
                    if (c == -1) {
                        return -1;
                    }
                    b[off] = (byte) c;
                    
                    int i = 1;
                    for (; i < len; i++) {
                        c = read();
                        if (c == -1) {
                            break;
                        }
                        b[off + i] = (byte) c;
                    }
                    return i;
                }
            };
            
            System.setIn(interactiveInputStream);
            return true;
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
    
    private void registerLegacyFunctions() {
        functions.put("getInterpreterClassLoader", args -> contextClassLoader);
        
        functions.put("enableLog", args -> null);
        
        functions.put("deepAnalyze", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("deepAnalyze() requires exactly 1 argument");
            }
            
            Object target = args.get(0);
            StringBuilder result = new StringBuilder();
            
            if (target == null) {
                result.append("Target object is null\n");
                outputHandler.println(result.toString());
                return null;
            }
            
            Class<?> clazz = target.getClass();
            result.append("=== Deep Object Analysis ===\n");
            result.append("String: ").append(target).append("\n");
            result.append("Class Name: ").append(clazz.getName()).append("\n");
            result.append("Simple Name: ").append(clazz.getSimpleName()).append("\n");
            result.append("Package: ").append(clazz.getPackage() != null ? clazz.getPackage().getName() : "None").append("\n");
            result.append("Is Array: ").append(clazz.isArray()).append("\n");
            result.append("Is Interface: ").append(clazz.isInterface()).append("\n");
            result.append("Is Annotation: ").append(clazz.isAnnotation()).append("\n");
            result.append("Is Enum: ").append(clazz.isEnum()).append("\n");
            result.append("Is Primitive: ").append(clazz.isPrimitive()).append("\n\n");
            
            Set<Field> allFields = new LinkedHashSet<>();
            Set<Method> allMethods = new LinkedHashSet<>();
            Map<Field, Class<?>> fieldSources = new HashMap<>();
            Map<Method, Class<?>> methodSources = new HashMap<>();
            
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                for (Field field : currentClass.getDeclaredFields()) {
                    allFields.add(field);
                    fieldSources.put(field, currentClass);
                }
                for (Method method : currentClass.getDeclaredMethods()) {
                    allMethods.add(method);
                    methodSources.put(method, currentClass);
                }
                for (Class<?> iface : currentClass.getInterfaces()) {
                    collectInterfaceMembers(iface, allFields, allMethods, fieldSources, methodSources);
                }
                currentClass = currentClass.getSuperclass();
            }
            
            result.append("=== Fields (with inheritance) ===\n");
            if (allFields.isEmpty()) {
                result.append("No fields\n");
            } else {
                for (Field field : allFields) {
                    Class<?> sourceClass = fieldSources.get(field);
                    result.append("  ").append(field.toString());
                    try {
                        field.setAccessible(true);
                        Object value = field.get(target);
                        result.append(" = ").append(value != null ? value.toString() : "null");
                    } catch (Exception e) {
                        result.append(" = [Cannot access: ").append(e.getMessage()).append("]");
                    }
                    if (sourceClass != clazz) {
                        if (sourceClass.isInterface()) {
                            result.append("\n    └─> implements ").append(sourceClass.getName());
                        } else {
                            result.append("\n    └─> extends ").append(sourceClass.getName());
                        }
                    }
                    result.append("\n");
                }
            }
            result.append("Total Fields: ").append(allFields.size()).append("\n\n");
            
            result.append("=== Methods (with inheritance) ===\n");
            if (allMethods.isEmpty()) {
                result.append("No methods\n");
            } else {
                for (Method method : allMethods) {
                    Class<?> sourceClass = methodSources.get(method);
                    result.append("  ").append(method.toString());
                    if (sourceClass != clazz) {
                        if (sourceClass.isInterface()) {
                            result.append("\n    └─> implements ").append(sourceClass.getName());
                        } else {
                            result.append("\n    └─> extends ").append(sourceClass.getName());
                        }
                    }
                    result.append("\n");
                }
            }
            result.append("Total Methods: ").append(allMethods.size()).append("\n\n");
            
            result.append("=== Superclass Hierarchy ===\n");
            Class<?> superClass = clazz.getSuperclass();
            int level = 0;
            while (superClass != null) {
                result.append("  ").append("  ".repeat(level)).append("└─> ").append(superClass.getName()).append("\n");
                superClass = superClass.getSuperclass();
                level++;
            }
            if (level == 0) {
                result.append("No superclass\n");
            }
            result.append("\n");
            
            result.append("=== Implemented Interfaces ===\n");
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length == 0) {
                result.append("No interfaces\n");
            } else {
                for (Class<?> _interface : interfaces) {
                    result.append("  └─> ").append(_interface.getName()).append("\n");
                }
            }
            result.append("Total Interfaces: ").append(interfaces.length).append("\n");
            
            outputHandler.println(result.toString());
            return null;
        });
        
        functions.put("getContext", args -> {
            try {
                Class<?> activityThreadClass = ClassResolver.findClassOrFail("android.app.ActivityThread", contextClassLoader);
                Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
                Object activityThread = currentActivityThreadMethod.invoke(null);
                Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
                return getApplicationMethod.invoke(activityThread);
            } catch (Exception e1) {
                try {
                    Class<?> contextImplClass = ClassResolver.findClassOrFail("android.app.ContextImpl", contextClassLoader);
                    Method getSystemContextMethod = contextImplClass.getMethod("getSystemContext");
                    return getSystemContextMethod.invoke(null);
                } catch (Exception e2) {
                    throw new RuntimeException("Failed to get Context. Are you running in an Android environment?");
                }
            }
        });
        
        functions.put("getApplicationInfo", args -> {
            try {
                Object context = functions.get("getContext").call(Collections.emptyList());
                Method getApplicationInfoMethod = context.getClass().getMethod("getApplicationInfo");
                return getApplicationInfoMethod.invoke(context);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get ApplicationInfo: " + e.getMessage());
            }
        });
        
        functions.put("getPackageName", args -> {
            try {
                Object context = functions.get("getContext").call(Collections.emptyList());
                Method getPackageNameMethod = context.getClass().getMethod("getPackageName");
                return getPackageNameMethod.invoke(context);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get package name: " + e.getMessage());
            }
        });
        
        functions.put("createSafeExecutor", args -> new Object() {
            private Object createLooperThread() throws Exception {
                Class<?> handlerThreadClass = ClassResolver.findClassOrFail("android.os.HandlerThread", contextClassLoader);
                Constructor<?> constructor = handlerThreadClass.getConstructor(String.class);
                Object handlerThread = constructor.newInstance("SafeExecutor-Thread");
                handlerThreadClass.getMethod("start").invoke(handlerThread);
                return handlerThread;
            }
            
            private Object getThreadLooper(Object handlerThread) throws Exception {
                return handlerThread.getClass().getMethod("getLooper").invoke(handlerThread);
            }
            
            private Object createHandler(Object looper) throws Exception {
                Class<?> handlerClass = ClassResolver.findClassOrFail("android.os.Handler", contextClassLoader);
                Class<?> looperClass = ClassResolver.findClassOrFail("android.os.Looper", contextClassLoader);
                Constructor<?> constructor = handlerClass.getConstructor(looperClass);
                return constructor.newInstance(looper);
            }
            
            public Object runOnMainThread(Callable<Object> task) throws Exception {
                Class<?> looperClass = ClassResolver.findClassOrFail("android.os.Looper", contextClassLoader);
                Class<?> handlerClass = ClassResolver.findClassOrFail("android.os.Handler", contextClassLoader);
                Class<?> runnableClass = Runnable.class;
                
                Method getMainLooperMethod = looperClass.getMethod("getMainLooper");
                Object mainLooper = getMainLooperMethod.invoke(null);
                
                Constructor<?> handlerConstructor = handlerClass.getConstructor(looperClass);
                Object mainHandler = handlerConstructor.newInstance(mainLooper);
                
                CountDownLatch latch = new CountDownLatch(1);
                final Object[] result = new Object[1];
                final Exception[] exception = new Exception[1];
                
                Object runnable = Proxy.newProxyInstance(
                    contextClassLoader,
                    new Class[] { runnableClass },
                    (proxy, method, params) -> {
                        if (method.getName().equals("run")) {
                            try {
                                result[0] = task.call();
                            } catch (Exception e) {
                                exception[0] = e;
                            } finally {
                                latch.countDown();
                            }
                        }
                        return null;
                    });
                
                Method postMethod = handlerClass.getMethod("post", runnableClass);
                postMethod.invoke(mainHandler, runnable);
                
                latch.await();
                
                if (exception[0] != null) {
                    throw exception[0];
                }
                
                return result[0];
            }
            
            public Object runOnLooperThread(Callable<Object> task) throws Exception {
                Object handlerThread = createLooperThread();
                try {
                    Object looper = getThreadLooper(handlerThread);
                    Object handler = createHandler(looper);
                    
                    CountDownLatch latch = new CountDownLatch(1);
                    final Object[] result = new Object[1];
                    final Exception[] exception = new Exception[1];
                    
                    Object runnable = Proxy.newProxyInstance(
                        contextClassLoader,
                        new Class[] { Runnable.class },
                        (proxy, method, params) -> {
                            if (method.getName().equals("run")) {
                                try {
                                    result[0] = task.call();
                                } catch (Exception e) {
                                    exception[0] = e;
                                } finally {
                                    latch.countDown();
                                }
                            }
                            return null;
                        });
                    
                    Method postMethod = handler.getClass().getMethod("post", Runnable.class);
                    postMethod.invoke(handler, runnable);
                    
                    latch.await();
                    
                    if (exception[0] != null) {
                        throw exception[0];
                    }
                    
                    return result[0];
                } finally {
                    try {
                        handlerThread.getClass().getMethod("quit").invoke(handlerThread);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            
            public Object runWithLooper(Callable<Object> task) throws Exception {
                Class<?> looperClass = ClassResolver.findClassOrFail("android.os.Looper", contextClassLoader);
                Method myLooperMethod = looperClass.getMethod("myLooper");
                Object currentLooper = myLooperMethod.invoke(null);
                
                boolean needPrepare = currentLooper == null;
                boolean needLoop = false;
                
                if (needPrepare) {
                    Method prepareMethod = looperClass.getMethod("prepare");
                    prepareMethod.invoke(null);
                    needLoop = true;
                }
                
                try {
                    Object result = task.call();
                    
                    if (needLoop) {
                        Object handler = createHandler(myLooperMethod.invoke(null));
                        
                        Class<?> handlerClass = ClassResolver.findClassOrFail("android.os.Handler", contextClassLoader);
                        Method postDelayedMethod = handlerClass.getMethod("postDelayed", Runnable.class, long.class);
                        
                        Object quitRunnable = Proxy.newProxyInstance(
                            contextClassLoader,
                            new Class[] { Runnable.class },
                            (proxy, method, params) -> {
                                if (method.getName().equals("run")) {
                                    try {
                                        Method quitMethod = looperClass.getMethod("quit");
                                        quitMethod.invoke(currentLooper);
                                    } catch (Exception e) {
                                        // ignore
                                    }
                                }
                                return null;
                            });
                        
                        postDelayedMethod.invoke(handler, quitRunnable, 100L);
                        
                        Method loopMethod = looperClass.getMethod("loop");
                        loopMethod.invoke(null);
                    }
                    
                    return result;
                } catch (Exception e) {
                    if (needLoop) {
                        try {
                            Method quitMethod = looperClass.getMethod("quit");
                            quitMethod.invoke(currentLooper);
                        } catch (Exception e2) {
                            // ignore
                        }
                    }
                    throw e;
                }
            }
            
            public Object createInstanceWithHandler(String className, Object... args) throws Exception {
                return runWithLooper(() -> {
                    Class<?> clazz = ClassResolver.findClassOrFail(className, contextClassLoader);
                    
                    for (Constructor<?> constructor : clazz.getConstructors()) {
                        if (constructor.getParameterTypes().length == args.length) {
                            boolean match = true;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] != null && !constructor.getParameterTypes()[i].isAssignableFrom(args[i].getClass())) {
                                    match = false;
                                    break;
                                }
                            }
                            if (match) {
                                return constructor.newInstance(args);
                            }
                        }
                    }
                    
                    throw new RuntimeException("No matching constructor found");
                });
            }
        });
        
        functions.put("asRunnable", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("asRunnable() requires one parameter: lambda expression or function");
            }
            
            Object func = args.get(0);
            return Proxy.newProxyInstance(
                contextClassLoader,
                new Class[] { Runnable.class },
                (proxy, method, params) -> {
                    if (method.getName().equals("run")) {
                        if (func instanceof Lambda) {
                            ((Lambda) func).invoke(new Object[0]);
                        }
                    }
                    return null;
                });
        });
        
        functions.put("asFunction", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("asFunction() requires one parameter: lambda expression or function");
            }
            
            Object func = args.get(0);
            return Proxy.newProxyInstance(
                contextClassLoader,
                new Class[] { java.util.function.Function.class },
                (proxy, method, params) -> {
                    if (method.getName().equals("apply")) {
                        if (func instanceof Lambda) {
                            return ((Lambda) func).invoke(new Object[]{params[0]});
                        }
                    }
                    return null;
                });
        });
        
        functions.put("runLater", args -> {
            if (args.size() != 1) {
                throw new RuntimeException("runLater() requires one parameter: Runnable");
            }
            
            Object runnable = args.get(0);
            Thread thread = new Thread(() -> {
                try {
                    if (runnable instanceof Lambda) {
                        ((Lambda) runnable).invoke(new Object[0]);
                    } else if (runnable instanceof Runnable) {
                        ((Runnable) runnable).run();
                    }
                } catch (Exception e) {
                    errorHandler.println("runLater error: " + e.getMessage());
                }
            });
            thread.start();
            return null;
        });
    }
    
    private void collectInterfaceMembers(Class<?> iface, Set<Field> allFields, Set<Method> allMethods,
                                         Map<Field, Class<?>> fieldSources, Map<Method, Class<?>> methodSources) {
        for (Field field : iface.getDeclaredFields()) {
            allFields.add(field);
            fieldSources.put(field, iface);
        }
        for (Method method : iface.getDeclaredMethods()) {
            allMethods.add(method);
            methodSources.put(method, iface);
        }
        for (Class<?> superIface : iface.getInterfaces()) {
            collectInterfaceMembers(superIface, allFields, allMethods, fieldSources, methodSources);
        }
    }
}
