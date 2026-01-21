package com.justnothing.testmodule.utils.tips;

/**
 * 基础提示回调实现
 * 提供默认实现，可以继承并重写特定方法
 */
public abstract class BaseTipCallback implements TipCallback {
    protected String content;
    protected String author;
    protected int priority;
    
    /**
     * 构造函数
     * @param content 提示内容
     * @param author 作者信息
     * @param priority 显示优先级
     */
    public BaseTipCallback(String content, String author, int priority) {
        this.content = content;
        this.author = author;
        this.priority = priority;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public String getAuthor() {
        return author;
    }
    
    @Override
    public boolean hasSpecialLogic() {
        return false;
    }
    
    @Override
    public void executeSpecialLogic() {
    }
    
    @Override
    public boolean shouldShow() {
        return true;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
}