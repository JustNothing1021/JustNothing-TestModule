package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.Colors;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;

public class DescriptorColorizer {

    public static void printColoredDescriptor(CommandExecutor.CmdExecContext ctx, Member member, boolean simple) {
        if (member instanceof Method method) {
            printMethodDescriptor(ctx, method, simple);
        } else if (member instanceof Field field) {
            printFieldDescriptor(ctx, field, simple);
        } else if (member instanceof Constructor<?> constructor) {
            printConstructorDescriptor(ctx, constructor, simple);
        }
    }

    private static void printMethodDescriptor(CommandExecutor.CmdExecContext ctx, Method method, boolean simple) {
        int modifiers = method.getModifiers();
        
        printModifiers(ctx, modifiers);
        
        TypeVariable<?>[] typeParms = method.getTypeParameters();
        if (typeParms.length > 0) {
            printTypeParameters(ctx, typeParms);
            ctx.print(" ", Colors.DEFAULT);
        }
        
        if (simple) {
            printClassType(ctx, method.getReturnType(), Colors.DARK_GREEN);
        } else {
            printType(ctx, method.getGenericReturnType());
        }
        ctx.print(" ", Colors.DEFAULT);
        
        if (!simple) {
            printType(ctx, method.getDeclaringClass(), Colors.GRAY);
            ctx.print(".", Colors.GRAY);
        }
        ctx.print(method.getName(), Colors.YELLOW);
        
        ctx.print("(", Colors.PURPLE);
        printParameters(ctx, method, simple);
        ctx.print(")", Colors.PURPLE);
        
        if (simple) {
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            if (exceptionTypes.length > 0) {
                ctx.print(" throws ", Colors.DARK_BLUE);
                for (int i = 0; i < exceptionTypes.length; i++) {
                    if (i > 0) {
                        ctx.print(", ", Colors.WHITE);
                    }
                    printClassType(ctx, exceptionTypes[i], Colors.GREEN);
                }
            }
        } else {
            Type[] exceptionTypes = method.getGenericExceptionTypes();
            if (exceptionTypes.length > 0) {
                ctx.print(" throws ", Colors.DARK_BLUE);
                for (int i = 0; i < exceptionTypes.length; i++) {
                    if (i > 0) {
                        ctx.print(", ", Colors.WHITE);
                    }
                    printType(ctx, exceptionTypes[i], Colors.GREEN);
                }
            }
        }
    }

    private static void printFieldDescriptor(CommandExecutor.CmdExecContext ctx, Field field, boolean simple) {
        int modifiers = field.getModifiers();
        
        printModifiers(ctx, modifiers);
        
        if (simple) {
            printClassType(ctx, field.getType(), Colors.DARK_GREEN);
        } else {
            printType(ctx, field.getGenericType());
        }
        ctx.print(" ", Colors.DEFAULT);
        
        if (simple) {
            ctx.print(field.getName(), Colors.CYAN);
        } else {
            printType(ctx, field.getDeclaringClass(), Colors.GRAY);
            ctx.print(".", Colors.GRAY);
            ctx.print(field.getName(), Colors.CYAN);
        }
    }

