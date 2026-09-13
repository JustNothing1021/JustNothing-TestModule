package com.justnothing.testmodule.command.framework.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.lang.reflect.Field;

/**
 * 统一的 Gson 配置工厂
 * 
 * 设计目标：
 * 1. 统一此前的混合序列化方式（Gson + 手动构建）
 * 2. 请求类（CommandRequest 子类）由 @CmdParam 注解驱动：serializedName 作为 JSON key
 * 3. 结果类（CommandResult 子类）字段默认参与序列化，字段名由 @SerializedName 控制；
 *    需排除的字段显式标注 @Expose(serialize = false, deserialize = false)
 * 
 * 使用方式：
 * <pre>
 * // 序列化
 * String json = GsonFactory.getInstance().toJson(result);
 * 
 * // 反序列化
 * ComplexHookResult result = GsonFactory.getInstance().fromJson(jsonStr, ComplexHookResult.class);
 * </pre>
 */
public class GsonFactory {

    private static volatile Gson instance;
    private static volatile Gson prettyInstance;

    private GsonFactory() {
    }

    public static synchronized Gson getInstance() {
        if (instance == null) {
            instance = createGsonInstance(false);
        }
        return instance;
    }

    /**
     * 缩进版 Gson：仅用于把对象渲染成人类可读的 JSON（如 {@code class analyze --raw}）。
     * 与 {@link #getInstance()} 共用同一套排除/命名策略，只有缩进不同。
     */
    public static synchronized Gson getPrettyInstance() {
        if (prettyInstance == null) {
            prettyInstance = createGsonInstance(true);
        }
        return prettyInstance;
    }

    private static Gson createGsonInstance(boolean pretty) {
        GsonBuilder builder = new GsonBuilder()
            .addSerializationExclusionStrategy(new AnnotationDrivenExclusionStrategy(true))
            .addDeserializationExclusionStrategy(new AnnotationDrivenExclusionStrategy(false))
            .setFieldNamingStrategy(new AnnotationDrivenFieldNamingStrategy());
        if (pretty) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    /**
     * 字段排除策略：
     * - @CmdParam 优先：按 serialize/deserialize 决定是否参与
     * - @Expose 显式声明：按其 serialize/deserialize 标志决定
     *   （如 {@code CommandResult.ErrorInfo.stacktrace} 标注 serialize=false，避免堆栈外泄）
     * - 无注解字段：请求类（CommandRequest 子类）与结果类（CommandResult 子类）
     *   默认参与（opt-out），避免为每个字段逐个标注；其余类按 @Expose 参与（opt-in）
     */
    private static final class AnnotationDrivenExclusionStrategy implements ExclusionStrategy {
        private final boolean serializing;

        AnnotationDrivenExclusionStrategy(boolean serializing) {
            this.serializing = serializing;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            CmdParam param = f.getAnnotation(CmdParam.class);
            if (param != null) {
                return serializing ? !param.serialize() : !param.deserialize();
            }
            Expose expose = f.getAnnotation(Expose.class);
            if (expose != null) {
                return serializing ? !expose.serialize() : !expose.deserialize();
            }
            boolean autoIncluded = CommandRequest.class.isAssignableFrom(f.getDeclaringClass())
                    || CommandResult.class.isAssignableFrom(f.getDeclaringClass());
            return !autoIncluded;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    /**
     * 字段命名策略：@CmdParam.serializedName 优先作为 JSON key，否则保持字段名。
     * 注意 @SerializedName 优先级高于 FieldNamingStrategy，不受影响。
     */
    private static final class AnnotationDrivenFieldNamingStrategy implements FieldNamingStrategy {
        @Override
        public String translateName(Field field) {
            CmdParam param = field.getAnnotation(CmdParam.class);
            if (param != null && !param.serializedName().isEmpty()) {
                return param.serializedName();
            }
            return FieldNamingPolicy.IDENTITY.translateName(field);
        }
    }
}
