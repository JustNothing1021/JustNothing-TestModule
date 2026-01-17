package com.justnothing.testmodule.utils.tips;

/**
 * 特殊提示回调实现
 * 用于Welcome页面副标题的特殊提示，不需要作者信息
 */
public class SpecialTipCallback extends BaseTipCallback {
    
    /**
     * 构造函数
     * @param content 提示内容
     * @param priority 显示优先级
     */
    public SpecialTipCallback(String content, int priority) {
        super(content, "", priority);
    }
    
    @Override
    public boolean shouldDisplay() {
        return true;
    }
    
    @Override
    public String getAuthor() {
        return "";
    }
}