package com.justnothing.testmodule.utils.tips;

/**
 * 提示回调接口
 * 所有提示类都需要实现这个接口
 */
public interface TipCallback {
    /**
     * 获取提示内容
     * 可以在这里实现复杂的逻辑（如概率计算）
     */
    String getContent();
    
    /**
     * 获取作者信息
     */
    String getAuthor();
    
    /**
     * 是否有特殊逻辑需要执行
     */
    boolean hasSpecialLogic();
    
    /**
     * 执行特殊逻辑
     */
    void executeSpecialLogic();
    
    /**
     * 是否应该被考虑（基础条件）
     */
    boolean shouldShow();
    
    /**
     * 是否应该显示（替换副标题）
     * 这是一个callable方法，可以动态判断是否应该显示
     */
    boolean shouldDisplay();
    
    /**
     * 显示优先级（数值越大优先级越高）
     */
    int getPriority();
}