package com.justnothing.testmodule.command.functions.script_new.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenericType {
    
    private final Class<?> rawType;
    private final List<GenericType> typeArguments;
    private final int arrayDepth;
    private final String originalTypeName;
    
    public GenericType(Class<?> rawType) {
        this(rawType, Collections.emptyList(), 0, null);
    }
    
    public GenericType(Class<?> rawType, List<GenericType> typeArguments) {
        this(rawType, typeArguments, 0, null);
    }
    
    public GenericType(Class<?> rawType, List<GenericType> typeArguments, int arrayDepth) {
        this(rawType, typeArguments, arrayDepth, null);
    }
    
    public GenericType(Class<?> rawType, List<GenericType> typeArguments, int arrayDepth, String originalTypeName) {
        this.rawType = rawType;
        this.typeArguments = typeArguments != null ? 
            Collections.unmodifiableList(new ArrayList<>(typeArguments)) : 
            Collections.emptyList();
        this.arrayDepth = arrayDepth;
        this.originalTypeName = originalTypeName;
    }
    
    public Class<?> getRawType() {
        return rawType;
    }
    
    public List<GenericType> getTypeArguments() {
        return typeArguments;
    }
    
    public int getArrayDepth() {
        return arrayDepth;
    }
    
    public String getOriginalTypeName() {
        return originalTypeName;
    }
    
    public boolean isGeneric() {
        return !typeArguments.isEmpty();
    }
    
    public boolean isArray() {
        return arrayDepth > 0;
    }
    
    public Class<?> getRuntimeType() {
        Class<?> type = rawType;
        for (int i = 0; i < arrayDepth; i++) {
            type = java.lang.reflect.Array.newInstance(type, 0).getClass();
        }
        return type;
    }
    
    public String getTypeName() {
        if (originalTypeName != null) {
            StringBuilder sb = new StringBuilder(originalTypeName);
            
            if (!typeArguments.isEmpty()) {
                sb.append("<");
                for (int i = 0; i < typeArguments.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(typeArguments.get(i).getTypeName());
                }
                sb.append(">");
            }
            
            for (int i = 0; i < arrayDepth; i++) {
                sb.append("[]");
            }
            
            return sb.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(rawType.getSimpleName());
        
        if (!typeArguments.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeArguments.get(i).getTypeName());
            }
            sb.append(">");
        }
        
        for (int i = 0; i < arrayDepth; i++) {
            sb.append("[]");
        }
        
        return sb.toString();
    }
    
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType.getName());
        
        if (!typeArguments.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeArguments.get(i).getFullName());
            }
            sb.append(">");
        }
        
        for (int i = 0; i < arrayDepth; i++) {
            sb.append("[]");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getTypeName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GenericType other = (GenericType) obj;
        if (!rawType.equals(other.rawType)) return false;
        if (arrayDepth != other.arrayDepth) return false;
        return typeArguments.equals(other.typeArguments);
    }
    
    @Override
    public int hashCode() {
        int result = rawType.hashCode();
        result = 31 * result + typeArguments.hashCode();
        result = 31 * result + arrayDepth;
        return result;
    }
    
    public static GenericType of(Class<?> rawType) {
        return new GenericType(rawType);
    }
    
    public static GenericType of(Class<?> rawType, GenericType... typeArguments) {
        List<GenericType> args = new ArrayList<>();
        Collections.addAll(args, typeArguments);
        return new GenericType(rawType, args);
    }
    
    public static GenericType arrayOf(GenericType elementType, int dimensions) {
        return new GenericType(elementType.rawType, elementType.typeArguments, dimensions);
    }
}
