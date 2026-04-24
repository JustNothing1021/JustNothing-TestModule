package com.justnothing.testmodule.command.output;

/**
 * 客户端能力需求。
 * 
 * <p>包含客户端的能力和需求信息：
 * <ul>
 *   <li>supportsInput - 是否支持交互输入</li>
 *   <li>isJsonMode - 是否使用JSON模式输出</li>
 * </ul>
 * </p>
 * 
 * <p>注意：如果 isJsonMode 为 true，则 supportsInput 会被强制设置为 false，
 * 因为JSON模式不支持交互输入。</p>
 */
public class ClientRequirements {
    
    private boolean supportsInput;
    private boolean isJsonMode;
    
    public ClientRequirements() {
        this(false, false);
    }
    
    public ClientRequirements(boolean supportsInput, boolean isJsonMode) {
        this.isJsonMode = isJsonMode;
        this.supportsInput = isJsonMode ? false : supportsInput;
    }
    
    public boolean isSupportsInput() {
        return supportsInput;
    }
    
    public void setSupportsInput(boolean supportsInput) {
        if (!isJsonMode) {
            this.supportsInput = supportsInput;
        }
    }
    
    public boolean isJsonMode() {
        return isJsonMode;
    }
    
    public void setJsonMode(boolean jsonMode) {
        this.isJsonMode = jsonMode;
        if (jsonMode) {
            this.supportsInput = false;
        }
    }
    
    @Override
    public String toString() {
        return "ClientRequirements{" +
                "supportsInput=" + supportsInput +
                ", isJsonMode=" + isJsonMode +
                '}';
    }
}