    private static void printConstructorDescriptor(CommandExecutor.CmdExecContext ctx, Constructor<?> constructor, boolean simple) {
        int modifiers = constructor.getModifiers();
        
        printModifiers(ctx, modifiers);
        
        if (simple) {
            printClassType(ctx, constructor.getDeclaringClass(), Colors.YELLOW);
        } else {
            printType(ctx, constructor.getDeclaringClass(), Colors.YELLOW);
        }
        
        ctx.print("(", Colors.PURPLE);
        if (simple) {
            Class<?>[] params = constructor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    ctx.print(", ", Colors.WHITE);
                }
                printClassType(ctx, params[i], Colors.GREEN);
            }
        } else {
            Type[] params = constructor.getGenericParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    ctx.print(", ", Colors.WHITE);
                }
                printType(ctx, params[i], Colors.GREEN);
            }
        }
        ctx.print(")", Colors.PURPLE);
    }

    public static String formatTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        
        StringBuilder result = new StringBuilder();
        int arrayDepth = 0;
        int i = 0;
        
        while (i < typeName.length() && typeName.charAt(i) == '[') {
            arrayDepth++;
            i++;
        }
        
        if (arrayDepth > 0) {
            char typeChar = typeName.charAt(i);
            String baseType;
            
            switch (typeChar) {
                case 'Z' -> baseType = "boolean";
                case 'B' -> baseType = "byte";
                case 'C' -> baseType = "char";
                case 'D' -> baseType = "double";
                case 'F' -> baseType = "float";
                case 'I' -> baseType = "int";
                case 'J' -> baseType = "long";
                case 'S' -> baseType = "short";
                case 'L' -> baseType = typeName.substring(i + 1, typeName.length() - 1);
                default -> baseType = typeName.substring(i);
            }
            
            result.append(baseType);
            for (int j = 0; j < arrayDepth; j++) {
                result.append("[]");
            }
        } else {
            result.append(typeName);
        }
        
        return result.toString();
    }

    private static void printType(CommandExecutor.CmdExecContext ctx, Type type) {
        printType(ctx, type, Colors.GREEN);
    }

    private static void printType(CommandExecutor.CmdExecContext ctx, Type type, byte color) {
        if (type instanceof Class<?> clazz) {
            printClassType(ctx, clazz, color);
        } else if (type instanceof GenericArrayType gat) {
            printType(ctx, gat.getGenericComponentType(), color);
            ctx.print("[", Colors.CYAN);
            ctx.print("]", Colors.CYAN);
        } else if (type instanceof ParameterizedType pt) {
            printType(ctx, pt.getRawType(), color);
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0) {
                ctx.print("<", Colors.WHITE);
                for (int i = 0; i < typeArgs.length; i++) {
                    if (i > 0) {
                        ctx.print(", ", Colors.WHITE);
                    }
                    printType(ctx, typeArgs[i], color);
                }
                ctx.print(">", Colors.WHITE);
            }
        } else if (type instanceof WildcardType wt) {
            Type[] upperBounds = wt.getUpperBounds();
            Type[] lowerBounds = wt.getLowerBounds();
            if (lowerBounds.length > 0) {
                ctx.print("?", Colors.DARK_BLUE);
                ctx.print(" super ", Colors.DARK_BLUE);
                printType(ctx, lowerBounds[0], color);
            } else if (upperBounds.length > 0 && upperBounds[0] != Object.class) {
                ctx.print("?", Colors.DARK_BLUE);
                ctx.print(" extends ", Colors.DARK_BLUE);
                printType(ctx, upperBounds[0], color);
            } else {
                ctx.print("?", Colors.DARK_BLUE);
            }
        } else if (type instanceof TypeVariable<?> tv) {
            ctx.print(tv.getName(), Colors.DARK_GREEN);
        } else {
            String typeStr = type.toString();
            if (typeStr.startsWith("class ")) {
                typeStr = typeStr.substring(6);
            } else if (typeStr.startsWith("interface ")) {
                typeStr = typeStr.substring(10);
            }
            printTypeString(ctx, typeStr, color);
        }
    }

    private static void printTypeString(CommandExecutor.CmdExecContext ctx, String typeStr, byte color) {
        int arrayIdx = typeStr.indexOf("[]");
        if (arrayIdx != -1) {
            String baseType = typeStr.substring(0, arrayIdx);
            String arraySuffix = typeStr.substring(arrayIdx);
            printTypeString(ctx, baseType, color);
            ctx.print(arraySuffix, Colors.CYAN);
            return;
        }
        
        int genericStart = typeStr.indexOf('<');
        if (genericStart == -1) {
            ctx.print(formatTypeName(typeStr), color);
            return;
        }
        
        ctx.print(formatTypeName(typeStr.substring(0, genericStart)), color);
        
        int depth = 0;
        int argStart = genericStart;
        for (int i = genericStart; i < typeStr.length(); i++) {
            char c = typeStr.charAt(i);
            if (c == '<') {
                if (depth == 0) {
                    ctx.print("<", Colors.WHITE);
                    argStart = i + 1;
                }
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    if (i > argStart) {
                        printTypeArg(ctx, typeStr.substring(argStart, i), color);
                    }
                    ctx.print(">", Colors.WHITE);
                    if (i + 1 < typeStr.length()) {
                        printTypeString(ctx, typeStr.substring(i + 1), color);
                    }
                    return;
                }
            } else if (c == ',' && depth == 1) {
                if (i > argStart) {
                    printTypeArg(ctx, typeStr.substring(argStart, i), color);
                }
                ctx.print(", ", Colors.WHITE);
                argStart = i + 1;
            }
        }
        
        ctx.print(formatTypeName(typeStr.substring(genericStart)), color);
    }

    private static void printTypeArg(CommandExecutor.CmdExecContext ctx, String arg, byte color) {
        arg = arg.trim();
        if (arg.equals("?")) {
            ctx.print("?", Colors.DARK_BLUE);
        } else if (arg.startsWith("? extends ")) {
            ctx.print("?", Colors.DARK_BLUE);
            ctx.print(" extends ", Colors.DARK_BLUE);
            printTypeString(ctx, arg.substring(10), color);
        } else if (arg.startsWith("? super ")) {
            ctx.print("?", Colors.DARK_BLUE);
            ctx.print(" super ", Colors.DARK_BLUE);
            printTypeString(ctx, arg.substring(8), color);
        } else {
            printTypeString(ctx, arg, color);
        }
    }

    private static void printClassType(CommandExecutor.CmdExecContext ctx, Class<?> clazz, byte color) {
        if (clazz.isArray()) {
            printClassType(ctx, Objects.requireNonNull(clazz.getComponentType()), color);
            ctx.print("[]", Colors.CYAN);
        } else {
            ctx.print(formatTypeName(clazz.getName()), color);
        }
    }

    private static void printModifiers(CommandExecutor.CmdExecContext ctx, int modifiers) {
        StringBuilder sb = new StringBuilder();
        
        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        else sb.append("[package-private] ");
        
        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        
        if (sb.length() > 0) {
            ctx.print(sb.toString().stripTrailing(), Colors.DARK_BLUE);
            ctx.print(" ", Colors.DEFAULT);
        }
    }

    private static void printTypeParameters(CommandExecutor.CmdExecContext ctx, TypeVariable<?>[] typeParms) {
        ctx.print("<", Colors.WHITE);
        for (int i = 0; i < typeParms.length; i++) {
            if (i > 0) {
                ctx.print(", ", Colors.WHITE);
            }
            printTypeVariable(ctx, typeParms[i]);
        }
        ctx.print(">", Colors.WHITE);
    }

    private static void printTypeVariable(CommandExecutor.CmdExecContext ctx, TypeVariable<?> typeVar) {
        ctx.print(typeVar.getName(), Colors.DARK_GREEN);
        
        Type[] bounds = typeVar.getBounds();
        if (bounds.length > 0 && !(bounds.length == 1 && bounds[0] == Object.class)) {
            ctx.print(" extends ", Colors.DARK_BLUE);
            for (int i = 0; i < bounds.length; i++) {
                if (i > 0) {
                    ctx.print(" & ", Colors.WHITE);
                }
                printType(ctx, bounds[i], Colors.DARK_GREEN);
            }
        }
    }

    private static void printParameters(CommandExecutor.CmdExecContext ctx, Method method, boolean simple) {
        if (simple) {
            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    ctx.print(", ", Colors.WHITE);
                }
                if (method.isVarArgs() && (i == params.length - 1)) {
                    printClassType(ctx, Objects.requireNonNull(params[i].getComponentType()), Colors.GREEN);
                    ctx.print("...", Colors.WHITE);
                } else {
                    printClassType(ctx, params[i], Colors.GREEN);
                }
            }
        } else {
            Type[] params = method.getGenericParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    ctx.print(", ", Colors.WHITE);
                }
                if (method.isVarArgs() && (i == params.length - 1)) {
                    if (params[i] instanceof GenericArrayType gat) {
                        printType(ctx, gat.getGenericComponentType(), Colors.GREEN);
                    } else if (params[i] instanceof Class<?> clazz && clazz.isArray()) {
                        printType(ctx, clazz.getComponentType(), Colors.GREEN);
                    } else {
                        printType(ctx, params[i], Colors.GREEN);
                    }
                    ctx.print("...", Colors.WHITE);
                } else {
                    printType(ctx, params[i], Colors.GREEN);
                }
            }
        }
    }
}
