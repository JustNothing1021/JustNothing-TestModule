package com.justnothing.testmodule.command.base.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 统一的 Gson 配置工厂
 * 
 * 设计目标：
 * 1. 替代原有的三套混合序列化系统（Gson + AutoSerializer + 手动构建）
 * 2. 使用 @Exclude + @SerializedName 注解驱动序列化
 * 3. 自动处理所有嵌套对象（List, Map, 自定义类型）
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

    private GsonFactory() {
    }

    public static synchronized Gson getInstance() {
        if (instance == null) {
            instance = createGsonInstance();
        }
        return instance;
    }

    private static Gson createGsonInstance() {
        return new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    }
}
