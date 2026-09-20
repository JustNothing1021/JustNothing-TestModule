package com.justnothing.testmodule.utils.tips;

/**
 * 简单提示回调实现
 * 用于不需要复杂逻辑的提示
 */
public class SimpleTipCallback extends BaseTipCallback {
    
    /**
     * 构造函数
     * @param content 提示内容
     * @param author 作者信息
     * @param priority 显示优先级
     */
    public SimpleTipCallback(String content, String author, int priority) {
        super(content, author, priority);
    }
    
    /**
     * 简化构造函数（默认不显示在副标题，低优先级）
     * @param content 提示内容
     * @param author 作者信息
     */
    public SimpleTipCallback(String content, String author) {
        super(content, author, 10);
    }
}