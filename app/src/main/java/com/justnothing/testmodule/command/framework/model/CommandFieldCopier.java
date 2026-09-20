package com.justnothing.testmodule.command.framework.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 按字段名把源对象的字段值复制到目标对象的同名字段上（含各自上溯的父类字段）。
 *
 * <p>{@link CommandRequest} 与 {@link CommandResult} 的 {@code fromJsonString} 都需要这套逻辑，
 * 原先两个类各复制了一份逐行相同的实现，这里合并成唯一一份。</p>
 */
final class CommandFieldCopier {

    private CommandFieldCopier() {
    }

    /**
     * 把 {@code source} 中所有非静态字段（含父类）按名字复制到 {@code target} 的同名字段上。
     * 目标对象上找不到同名字段时静默跳过。
     */
    static void copy(Object source, Object target) throws ReflectiveOperationException {
        for (Class<?> type = source.getClass();
             type != null && type != Object.class;
             type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Field targetField = findField(target.getClass(), field.getName());
                if (targetField == null) {
                    continue;
                }
                field.setAccessible(true);
                targetField.setAccessible(true);
                targetField.set(target, field.get(source));
            }
        }
    }

    /** 在类及其父类中按名字查找字段，找不到返回 null。 */
    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 当前类没有，继续往父类找
            }
        }
        return null;
    }
}
